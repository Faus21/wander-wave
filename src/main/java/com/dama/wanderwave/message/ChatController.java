package com.dama.wanderwave.message;

import com.dama.wanderwave.chat.ChatListElement;
import com.dama.wanderwave.chat.ChatService;
import com.dama.wanderwave.handler.post.PostNotFoundException;
import com.dama.wanderwave.message.request.ChatMessageRequest;
import com.dama.wanderwave.message.response.ChatMessageResponse;
import com.dama.wanderwave.post.PostRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpSession;
import org.springframework.messaging.simp.user.SimpSubscription;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
//@RequestMapping("/api/chats")
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageService messageService;
    private final ChatService chatService;
    private final PostRepository postRepository;
    private final SimpUserRegistry simpUserRegistry;

    @Value("${application.frontend-url}")
    private String FRONTEND_URL;

    @MessageMapping("/chat")
    @Operation(summary = "Process chat message", description = "Sends a chat message to the recipient.")
    public void processMessage(
            @Payload ChatMessageRequest chatMessage,
            @Header("postId") String postId
    ) {
        logActiveChannels();

        log.info("Processing chat message from sender: {} to recipient: {}", chatMessage.getSenderId(), chatMessage.getRecipientId());

        if (postId != null && postId.length() == 16) {
            log.debug("Post ID provided: {}", postId);
            postRepository.findById(postId).orElseThrow(
                    () -> {
                        log.error("Post not found with id: {}", postId);
                        return new PostNotFoundException("Post not found with id: " + postId);
                    }
            );
            chatMessage.setContent(FRONTEND_URL + "/p/" + postId);
            log.debug("Updated chat message content with post URL: {}", chatMessage.getContent());
        }

        ChatMessage savedMsg = messageService.save(chatMessage);
        log.info("Chat message saved with ID: {}", savedMsg.getId());

        log.info("Sending chat notification to recipient: {}", savedMsg.getRecipientId());

        ChatNotification notification = ChatNotification.builder()
                .id(savedMsg.getId())
                .senderId(savedMsg.getSenderId())
                .recipientId(savedMsg.getRecipientId())
                .content(savedMsg.getContent())
                .build();

        messagingTemplate.convertAndSendToUser(
                savedMsg.getRecipientId(),
                "/queue/messages",
                notification
        );

        log.info("Notification sent to recipient: {}", savedMsg.getRecipientId());
//        sendChatNotification(savedMsg, token);
    }

//    void sendChatNotification(ChatMessage savedMsg, String token) {
//        log.info("Sending chat notification to recipient: {}", savedMsg.getRecipientId());
//
//        ChatNotification notification = ChatNotification.builder()
//                .id(savedMsg.getId())
//                .senderId(savedMsg.getSenderId())
//                .recipientId(savedMsg.getRecipientId())
//                .content(savedMsg.getContent())
//                .build();
//
//        Map<String, Object> headers = new HashMap<>();
//        headers.put("Authorization", token);
//
//        messagingTemplate.convertAndSendToUser(
//                savedMsg.getRecipientId(),
//                "/queue/messages",
//                notification,
//                headers
//        );
//
//        log.info("Notification sent to recipient: {}", savedMsg.getRecipientId());
//    }

    @GetMapping("/api/chats/messages/{senderId}/{recipientId}")
    @Operation(summary = "Retrieve chat messages", description = "Fetches chat messages between two users.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Messages retrieved successfully", content = @Content)
    })
    public ResponseEntity<List<ChatMessageResponse>> findChatMessages(
            @Parameter(description = "ID of the sender") @PathVariable("senderId") String senderId,
            @Parameter(description = "ID of the recipient") @PathVariable("recipientId") String recipientId) {
        log.info("Fetching chat messages between sender: {} and recipient: {}", senderId, recipientId);

        List<ChatMessageResponse> chatMessages = messageService.findChatMessages(senderId, recipientId);
        log.debug("Retrieved {} messages", chatMessages.size());

        return ResponseEntity.ok(chatMessages);
    }

    @PatchMapping("/api/chats/mute/{senderId}/{recipientId}")
    @Operation(summary = "Toggle mute state for a chat",
            description = "Changes the mute state of the chat between the sender and recipient. " +
                    "If the chat room exists, the mute status is updated.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mute state updated successfully", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Chat room not found, mute state not changed", content = @Content())
    })
    public ResponseEntity<Void> changeMuteState(@PathVariable("senderId") String senderId,
                                                @PathVariable("recipientId") String recipientId,
                                                @Parameter(name = "muteState", description = "Desired mute state for the chat") boolean muteState) {
        log.info("Changing mute state for chat between sender: {} and recipient: {} to: {}", senderId, recipientId, muteState);

        chatService.changeMuteState(senderId, recipientId, muteState);
        log.debug("Mute state updated successfully");

        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/chats/contacts")
    @Operation(summary = "Get contacts with last messages",
            description = "Fetches the list of contacts along with their latest message.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contacts and messages retrieved successfully", content = @Content)
    })
    public ResponseEntity<List<ChatListElement>> retrieveContactsWithLastMessages() {
        log.info("Retrieving contacts with last messages");

        List<ChatListElement> contacts = chatService.retrieveContactsWithLastMessages();
        log.debug("Retrieved {} contacts", contacts.size());

        return ResponseEntity.ok(contacts);
    }

    @DeleteMapping("/api/chats/clear/{senderId}/{recipientId}")
    @Operation(summary = "Clear chat messages",
            description = "Deletes all messages in the chat between the sender and recipient.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chat cleared successfully", content = @Content),
            @ApiResponse(responseCode = "404", description = "Chat room not found", content = @Content)
    })
    public ResponseEntity<Void> clearChat(@PathVariable("senderId") String senderId,
                                          @PathVariable("recipientId") String recipientId) {
        log.info("Clearing chat between sender: {} and recipient: {}", senderId, recipientId);

        chatService.clearChat(senderId, recipientId);
        log.debug("Chat cleared successfully");

        return ResponseEntity.ok().build();
    }

    private void logActiveChannels() {
        log.info("Active WebSocket channels:");
        for (SimpUser user : simpUserRegistry.getUsers()) {
            log.info("User: {}", user.getName());
            for (SimpSession session : user.getSessions()) {
                for (SimpSubscription subscription : session.getSubscriptions()) {
                    log.info("  - Channel: {}", subscription.getDestination());
                }
            }
        }
    }
}