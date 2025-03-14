package com.dama.wanderwave.notification;

import com.dama.wanderwave.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Builder
@AllArgsConstructor
@Table(name = "notifications")
@ToString
public class Notification {

	@Id
	@GeneratedValue(generator = "hash_generator")
	@GenericGenerator(name = "hash_generator", type = com.dama.wanderwave.hash.HashUUIDGenerator.class)
	@Column(name = "notification_id", nullable = false, updatable = false)
	private String id;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "recipient_id", nullable = false, referencedColumnName = "user_id", foreignKey = @ForeignKey(name = "fk_notification_recipient_user"))
	@NotNull(message = "Recipient must be specified")
	private User recipient;

	@Column(name = "is_read", nullable = false)
	private boolean isRead = false;

	@CreatedDate
	@Column(name = "created_at", updatable = false, nullable = false)
	private LocalDateTime createdAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@NotNull(message = "Notification type must be specified")
	private NotificationType type;

	@Column(name = "object_id", nullable = false)
	private String objectId;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "action_user_id", nullable = false, referencedColumnName = "user_id", foreignKey = @ForeignKey(name = "fk_notification_sender_user"))
	private User actionUser;

	public enum NotificationType {
		LIKE, COMMENT, FOLLOW
	}
}
