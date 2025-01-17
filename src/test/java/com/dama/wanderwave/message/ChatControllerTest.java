package com.dama.wanderwave.message;

import com.dama.wanderwave.chat.Chat;
import com.dama.wanderwave.chat.ChatService;
import com.dama.wanderwave.message.request.ChatMessageRequest;
import com.dama.wanderwave.message.response.ChatMessageResponse;
import com.dama.wanderwave.post.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatController Tests")
class ChatControllerTest {

    @InjectMocks
    private ChatController chatController;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private ChatMessageService chatMessageService;

    @Mock
    private PostRepository postRepository;

    @Mock
    private ChatService chatService;

    private ChatMessage chatMessage;
    private ChatNotification chatNotification;

    @BeforeEach
    void setUp() {
        ChatMessageKey chatMessageKey = new ChatMessageKey("senderId", "recipientId");
        Chat chat = new Chat();
        chat.setId("chatId");

        chatMessage = new ChatMessage();
        chatMessage.setChat(chat);
        chatMessage.setContent("Hello");
        chatMessage.setCreatedAt(LocalDateTime.now());

        chatNotification = ChatNotification.builder()
                                   .id(chatMessageKey.toString())
                                   .senderId(chatMessage.getSenderId())
                                   .recipientId(chatMessage.getRecipientId())
                                   .content(chatMessage.getContent())
                                   .build();
    }

    @Nested
    @DisplayName("processMessage Method")
    class ProcessMessageTests {

        @Test
        @DisplayName("Should process and save chat message")
        void processMessageShouldSaveAndSendNotification() {
            when(chatMessageService.save(any(ChatMessageRequest.class))).thenReturn(chatMessage);

            chatController.processMessage(ChatMessageRequest.builder().build(), "");

            verify(chatMessageService, times(1)).save(any(ChatMessageRequest.class));
            verify(messagingTemplate, times(1)).convertAndSendToUser(
                    anyString(),
                    anyString(),
                    any(ChatNotification.class)
            );
        }

        @Test
        @DisplayName("Should throw exception if saving message fails")
        void processMessageShouldThrowExceptionIfSaveFails() {
//            when(chatMessageService.save(any(ChatMessageRequest.class))).thenThrow(new RuntimeException("Save failed"));

            assertThrows(RuntimeException.class, () -> chatController.processMessage(ChatMessageRequest.builder().build(), ""));
        }
    }

    @Nested
    @DisplayName("sendChatNotification Method")
    class SendChatNotificationTests {

        @Test
        @DisplayName("Should send chat notification")
        void sendChatNotificationShouldSendNotification() {
//            chatController.sendChatNotification(chatMessage);

            verify(messagingTemplate, times(1)).convertAndSendToUser(
                    anyString(),
                    anyString(),
                    any(ChatNotification.class)
            );
        }

        @Test
        @DisplayName("Should throw exception if sending notification fails")
        void sendChatNotificationShouldThrowExceptionIfSendFails() {
            doThrow(new RuntimeException("Send failed")).when(messagingTemplate).convertAndSendToUser(
                    chatMessage.getRecipientId(),
                    "/queue/messages",
                    chatNotification
            );

//            assertThrows(RuntimeException.class, () -> chatController.sendChatNotification(chatMessage));
        }
    }

    @Nested
    @DisplayName("findChatMessages Method")
    class FindChatMessagesTests {

        @Test
        @DisplayName("Should return chat messages between two users")
        void findChatMessagesShouldReturnMessages() {
            List<ChatMessageResponse> chatMessages = Collections.singletonList(chatMessage)
                    .stream().map(m -> chatMessageService.fromChatMessage(m)).toList();
            when(chatMessageService.findChatMessages("senderId", "recipientId")).thenReturn(chatMessages);

            ResponseEntity<List<ChatMessageResponse>> response = chatController.findChatMessages("senderId", "recipientId");

            assertEquals(200, response.getStatusCode().value());
            assertEquals(chatMessages, response.getBody());
        }

        @Test
        @DisplayName("Should return empty list if no messages found")
        void findChatMessagesShouldReturnEmptyList() {
            when(chatMessageService.findChatMessages("senderId", "recipientId")).thenReturn(Collections.emptyList());

            ResponseEntity<List<ChatMessageResponse>> response = chatController.findChatMessages("senderId", "recipientId");

            assertEquals(200, response.getStatusCode().value());
            assertEquals(Collections.emptyList(), response.getBody());
        }

        @Test
        @DisplayName("Should throw exception if finding messages fails")
        void findChatMessagesShouldThrowExceptionIfFindFails() {
            when(chatMessageService.findChatMessages("senderId", "recipientId")).thenThrow(new RuntimeException("Find failed"));

            assertThrows(RuntimeException.class, () -> chatController.findChatMessages("senderId", "recipientId"));
        }
    }

    @Nested
    @DisplayName("changeMuteState Method")
    class ChangeMuteStateTests {

        @Test
        @DisplayName("Should change mute state successfully")
        void changeMuteStateShouldChangeStateSuccessfully() {
            String senderId = "senderId";
            String recipientId = "recipientId";
            boolean muteState = true;

            ResponseEntity<Void> response = chatController.changeMuteState(senderId, recipientId, muteState);

            verify(chatService, times(1)).changeMuteState(senderId, recipientId, muteState);
            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("Should throw exception if chat room not found")
        void changeMuteStateShouldThrowExceptionIfChatRoomNotFound() {
            String senderId = "senderId";
            String recipientId = "recipientId";
            boolean muteState = true;

            doThrow(new RuntimeException("Chat room not found")).when(chatService).changeMuteState(senderId, recipientId, muteState);

            assertThrows(RuntimeException.class, () -> chatController.changeMuteState(senderId, recipientId, muteState));
        }
    }
}
