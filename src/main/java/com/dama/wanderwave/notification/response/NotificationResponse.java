package com.dama.wanderwave.notification.response;

import com.dama.wanderwave.notification.Notification;
import com.dama.wanderwave.user.response.ShortUserResponse;
import com.dama.wanderwave.user.response.UserResponse;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private String id;
    private String content;
    private ShortUserResponse recipient;
    private boolean isRead;
    private LocalDateTime createdAt;
    private Notification.NotificationType type;
    private String objectId;
    private ShortUserResponse actionUser;

    public static NotificationResponse fromEntity(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .content(notification.getContent())
                .recipient(ShortUserResponse.fromEntity(notification.getRecipient()))
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .type(notification.getType())
                .objectId(notification.getObjectId())
                .actionUser(ShortUserResponse.fromEntity(notification.getActionUser()))
                .build();
    }
}