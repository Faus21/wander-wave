package com.dama.wanderwave.message;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageService chatMessageService;

    @MessageMapping("/chat")
    @Operation(summary = "Process chat message", description = "Sends a chat message to the recipient.")
    public void processMessage(@Payload ChatMessage chatMessage) {
        ChatMessage savedMsg = chatMessageService.save(chatMessage);
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
            @ApiResponse(responseCode = "200", description = "Messages retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Chat messages not found")
    })
    public ResponseEntity<List<ChatMessage>> findChatMessages(
            @Parameter(description = "ID of the sender") @PathVariable("senderId") String senderId,
            @Parameter(description = "ID of the recipient") @PathVariable("recipientId") String recipientId) {
        List<ChatMessage> chatMessages = chatMessageService.findChatMessages(senderId, recipientId);
        return ResponseEntity.ok(chatMessages);
    }
}
