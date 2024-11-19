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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UserService {

    private final UserRepository userRepository;

    @Cacheable(value = "users", key = "#id")
    public UserResponse getUserById(String id) {
        var user = findUserByIdOrThrow(id);
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
        if (modifyUserConnection(followerId, follower.getSubscriptions(), followedId, subscribe, "subscriptions")) {
            modifyUserConnection(followedId, followed.getSubscribers(), followerId, subscribe, "subscribers");
            return true;
        }
        return false;
    }

    public void updateFollowCounts(User follower, User followed, boolean subscribe) {
        int value = subscribe ? 1 : -1;
        follower.setSubscriptionsCount(follower.getSubscriptionsCount() + value);
        followed.setSubscriberCount(followed.getSubscriberCount() + value);
    }

    public String updateBlacklist(BlockRequest request, boolean add) {
        String blockerId = request.getBlockerId();
        String blockedId = request.getBlockedId();

        User blocker = findUserByIdOrThrow(blockerId);
        checkUserAccessRights(blocker, blockedId);
        findUserByIdOrThrow(blockedId);

        boolean isSuccess = modifyUserConnection(blockerId, blocker.getBlackList().userIds(), blockedId, add, "blacklist");

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

    public String updateBan(String id, boolean ban) {
        User toBan = findUserByIdOrThrow(id);
        toBan.setAccountLocked(ban);
        userRepository.save(toBan);

        String action = ban ? "banned" : "unbanned";
        log.info("User with ID {} has been {} (account {})", id, action, ban ? "locked" : "unlocked");

        return ban ? "User banned successfully" : "User unbanned successfully";
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
        if (connectionsPage.isEmpty()) {
            log.info("No {} found for userId: {}", connectionType, userId);
            return Collections.emptyList();
        }
        log.info("Fetched {} {} for userId: {}", connectionsPage.getTotalElements(), connectionType, userId);
        return userRepository.findAllByIdIn(connectionsPage.getContent()).stream()
                .map(this::userToUserResponse)
                .toList();
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

    private User findUserByIdOrThrow(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id " + userId));
    }

    private UserResponse userToUserResponse(User user) {
        return UserResponse
                .builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .description(user.getDescription())
                .avatarUrl(user.getImageUrl())
                .subscriberCount(user.getSubscriberCount())
                .subscriptionsCount(user.getSubscriptionsCount())
                .build();
    }
}
