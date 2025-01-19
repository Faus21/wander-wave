package com.dama.wanderwave.user;

import com.dama.wanderwave.handler.user.UnauthorizedActionException;
import com.dama.wanderwave.handler.user.UserNotFoundException;
import com.dama.wanderwave.notification.NotificationService;
import com.dama.wanderwave.user.request.SubscribeRequest;
import com.dama.wanderwave.user.response.ShortUserResponse;
import com.dama.wanderwave.user.response.UserResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.BiFunction;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UserService {

    private static final String DEFAULT_AVATAR_URL = "https://wanderwave.blob.core.windows.net/avatars/user.png";

    private final UserRepository userRepository;

    private final static int SUBSCRIPTIONS_PAGE = 10;
    private final ModelMapper modelMapper;
    private final NotificationService notificationService;


    public UserResponse getUserById(String id) {
        User user = findUserByIdOrThrow(id);

        return isBannedOrBlacklisted(user);
    }

    @Transactional
    public String updateSubscription(SubscribeRequest request, boolean subscribe) {
        var followerId = request.getFollowerId();
        var followedId = request.getFollowedId();

        User follower = findUserByIdOrThrow(followerId);
        checkUserAccessRights(follower, followerId);

        User followed = findUserByIdOrThrow(followedId);

        if (updateFollowStatus(follower, followed, followerId, followedId, subscribe)) {
            updateFollowCounts(follower, followed, subscribe);
            userRepository.save(follower);
            userRepository.save(followed);

            if (subscribe) {
                notificationService.sendFollowNotification(
                        followedId,
                        followedId,
                        followerId
                );
            }

            return subscribe ? "User subscribed successfully" : "User unsubscribed successfully";
        } else {
            return subscribe ? "Already subscribed" : "Not subscribed, cannot unsubscribe";
        }
    }

    private boolean updateFollowStatus(User follower, User followed, String followerId, String followedId, boolean subscribe) {
        boolean subscriptionUpdated = modifyUserConnection(
                followerId,
                follower.getSubscriptions(),
                followedId,
                subscribe,
                "subscriptions"
        );

        if (subscriptionUpdated) {
            modifyUserConnection(
                    followedId,
                    followed.getSubscribers(),
                    followerId,
                    subscribe,
                    "subscribers"
            );
            return true;
        }

        return false;
    }

    public void updateFollowCounts(User follower, User followed, boolean subscribe) {
        int value = subscribe ? 1 : -1;
        follower.setSubscriptionsCount(follower.getSubscriptionsCount() + value);
        followed.setSubscriberCount(followed.getSubscriberCount() + value);
    }

    public String updateBan(String id, boolean ban) {
        User toBan = findUserByIdOrThrow(id);
        toBan.setAccountLocked(ban);
        userRepository.save(toBan);
        String action = ban ? "banned" : "unbanned";
        log.info("User with ID {} has been {} (account {})", id, action, ban ? "locked" : "unlocked");
        return ban ? "User banned successfully" : "User unbanned successfully";
    }

    public String updateBlacklist(String blockedId, boolean add) {
        String blockerId = getAuthenticatedUser().getId();

        User blocker = findUserByIdOrThrow(blockerId);
        if (blocker.getBlackList() == null) {
            blocker.setBlackList(new BlackList(new HashSet<>()));
        }
        checkUserAccessRights(blocker, blockerId);
        findUserByIdOrThrow(blockedId);

        boolean isSuccess = modifyUserConnection(blockerId, blocker.getBlackList().userIds(),
                blockedId, add, "blacklist");

        if (isSuccess) {
            userRepository.save(blocker);
            String action = add ? "blocked" : "unblocked";
            log.info("User with ID {} {} user with ID {}", blockerId, action, blockedId);
            return add ? "User blocked successfully" : "User unblocked successfully";
        } else {
            String action = add ? "block" : "unblock";
            log.warn("Failed to {} user with ID {} in blocker ID {}'s blacklist", action, blockedId, blockerId);
            return add ? "Failed to block user" : "Failed to unblock user";
        }
    }


    private boolean modifyUserConnection(String userId, Set<String> connections, String targetId, boolean add, String connectionType) {
        boolean isSuccess = add ? !connections.contains(targetId) : connections.remove(targetId);
        if (isSuccess) {
            if (add) {
                connections.add(targetId);
                log.info("User with ID {} added to {} for user ID {}", targetId, connectionType, userId);
            } else {
                log.info("User with ID {} removed from {} for user ID {}", targetId, connectionType, userId);
            }
        } else {
            log.warn("User with ID {} {} in {} for user ID {}", targetId, add ? "is already" : "was not found", connectionType, userId);
        }

        return isSuccess;
    }

    public List<UserResponse> getUserSubscribers(String userId, int page, int size) {
        return getUserConnections(userId, page, size, userRepository::findSubscribersIdsByUserId, "subscribers");
    }

    public List<UserResponse> getUserSubscriptions(String userId, int page, int size) {
        return getUserConnections(userId, page, size, userRepository::findSubscriptionsIdsByUserId, "subscriptions");
    }

    private List<UserResponse> getUserConnections(String userId, int page, int size,
                                                  BiFunction<String, Pageable, Page<String>> findMethod, String connectionType) {
        Pageable pageable = PageRequest.of(page, size);
        Page<String> connectionsPage = findMethod.apply(userId, pageable);
        if (connectionsPage == null || connectionsPage.isEmpty()) {
            log.info("No {} found for userId: {}", connectionType, userId);
            return Collections.emptyList();
        }
        log.info("Fetched {} {} for userId: {}", connectionsPage.getTotalElements(), connectionType, userId);
        return allUsersToUserResponse(userRepository.findAllByIdIn(connectionsPage.getContent()));
    }


    public User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    public void checkUserAccessRights(User authenticatedUser, String userId) {
        if (authenticatedUser.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"))) {
            return;
        }
        userRepository.findById(userId)
                .filter(user -> user.getEmail().equals(authenticatedUser.getEmail()))
                .orElseThrow(() -> new UnauthorizedActionException("Unauthorized action from user"));
    }

    public User findUserByIdOrThrow(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id " + userId));
    }

    public User findUserByNicknameOrThrow(String nickname) {
        return userRepository.findByNickname(nickname)
                .orElseThrow(() -> new UserNotFoundException("User not found with nickname " + nickname));
    }

    private List<UserResponse> allUsersToUserResponse(List<User> users) {
        return users.stream()
                .map(this::userToUserResponse)
                .toList();
    }

    protected UserResponse userToUserResponse(User user) {
        return modelMapper.map(user, UserResponse.class);
    }

    private UserResponse userResponseToBannedUserResponse(UserResponse userResponse) {
        return UserResponse.builder()
                .nickname(userResponse.getNickname())
                .id(userResponse.getId())
                .avatarUrl(DEFAULT_AVATAR_URL)
                .build();
    }

    @Transactional
    public List<UserResponse> getUserFriendshipRecommendations() {
        User user = getAuthenticatedUser();
        log.debug("User found: {}", user);

        log.info("Fetching friendship recommendations for user with ID: {}", user.getId());

        int subs = user.getSubscriptionsCount();
        log.debug("User has {} subscriptions", subs);

        Set<String> uniqueUserIds = new HashSet<>();

        if (subs == 0) {
            log.info("No subscriptions found for user, returning random users.");
            List<UserResponse> randomUsers = allUsersToUserResponse(getRandomUsersFromDatabase(SUBSCRIPTIONS_PAGE));
            randomUsers.forEach(u -> uniqueUserIds.add(u.getId()));
            return new ArrayList<>(uniqueUserIds.stream()
                    .map(id -> randomUsers.stream()
                            .filter(u -> u.getId().equals(id))
                            .findFirst()
                            .orElse(null))
                    .filter(Objects::nonNull)
                    .toList());
        }

        int pages = Math.max(1, subs / SUBSCRIPTIONS_PAGE);
        Pageable randomPage = PageRequest.of((int) (Math.random() * pages), SUBSCRIPTIONS_PAGE);
        log.debug("Generated random page: {}", randomPage);

        List<UserResponse> randomSubscriptions =
                getUserSubscriptions(user.getId(), randomPage.getPageNumber(), randomPage.getPageSize());
        log.debug("Random subscriptions fetched: {}", randomSubscriptions.size());

        if (randomSubscriptions.size() > SUBSCRIPTIONS_PAGE) {
            log.debug("Shuffling subscriptions as their count is greater than the page size");
            Collections.shuffle(randomSubscriptions);
            randomSubscriptions = randomSubscriptions.subList(0, SUBSCRIPTIONS_PAGE);
        }

        log.debug("Fetching nested random subscriptions for shuffled user responses");
        List<String> nestedRandomSubscriptions = userRepository.findAllByIdIn(
                        randomSubscriptions.stream()
                                .map(UserResponse::getId)
                                .toList()
                )
                .stream()
                .map(u -> getRandomElement(u.getSubscriptions()))
                .filter(Objects::nonNull)
                .toList();
        log.debug("Nested random subscriptions fetched: {}", nestedRandomSubscriptions.size());

        List<UserResponse> result = allUsersToUserResponse(userRepository.findAllByIdIn(nestedRandomSubscriptions));
        log.debug("Initial result size after fetching nested random subscriptions: {}", result.size());

        result.forEach(u -> uniqueUserIds.add(u.getId()));

        if (uniqueUserIds.size() < SUBSCRIPTIONS_PAGE) {
            int size = SUBSCRIPTIONS_PAGE - uniqueUserIds.size();
            log.info("Adding {} more random users to complete the required page size", size);
            List<UserResponse> additionalRandomUsers = allUsersToUserResponse(getRandomUsersFromDatabase(size));
            additionalRandomUsers.forEach(u -> uniqueUserIds.add(u.getId()));
            result.addAll(additionalRandomUsers);
        }

        uniqueUserIds.remove(user.getId());
        log.info("Returning final list of friendship recommendations with size: {}", uniqueUserIds.size());

        return uniqueUserIds.stream()
                .map(id -> result.stream()
                        .filter(u -> u.getId().equals(id))
                        .findFirst()
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    private List<User> getRandomUsersFromDatabase(int count) {
        Pageable pageable = PageRequest.of(0, count);
        Page<User> res = userRepository.findAll(pageable);
        return !res.isEmpty() ? res.getContent() : Collections.emptyList();
    }

    private String getRandomElement(Set<String> set) {
        if (set == null || set.isEmpty()) {
            return null;
        }

        int size = set.size();
        int item = new Random().nextInt(size);

        Iterator<String> iterator = set.iterator();
        for (int i = 0; i < item; i++) {
            iterator.next();
        }

        return iterator.next();
    }

    @Transactional
    public void changeAvatar(String url) {
        User user = getAuthenticatedUser();
        user.setImageUrl(url);
        userRepository.save(user);
    }

    public UserResponse getUserByNickname(String nickname) {
        User user = userRepository.findByNickname(nickname)
                .orElseThrow(() -> new UserNotFoundException("User not found with nickname " + nickname));

        return isBannedOrBlacklisted(user);
    }

    private UserResponse isBannedOrBlacklisted(User user) {
        User authenticatedUser = getAuthenticatedUser();
        BlackList blackList = user.getBlackList();
        if ((blackList != null && blackList.userIds() != null && blackList.userIds().contains(authenticatedUser.getId())) ||
                user.isAccountLocked()) {
            return userResponseToBannedUserResponse(userToUserResponse(user));
        }

        return userToUserResponse(user);
    }

    public Page<UserResponse> getAllUsers(String nickname, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> usersPage;
        if (nickname != null && !nickname.isEmpty()) {
            usersPage = userRepository.findByNicknameContainingIgnoreCase(nickname, pageable);
        } else {
            usersPage = userRepository.findAll(pageable);
        }
        return usersPage.map(this::userToUserResponse);
    }


    public boolean isSubscribed(String userId) {
        User authenticatedUser = getAuthenticatedUser();
        return userRepository.isSubscribed(authenticatedUser.getId(), userId);
    }

    public void changeUsername(String username) {
        User user = getAuthenticatedUser();
        user.setNickname(username);
        userRepository.save(user);
    }

    public void changeDescription(String description) {
        User user = getAuthenticatedUser();
        user.setDescription(description);
        userRepository.save(user);
    }

    public List<ShortUserResponse> getUserBlacklist() {
        User user = getAuthenticatedUser();

        if (user.getBlackList() == null ||
                user.getBlackList().userIds() == null ||
                user.getBlackList().userIds().isEmpty()) {
            return Collections.emptyList();
        }

        return user.getBlackList().userIds()
                .stream()
                .map(id -> {
                    User blacklistUser = findUserByIdOrThrow(id);
                    return ShortUserResponse.fromEntity(blacklistUser);
                })
                .toList();
    }
}
