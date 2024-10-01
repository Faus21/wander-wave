package com.dama.wanderwave.message;

import com.dama.wanderwave.chat.Chat;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "messages")
public class ChatMessage {

	@EmbeddedId
	private ChatMessageKey id;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "chat_id", nullable = false, referencedColumnName = "chat_id", foreignKey = @ForeignKey(name = "fk_message_chat"))
	@NotNull(message = "Chat must be specified")
	private Chat chat;

	@NotBlank(message = "Message content cannot be blank")
	@Column(length = 1024, nullable = false)
	private String content;

	@CreatedDate
	@Column(name = "created_at", updatable = false, nullable = false)
	private LocalDateTime createdAt;

	public String getSenderId() {
		return id.getSenderId();
	}

	public String  getRecipientId() {
		return id.getRecipientId();
	}
}