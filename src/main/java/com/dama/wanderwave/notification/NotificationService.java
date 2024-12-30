package com.dama.wanderwave.notification;

import com.dama.wanderwave.notification.response.NotificationResponse;
import com.dama.wanderwave.user.User;
import com.dama.wanderwave.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationResponse sendLikeNotification(String recipientId, String objectId, String actionUserId) {
        return createNotification(
                recipientId,
                "You have a new like on your post!",
                Notification.NotificationType.LIKE,
                objectId,
                actionUserId
        );
    }

    public NotificationResponse sendCommentNotification(String recipientId, String objectId, String actionUserId) {
        return createNotification(
                recipientId,
                "You have a new comment on your post!",
                Notification.NotificationType.COMMENT,
                objectId,
                actionUserId
        );
    }

    public NotificationResponse sendFollowNotification(String recipientId, String objectId, String actionUserId) {
        return createNotification(
                recipientId,
                "You have a new follower!",
                Notification.NotificationType.FOLLOW,
                objectId,
                actionUserId
        );
    }

    private NotificationResponse createNotification(String recipientId, String content, Notification.NotificationType type, String objectId, String actionUserId) {
        User recipient = userService.findUserByIdOrThrow(recipientId);
        User actionUser = userService.findUserByIdOrThrow(actionUserId);

        Notification notification = Notification.builder()
                .content(content)
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
        User user = userService.getAuthenticatedUser();
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(page, size))
                .stream()
                .map(NotificationResponse::fromEntity)
                .toList();
    }

    public List<NotificationResponse> getUnreadNotifications(int page, int size) {
        User user = userService.getAuthenticatedUser();
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
        Notification notification = getNotificationById(notificationId);
        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    public void markAllNotificationsAsRead() {
        User user = userService.getAuthenticatedUser();
        List<Notification> notifications = notificationRepository.findByRecipientIdAndIsReadFalse(user.getId());
        notifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(notifications);
    }
}