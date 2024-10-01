package com.dama.wanderwave.message;

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
    public void processMessage(@Payload ChatMessage chatMessage) {

        ChatMessage savedMsg = chatMessageService.save(chatMessage);
        sendChatNotification(savedMsg);
    }

    private void sendChatNotification(ChatMessage savedMsg) {
        ChatNotification notification = ChatNotification.builder()
                                                .id(savedMsg.getId().toString())
                                                .senderId(savedMsg.getSenderId())
                                                .recipientId(savedMsg.getRecipientId())
                                                .content(savedMsg.getContent())
                                                .build();

        messagingTemplate.convertAndSendToUser(
                savedMsg.getId().getRecipientId(),
                "/queue/messages",
                notification
        );
    }

    @GetMapping("/messages/{senderId}/{recipientId}")
    public ResponseEntity<List<ChatMessage>> findChatMessages(@PathVariable("senderId") String senderId,
                                                              @PathVariable("recipientId") String recipientId) {
        List<ChatMessage> chatMessages = chatMessageService.findChatMessages(senderId, recipientId);
        return ResponseEntity.ok(chatMessages);
    }
}
