package com.dama.wanderwave.notification.request;

import com.dama.wanderwave.notification.Notification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {

    @NotBlank(message = "Recipient ID cannot be blank")
    private String recipientId;

    @NotBlank(message = "Content cannot be blank")
    private String content;

    @NotNull(message = "Notification type must be specified")
    private Notification.NotificationType type;

    @NotBlank(message = "Object ID cannot be blank")
    private String objectId;

    @NotBlank(message = "Action user ID cannot be blank")
    private String actionUserId;
}