package com.dama.wanderwave.message;

import com.dama.wanderwave.chatroom.Chat;
import com.dama.wanderwave.chatroom.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatMessageService {
    private final ChatMessageRepository repository;
    private final ChatService chatService;

    public ChatMessage save(ChatMessage chatMessage) {
        Chat chat = chatService
		                    .getChatRoom(chatMessage.getSenderId(), chatMessage.getRecipientId(), true)
		                    .orElseThrow(() -> new RuntimeException("Chat room could not be created or found."));

        chatMessage.setChat(chat);
        return repository.save(chatMessage);
    }

    public List<ChatMessage> findChatMessages(String senderId, String recipientId) {
        Optional<Chat> chatRoom = chatService.getChatRoom(senderId, recipientId, false);

        return chatRoom.map(room -> repository.findByChatId(room.getId()))
                       .orElseGet(Collections::emptyList);
    }
}
