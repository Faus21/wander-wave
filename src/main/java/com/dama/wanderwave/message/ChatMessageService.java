package com.dama.wanderwave.message;

import com.dama.wanderwave.chat.Chat;
import com.dama.wanderwave.chat.ChatService;
import com.dama.wanderwave.handler.ChatRoomNotFoundException;
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
		                    .findOrCreateChatRoom(chatMessage.getSenderId(), chatMessage.getRecipientId(), true)
		                    .orElseThrow(() -> new ChatRoomNotFoundException("Chat room could not be created or found."));

        chatMessage.setChat(chat);
        return repository.save(chatMessage);
    }

    public List<ChatMessage> findChatMessages(String senderId, String recipientId) {
        Optional<Chat> chatRoom = chatService.findOrCreateChatRoom(senderId, recipientId, false);

        return chatRoom.map(room -> repository.findByChatId(room.getId()))
                       .orElseGet(Collections::emptyList);
    }
}
