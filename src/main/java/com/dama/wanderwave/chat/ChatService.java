package com.dama.wanderwave.chat;

import com.dama.wanderwave.handler.ChatRoomException;
import com.dama.wanderwave.handler.UserNotFoundException;
import com.dama.wanderwave.hash.HashUUIDGenerator;
import com.dama.wanderwave.user.User;
import com.dama.wanderwave.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final HashUUIDGenerator hashUUIDGenerator;

    public Optional<Chat> getChatRoom(
            String senderId,
            String recipientId,
            boolean createNewRoomIfNotExists
    ) {
        return chatRepository
                       .findBySenderIdAndRecipientId(senderId, recipientId)
                       .or(() -> createNewRoomIfNotExists
                                         ? Optional.of(createNewChatRoom(senderId, recipientId))
                                         : Optional.empty()
                       );
    }


    Chat createNewChatRoom( String senderId, String recipientId ) {
        String chatId = createChatId(senderId, recipientId);

        User sender = getUserOrThrow(senderId);
        User recipient = getUserOrThrow(recipientId);

        Chat chat = createAndSaveChatRoom(chatId, sender, recipient);
        createAndSaveChatRoom(chatId, recipient, sender );

        return chat;
    }

    String createChatId( String senderId, String recipientId ) {
        return hashUUIDGenerator.encodeString(senderId + recipientId);
    }

    User getUserOrThrow( String userId ) {
        return userRepository.findById(userId)
                       .orElseThrow(() -> new UserNotFoundException(String.format("User not found! ID::%s", userId)));
    }

    protected Chat createAndSaveChatRoom(String chatId, User sender, User recipient) {

        if (chatId == null || sender == null || recipient == null) {
            throw new ChatRoomException("Chat attributes cannot be null");
        }

        Chat chat = Chat.builder()
                            .id(chatId)
                            .sender(sender)
                            .recipient(recipient)
                            .build();

        return chatRepository.save(chat);
    }

}