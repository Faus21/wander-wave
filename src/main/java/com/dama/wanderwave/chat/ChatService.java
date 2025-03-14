package com.dama.wanderwave.chat;

import com.dama.wanderwave.handler.chat.ChatRoomException;
import com.dama.wanderwave.handler.chat.ChatRoomNotFoundException;
import com.dama.wanderwave.handler.user.UserNotFoundException;
import com.dama.wanderwave.hash.HashUUIDGenerator;
import com.dama.wanderwave.user.User;
import com.dama.wanderwave.user.UserRepository;
import com.dama.wanderwave.user.UserService;
import jakarta.persistence.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ChatService {

    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final HashUUIDGenerator hashUUIDGenerator;
    private final UserService userService;


    public Optional<Chat> findOrCreateChatRoom(String senderId, String recipientId, boolean createNewRoomIfNotExists) {
        log.info("Getting chat room between senderId: {} and recipientId: {}, createNewRoomIfNotExists: {}", senderId, recipientId, createNewRoomIfNotExists);
        return chatRepository
                .findBySenderIdAndRecipientId(senderId, recipientId)
                .or(() -> {
                    if (createNewRoomIfNotExists) {
                        log.info("Chat room not found, creating a new one.");
                        return Optional.of(createNewChatRoom(senderId, recipientId));
                    } else {
                        log.warn("Chat room not found, and createNewRoomIfNotExists is false.");
                        return Optional.empty();
                    }
                });
    }

    public Chat findChatByUsers(String senderId, String recipientId) {
        log.info("Getting chat room between senderId: {} and recipientId: {}", senderId, recipientId);
        return chatRepository.findBySenderIdAndRecipientId(senderId, recipientId)
                .orElse(chatRepository.findBySenderIdAndRecipientId(recipientId, senderId)
                        .orElse(null));

    }

    @Transactional
    Chat createNewChatRoom(String senderId, String recipientId) {
        log.info("Creating new chat room for senderId: {} and recipientId: {}", senderId, recipientId);
        String chatId = createChatId(senderId, recipientId);
//        String reversedChatId = createChatId(recipientId, senderId);

        User sender = getUserOrThrow(senderId);
        User recipient = getUserOrThrow(recipientId);

        Chat chat = createAndSaveChatRoom(chatId, sender, recipient);
//        createAndSaveChatRoom(reversedChatId, recipient, sender);

        log.info("New chat room created with ID: {}", chatId);
        return chat;
    }

    String createChatId(String senderId, String recipientId) {
        log.debug("Generating chat ID for senderId: {} and recipientId: {}", senderId, recipientId);
        return hashUUIDGenerator.encodeString(senderId + recipientId);
    }

    User getUserOrThrow(String userId) {
        log.debug("Fetching user with ID: {}", userId);
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", userId);
                    return new UserNotFoundException(String.format("User not found! ID::%s", userId));
                });
    }

    protected Chat createAndSaveChatRoom(String chatId, User sender, User recipient) {


        if (chatId == null || sender == null || recipient == null) {
            log.error("Failed to create chat room, one of the attributes is null: chatId: {}, sender: {}, recipient: {}", chatId, sender, recipient);
            throw new ChatRoomException("Chat attributes cannot be null");
        }

        Chat chat = Chat.builder()
                .id(chatId)
                .sender(sender)
                .recipient(recipient)
                .build();

        return chatRepository.save(chat);
    }

    @Transactional
    public void changeMuteState(String senderId, String recipientId, boolean muteState) {
        log.info("Changing mute state for chat between senderId: {} and recipientId: {} to {}", senderId, recipientId, muteState);

        Chat room = findOrCreateChatRoom(senderId, recipientId, false)
                .orElseThrow(() -> {
                    log.error("Chat room not found for senderId: {} and recipientId: {}", senderId, recipientId);
                    return new ChatRoomNotFoundException("Chat room could not be found.");
                });

        room.setMuted(muteState);

        chatRepository.save(room);
        log.info("Mute state changed for chat room: {}", room.getId());
    }

    public List<ChatListElement> retrieveContactsWithLastMessages() {
        User user = userService.getAuthenticatedUser();
        return getLatestMessagesForUser(user.getId());
    }

    private List<ChatListElement> getLatestMessagesForUser(String userId) {
        List<Tuple> tuples = chatRepository.findChatListWithLastMessage(userId);

        return tuples.stream()
                .map(tuple -> {
                    LocalDateTime createdAt = Optional.ofNullable(tuple.get("createdAt", Timestamp.class))
                            .map(Timestamp::toLocalDateTime)
                            .orElse(null);

                    return ChatListElement.builder()
                            .userId(tuple.get("userId", String.class))
                            .name(tuple.get("name", String.class))
                            .imgUrl(tuple.get("imgUrl", String.class))
                            .content(tuple.get("content", String.class))
                            .createdAt(createdAt)
                            .build();
                })
                .sorted((o1, o2) -> o2.createdAt().compareTo(o1.createdAt()))
                .toList();
    }

    public void clearChat(String senderId, String recipientId) {
        Chat chatRoom = chatRepository.findBySenderIdAndRecipientId(senderId, recipientId)
                .orElse(chatRepository.findBySenderIdAndRecipientId(recipientId, senderId)
                        .orElse(null));

        if (chatRoom != null) {
            chatRepository.deleteById(chatRoom.getId());
        }
    }
}