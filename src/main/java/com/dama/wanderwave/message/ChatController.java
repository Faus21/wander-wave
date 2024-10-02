package com.dama.wanderwave.message;

import com.dama.wanderwave.chat.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@Controller
@RequiredArgsConstructor
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
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Mute sender's chat", description = "")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mute state changed successfully", content = @Content),
            @ApiResponse(responseCode = "404", description = "Chat room not found", content = @Content)
    })
    public ResponseEntity<Void> changeMuteState(@PathVariable("senderId") String senderId,
                                                @PathVariable("recipientId") String recipientId,
                                                @Parameter(name = "muteState") boolean muteState) {
        chatService.changeMuteState(senderId, recipientId, muteState);
        return ResponseEntity.ok().build();
    }
    // TODO get all chats
}
