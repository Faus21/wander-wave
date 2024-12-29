package com.dama.wanderwave.user;

import com.dama.wanderwave.handler.user.UnauthorizedActionException;
import com.dama.wanderwave.handler.user.UserNotFoundException;
import com.dama.wanderwave.user.request.BlockRequest;
import com.dama.wanderwave.user.request.SubscribeRequest;
import com.dama.wanderwave.user.response.UserResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
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

    private final UserRepository userRepository;

    private final static int SUBSCRIPTIONS_PAGE = 10;

    @Cacheable(value = "users", key = "#id")
    public UserResponse getUserById(String id) {
        User user = findUserByIdOrThrow(id);
        return userToUserResponse(user);
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

    public String updateBlacklist(BlockRequest request, boolean add) {
        String blockerId = request.getBlockerId();
        String blockedId = request.getBlockedId();

        User blocker = findUserByIdOrThrow(blockerId);
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
        userRepository.findById(userId)
                .filter(user -> user.getEmail().equals(authenticatedUser.getEmail()))
                .orElseThrow(() -> new UnauthorizedActionException("Unauthorized action from user"));
    }

    public User findUserByIdOrThrow(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id " + userId));
    }

    private List<UserResponse> allUsersToUserResponse(List<User> users) {
        return users.stream()
                .map(this::userToUserResponse)
                .toList();
    }

    private UserResponse userToUserResponse(User user) {
        return UserResponse
                .builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .description(user.getDescription())
                .avatarUrl(user.isAccountLocked() ? "" : user.getImageUrl())
                .subscriberCount(user.getSubscriberCount())
                .subscriptionsCount(user.getSubscriptionsCount())
                .build();
    }

    @Transactional
    public List<UserResponse> getUserFriendshipRecommendations() {
        User user = getAuthenticatedUser();
        log.debug("User found: {}", user);

        log.info("Fetching friendship recommendations for user with ID: {}", user.getId());


        int subs = user.getSubscriptionsCount();
        log.debug("User has {} subscriptions", subs);

        if (subs == 0) {
            log.info("No subscriptions found for user, returning random users.");
            return allUsersToUserResponse(getRandomUsersFromDatabase(SUBSCRIPTIONS_PAGE));
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

        List<UserResponse> mutableResult = new ArrayList<>(result);

        if (mutableResult.size() < SUBSCRIPTIONS_PAGE) {
            int size = SUBSCRIPTIONS_PAGE - mutableResult.size();
            log.info("Adding {} more random users to complete the required page size", size);
            mutableResult.addAll(allUsersToUserResponse(getRandomUsersFromDatabase(size)));
        }

        mutableResult.removeIf(u -> u.getId().equals(user.getId()));
        log.info("Returning final list of friendship recommendations with size: {}", mutableResult.size());
        return mutableResult;
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

    @Cacheable(value = "users", key = "#nickname")
    public UserResponse getUserByNickname(String nickname) {
        User user = userRepository.findByNickname(nickname)
                .orElseThrow(() -> new UserNotFoundException("User not found with nickname " + nickname));
        return userToUserResponse(user);
    }

    public boolean isSubscribed(String userId) {
        User authenticatedUser = getAuthenticatedUser();
        return userRepository.isSubscribed(authenticatedUser.getId(), userId);
    }
}
