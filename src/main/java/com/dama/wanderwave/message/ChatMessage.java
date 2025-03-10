package com.dama.wanderwave.message;

import com.dama.wanderwave.chat.Chat;
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
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "messages")
public class ChatMessage {

    @Id
    @GeneratedValue(generator = "hash_generator")
    @GenericGenerator(name = "hash_generator", type = com.dama.wanderwave.hash.HashUUIDGenerator.class)
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    @NotNull(message = "Sender ID must be specified")
    private String senderId;

    @NotNull(message = "Recipient ID must be specified")
    private String recipientId;

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

}