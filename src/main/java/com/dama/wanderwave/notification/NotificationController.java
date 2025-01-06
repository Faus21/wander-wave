package com.dama.wanderwave.notification;

import com.dama.wanderwave.notification.request.NotificationRequest;
import com.dama.wanderwave.notification.response.NotificationResponse;
import com.dama.wanderwave.utils.ResponseRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification", description = "Endpoints for managing user notifications.")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/like")
    @Operation(summary = "Send like notification", description = "Sends a notification when a user likes a post.")
    public ResponseEntity<ResponseRecord> sendLikeNotification(@RequestBody NotificationRequest request) {
        NotificationResponse response = notificationService.sendLikeNotification(
                request.getRecipientId(),
                request.getObjectId(),
                request.getActionUserId()
        );
        return ResponseEntity.ok(new ResponseRecord(HttpStatus.OK.value(), response));
    }

    @PostMapping("/comment")
    @Operation(summary = "Send comment notification", description = "Sends a notification when a user comments on a post.")
    public ResponseEntity<ResponseRecord> sendCommentNotification(@RequestBody NotificationRequest request) {
        NotificationResponse response = notificationService.sendCommentNotification(
                request.getRecipientId(),
                request.getObjectId(),
                request.getActionUserId()
        );
        return ResponseEntity.ok(new ResponseRecord(HttpStatus.OK.value(), response));
    }

    @PostMapping("/follow")
    @Operation(summary = "Send follow notification", description = "Sends a notification when a user follows another user.")
    public ResponseEntity<ResponseRecord> sendFollowNotification(@RequestBody NotificationRequest request) {
        NotificationResponse response = notificationService.sendFollowNotification(
                request.getRecipientId(),
                request.getObjectId(),
                request.getActionUserId()
        );
        return ResponseEntity.ok(new ResponseRecord(HttpStatus.OK.value(), response));
    }

    @GetMapping("")
    @Operation(summary = "Get notifications by user ID", description = "Retrieves all notifications for a specific user.")
    public ResponseEntity<ResponseRecord> getNotificationsByUserId(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<NotificationResponse> response = notificationService.getNotifications(page, size);
        return ResponseEntity.ok(new ResponseRecord(HttpStatus.OK.value(), response));
    }

    @GetMapping("/unread")
    @Operation(summary = "Get unread notifications", description = "Retrieves all unread notifications for a specific user.")
    public ResponseEntity<ResponseRecord> getUnreadNotificationsByUserId(
            @RequestParam(defaultValue = "0") int page,
            @Max(20) Integer size) {
        List<NotificationResponse> response = notificationService.getUnreadNotifications(page, size);
        return ResponseEntity.ok(new ResponseRecord(HttpStatus.OK.value(), response));
    }

    @PostMapping("/mark-as-read/{notificationId}")
    @Operation(summary = "Mark notification as read", description = "Marks a specific notification as read.")
    public ResponseEntity<ResponseRecord> markNotificationAsRead(@PathVariable String notificationId) {
        NotificationResponse response = NotificationResponse.fromEntity(notificationService.markNotificationAsRead(notificationId));
        return ResponseEntity.ok(new ResponseRecord(HttpStatus.OK.value(), response));
    }

    @PostMapping("/mark-all-as-read")
    @Operation(summary = "Mark all notifications as read", description = "Marks all notifications for a user as read.")
    public ResponseEntity<ResponseRecord> markAllNotificationsAsRead() {
        notificationService.markAllNotificationsAsRead();
        return ResponseEntity.ok(new ResponseRecord(HttpStatus.OK.value(), "All notifications marked as read"));
    }
}