package com.dama.wanderwave.notification;

import com.dama.wanderwave.handler.user.UserNotFoundException;
import com.dama.wanderwave.notification.response.NotificationResponse;
import com.dama.wanderwave.user.User;
import com.dama.wanderwave.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public void sendLikeNotification(String recipientId, String objectId, String actionUserId) {
        User recipient = findUserByIdOrThrow(recipientId);
        User actionUser = findUserByIdOrThrow(actionUserId);

        if (notificationRepository.existsByRecipientAndActionUserAndObjectId(recipient, actionUser, objectId)) {
            return;
        }

        createNotification(
                recipientId,
                Notification.NotificationType.LIKE,
                objectId,
                actionUserId
        );
    }

    public void sendCommentNotification(String recipientId, String objectId, String actionUserId) {
        createNotification(
                recipientId,
                Notification.NotificationType.COMMENT,
                objectId,
                actionUserId
        );
    }

    public void sendFollowNotification(String recipientId, String objectId, String actionUserId) {
        User recipient = findUserByIdOrThrow(recipientId);
        User actionUser = findUserByIdOrThrow(actionUserId);

        if (notificationRepository.existsByRecipientAndActionUserAndObjectId(recipient, actionUser, objectId)) {
            return;
        }

        createNotification(
                recipientId,
                Notification.NotificationType.FOLLOW,
                objectId,
                actionUserId
        );
    }

    private NotificationResponse createNotification(String recipientId, Notification.NotificationType type, String objectId, String actionUserId) {
        User recipient = findUserByIdOrThrow(recipientId);
        User actionUser = findUserByIdOrThrow(actionUserId);

        Notification notification = Notification.builder()
                .recipient(recipient)
                .type(type)
                .objectId(objectId)
                .actionUser(actionUser)
                .createdAt(LocalDateTime.now())
                .build();

        Notification savedNotification = notificationRepository.save(notification);
        sendRealTimeNotification(savedNotification);
        return NotificationResponse.fromEntity(savedNotification);
    }

    private void sendRealTimeNotification(Notification notification) {
        messagingTemplate.convertAndSendToUser(
                notification.getRecipient().getId(),
                "/queue/notifications",
                NotificationResponse.fromEntity(notification)
        );
    }

    public List<NotificationResponse> getNotifications(int page, int size) {
        User user = getAuthenticatedUser();
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(page, size))
                .stream()
                .map(NotificationResponse::fromEntity)
                .toList();
    }

    public List<NotificationResponse> getUnreadNotifications(int page, int size) {
        User user = getAuthenticatedUser();
        return notificationRepository
                .findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(user.getId(), PageRequest.of(page, size))
                .stream()
                .map(NotificationResponse::fromEntity)
                .toList();
    }

    public Notification getNotificationById(String notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
    }

    public Notification markNotificationAsRead(String notificationId) {
        log.info("Attempting to mark notification with ID: {} as read", notificationId);

        Notification notification = getNotificationById(notificationId);
        notification.setRead(true);

        log.info("Notification with ID: {} marked as read", notificationId);

        Notification savedNotification = notificationRepository.save(notification);
        log.info("Notification with ID: {} successfully marked as read and saved", notificationId);

        return savedNotification;
    }

    public void markAllNotificationsAsRead() {
        User user = getAuthenticatedUser();
        List<Notification> notifications = notificationRepository.findByRecipientIdAndIsReadFalse(user.getId());
        notifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(notifications);
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    private User findUserByIdOrThrow(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id " + userId));
    }
}