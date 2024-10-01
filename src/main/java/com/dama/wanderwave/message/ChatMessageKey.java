package com.dama.wanderwave.message;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class ChatMessageKey implements Serializable {
    @NotNull(message = "Sender ID must be specified")
    private String  senderId;

    @NotNull(message = "Recipient ID must be specified")
    private String recipientId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatMessageKey that = (ChatMessageKey) o;
        return Objects.equals(senderId, that.senderId) && Objects.equals(recipientId, that.recipientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(senderId, recipientId);
    }
}