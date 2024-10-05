package com.dama.wanderwave.message;

import com.dama.wanderwave.chat.Chat;
import com.dama.wanderwave.chat.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @Mock
    private ChatMessageRepository repository;

    @Mock
    private ChatService chatService;

    @InjectMocks
    private ChatMessageService chatMessageService;

    private ChatMessage chatMessage;
    private Chat chat;

    @BeforeEach
    void setUp() {
        chat = new Chat();
        chat.setId("chatId");

        chatMessage = new ChatMessage();
        chatMessage.setId(new ChatMessageKey("senderId", "recipientId"));
        chatMessage.setChat(chat);
    }

    @Test
    @DisplayName("Should save chat message when chat exists")
    void save_shouldSaveChatMessage_whenChatExists() {

        when(chatService.findOrCreateChatRoom("senderId", "recipientId", true)).thenReturn(Optional.of(chat));
        when(repository.save(any(ChatMessage.class))).thenReturn(chatMessage);


        ChatMessage savedMessage = chatMessageService.save(chatMessage);


        assertThat(savedMessage).isNotNull();
        assertThat(savedMessage.getChat()).isEqualTo(chat);
        verify(chatService).findOrCreateChatRoom("senderId", "recipientId", true);
        verify(repository).save(chatMessage);
    }

    @Test
    @DisplayName("Should throw exception when chat does not exist")
    void save_shouldThrowException_whenChatDoesNotExist() {

        when(chatService.findOrCreateChatRoom("senderId", "recipientId", true)).thenReturn(Optional.empty());


        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> chatMessageService.save(chatMessage))
                .withMessage("Chat room could not be created or found.");

        verify(chatService).findOrCreateChatRoom("senderId", "recipientId", true);
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @MethodSource("provideChatRoomsForFindChatMessages")
    @DisplayName("Should return chat messages based on chat existence")
    void findChatMessages_shouldReturnMessagesBasedOnChatExistence(boolean chatExists, List<ChatMessage> expectedMessages) {

        when(chatService.findOrCreateChatRoom("senderId", "recipientId", false)).thenReturn(chatExists ? Optional.of(chat) : Optional.empty());

        if (chatExists) {
            when(repository.findByChatId(chat.getId())).thenReturn(expectedMessages);
        }

        List<ChatMessage> messages = chatMessageService.findChatMessages("senderId", "recipientId");

        assertThat(messages).isEqualTo(expectedMessages);
        verify(chatService).findOrCreateChatRoom("senderId", "recipientId", false);
        if (chatExists) {
            verify(repository).findByChatId(chat.getId());
        } else {
            verify(repository, never()).findByChatId(any());
        }
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> provideChatRoomsForFindChatMessages() {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setId(new ChatMessageKey("senderId", "recipientId"));

        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of(true, List.of(chatMessage)),
                org.junit.jupiter.params.provider.Arguments.of(false, Collections.emptyList())
        );
    }
}
