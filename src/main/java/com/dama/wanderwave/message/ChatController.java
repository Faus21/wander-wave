package com.dama.wanderwave.message;

import com.dama.wanderwave.chat.ChatListElement;
import com.dama.wanderwave.chat.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/chats")
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageService messageService;
    private final ChatService chatService;

    @MessageMapping("/chat")
    @Operation(summary = "Process chat message", description = "Sends a chat message to the recipient.")
    public void processMessage(@Payload ChatMessage chatMessage) {
        ChatMessage savedMsg = messageService.save(chatMessage);
        sendChatNotification(savedMsg);
    }

    void sendChatNotification( ChatMessage savedMsg ) {
        ChatNotification notification = ChatNotification.builder()
                                                .id(savedMsg.getId().toString())
                                                .senderId(savedMsg.getSenderId())
                                                .recipientId(savedMsg.getRecipientId())
                                                .content(savedMsg.getContent())
                                                .build();

        messagingTemplate.convertAndSendToUser(
                savedMsg.getRecipientId(),
                "/queue/messages",
                notification
        );
    }

    @GetMapping("/messages/{senderId}/{recipientId}")
    @Operation(summary = "Retrieve chat messages", description = "Fetches chat messages between two users.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Messages retrieved successfully", content = @Content)
    })
    public ResponseEntity<List<ChatMessage>> findChatMessages(
            @Parameter(description = "ID of the sender") @PathVariable("senderId") String senderId,
            @Parameter(description = "ID of the recipient") @PathVariable("recipientId") String recipientId) {
        List<ChatMessage> chatMessages = messageService.findChatMessages(senderId, recipientId);
        return ResponseEntity.ok(chatMessages);
    }


    @PatchMapping("/mute/{senderId}/{recipientId}")
    @Operation(summary = "Toggle mute state for a chat",
            description = "Changes the mute state of the chat between the sender and recipient. " +
                                  "If the chat room exists, the mute status is updated.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mute state updated successfully", content = @Content),
            @ApiResponse(responseCode = "404", description = "Chat room not found, mute state not changed", content = @Content)
    })
    public ResponseEntity<Void> changeMuteState(@PathVariable("senderId") String senderId,
                                                @PathVariable("recipientId") String recipientId,
                                                @Parameter(name = "muteState", description = "Desired mute state for the chat") boolean muteState) {
        chatService.changeMuteState(senderId, recipientId, muteState);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{userId}/contacts")
    @Operation(summary = "Get contacts with last messages",
            description = "Fetches the list of contacts along with their latest message.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contacts and messages retrieved successfully", content = @Content)
    })
    public ResponseEntity<List<ChatListElement>> retrieveContactsWithLastMessages( @PathVariable String userId) {
        List<ChatListElement> contacts = chatService.retrieveContactsWithLastMessages(userId);
        return ResponseEntity.ok(contacts);
    }
}
