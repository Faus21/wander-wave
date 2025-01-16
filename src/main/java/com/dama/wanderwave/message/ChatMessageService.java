package com.dama.wanderwave.message;

import com.dama.wanderwave.chat.Chat;
import com.dama.wanderwave.chat.ChatService;
import com.dama.wanderwave.handler.chat.ChatRoomNotFoundException;
import com.dama.wanderwave.message.request.ChatMessageRequest;
import com.dama.wanderwave.message.response.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatMessageService {
    private final ChatMessageRepository repository;
    private final ChatService chatService;

    public ChatMessage save(ChatMessageRequest chatMessageRequest) {
        Chat chat = chatService
                .findOrCreateChatRoom(chatMessageRequest.getSenderId(), chatMessageRequest.getRecipientId(), true)
                .orElseThrow(() -> new ChatRoomNotFoundException("Chat room could not be created or found."));

        ChatMessage chatMessage = ChatMessage.builder()
                .recipientId(chatMessageRequest.getRecipientId())
                .senderId(chatMessageRequest.getSenderId())
                .content(chatMessageRequest.getContent())
                .chat(chat)
                .createdAt(LocalDateTime.now())
                .build();

        return repository.save(chatMessage);
    }

    public List<ChatMessageResponse> findChatMessages(String senderId, String recipientId) {
        Optional<Chat> chatRoom = Optional.ofNullable(chatService.findChatByUsers(senderId, recipientId));

        return chatRoom.map(room -> repository.findByChatId(room.getId()))
                .orElseGet(Collections::emptyList)
                .stream()
                .map(this::fromChatMessage)
                .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                .toList();
    }

    public ChatMessageResponse fromChatMessage(ChatMessage chatMessage) {
        return ChatMessageResponse.builder()
                .id(chatMessage.getId())
                .chatId(chatMessage.getChat().getId())
                .senderId(chatMessage.getSenderId())
                .recipientId(chatMessage.getRecipientId())
                .createdAt(chatMessage.getCreatedAt())
                .content(chatMessage.getContent())
                .build();
    }
}
