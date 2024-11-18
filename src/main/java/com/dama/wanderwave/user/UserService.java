package com.dama.wanderwave.user;

import com.dama.wanderwave.handler.user.UnauthorizedActionException;
import com.dama.wanderwave.handler.user.UserNotFoundException;
import com.dama.wanderwave.user.request.BlockRequest;
import com.dama.wanderwave.user.request.SubscribeRequest;
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

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UserService {

    private final UserRepository userRepository;

    @Cacheable(value = "users", key = "#id")
    public User getUserById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id " + id));
    }

    @Transactional
    public String updateSubscription(SubscribeRequest request, int value) {
        String followerId = request.getFollowerId();
        String followedId = request.getFollowedId();

        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id " + followerId));

        verifyUserAccess(follower, followerId);

        User followed = userRepository.findById(followedId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id " + followedId));

        updateSubscriptionsAndSubscribers(follower, followed, followerId, followedId, value);

        userRepository.save(follower);
        userRepository.save(followed);

        log.info("User {} {} successfully from user {}", followerId, (value == 1 ? "subscribed" : "unsubscribed"), followedId);
        return value == 1 ? "User subscribed successfully" : "User unsubscribed successfully";
    }

    private void updateSubscriptionsAndSubscribers(User follower, User followed, String followerId, String followedId, int value) {
        if (value == 1) {
            if (follower.getSubscriptions().contains(followedId)) {
                log.warn("User with ID {} is already subscribed to user with ID {}", followerId, followedId);
            } else {
                follower.getSubscriptions().add(followedId);
                followed.getSubscribers().add(followerId);
                log.info("User with ID {} followed user with ID {}", followerId, followedId);
            }
        } else if (value == -1) {
            if (!follower.getSubscriptions().contains(followedId)) {
                log.warn("User with ID {} is not subscribed to user with ID {}, cannot unfollow", followerId, followedId);
            } else {
                follower.getSubscriptions().remove(followedId);
                followed.getSubscribers().remove(followerId);
                log.info("User with ID {} unfollowed user with ID {}", followerId, followedId);
            }
        }

        follower.setSubscriptionsCount(follower.getSubscriptionsCount() + value);
        followed.setSubscriberCount(followed.getSubscriberCount() + value);
    }

    public String updateBlacklist(BlockRequest request, boolean add) {
        String blockerId = request.getBlockerId();
        String blockedId = request.getBlockedId();

        User blocker = userRepository.findById(blockerId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id " + blockerId));

        verifyUserAccess(blocker, blockerId);

        User blocked = userRepository.findById(blockedId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id " + blockedId));

        updateBlacklist(blocker, blockedId, add);
        userRepository.save(blocker);

        String action = add ? "blocked" : "unblocked";
        log.info("User with ID {} {} user with ID {}", blockerId, action, blockedId);

        return add ? "User blocked successfully" : "User unblocked successfully";
    }

    private void updateBlacklist(User blocker, String blockedId, boolean add) {
        if (add) {
            blocker.getBlackList().addUser(blockedId);
        } else {
            var success = blocker.getBlackList().removeUser(blockedId);
            if (!success) {
                log.warn("User with ID {} was not found in the blacklist of user ID {}", blockedId, blocker.getId());
            }
        }
    }

    public String updateBan(String id, boolean ban) {
        User toBan = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id " + id));

        toBan.setAccountLocked(ban);
        userRepository.save(toBan);

        if (ban) {
            log.info("User with ID {} has been banned (account locked)", id);
            return "User banned successfully";
        } else {
            log.info("User with ID {} has been unbanned (account unlocked)", id);
            return "User unbanned successfully";
        }
    }

    public Page<User> getUserSubscribers(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> subscribers = userRepository.findSubscribersByUserId(userId, pageable);
        if (subscribers.isEmpty()) {
            log.info("No subscribers found for userId: {}", userId);
        } else {
            log.info("Fetched {} subscribers for userId: {}", subscribers.getTotalElements(), userId);
        }
        return subscribers;
    }

    public Page<User> getUserSubscriptions(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> subscriptions = userRepository.findSubscriptionsByUserId(userId, pageable);
        if (subscriptions.isEmpty()) {
            log.info("No subscriptions found for userId: {}", userId);
        } else {
            log.info("Fetched {} subscriptions for userId: {}", subscriptions.getTotalElements(), userId);
        }
        return subscriptions;
    }

    public User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    public User verifyUserAccess(User authenticatedUser, String userId) {
        return userRepository.findById(userId)
                .filter(user -> user.getEmail().equals(authenticatedUser.getEmail()))
                .orElseThrow(() -> new UnauthorizedActionException("Unauthorized action from user"));
    }
}
