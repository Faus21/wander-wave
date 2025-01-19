package com.dama.wanderwave.chat;

import com.dama.wanderwave.handler.chat.ChatRoomException;
import com.dama.wanderwave.handler.chat.ChatRoomNotFoundException;
import com.dama.wanderwave.handler.user.UserNotFoundException;
import com.dama.wanderwave.hash.HashUUIDGenerator;
import com.dama.wanderwave.user.User;
import com.dama.wanderwave.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService Tests")
class ChatServiceTest {

	@InjectMocks
	private ChatService chatService;

	@Mock
	private ChatRepository chatRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private HashUUIDGenerator hashUUIDGenerator;



	private User sender;
	private User recipient;
	private Chat expectedChat;

	@BeforeEach
	void setUp() {
		sender = new User();
		sender.setId("senderId");
		sender.setNickname("Sender");

		recipient = new User();
		recipient.setId("recipientId");
		recipient.setNickname("Recipient");

		expectedChat = Chat.builder()
				               .id("chatId")
				               .sender(sender)
				               .recipient(recipient)
				               .build();
	}

	@Nested
	@DisplayName("getChatRoom Method")
	class FindOrCreateChatRoom {

		@Test
		@DisplayName("Should return existing chat when it exists")
		void findOrCreateChatRoom_ShouldReturnExistingChat_WhenExists() {
			when(chatRepository.findBySenderIdAndRecipientId(sender.getId(), recipient.getId()))
					.thenReturn(Optional.of(expectedChat));

			Optional<Chat> actualChat = chatService.findOrCreateChatRoom(sender.getId(), recipient.getId(), false);

			assertThat(actualChat).isPresent().contains(expectedChat);
			verify(chatRepository).findBySenderIdAndRecipientId(sender.getId(), recipient.getId());
		}

		@Test
		@DisplayName("Should return new chat when not exists and createNewRoom flag is true")
		void getChatRoom_ShouldReturnNewChat_WhenNotExistsAndCreateNewRoomFlagIsTrue() {
			when(chatRepository.findBySenderIdAndRecipientId(sender.getId(), recipient.getId()))
					.thenReturn(Optional.empty());
			when(userRepository.findById(sender.getId())).thenReturn(Optional.of(sender));
			when(userRepository.findById(recipient.getId())).thenReturn(Optional.of(recipient));
			when(hashUUIDGenerator.encodeString(sender.getId() + recipient.getId())).thenReturn("chatId");
			when(chatRepository.save(any(Chat.class))).thenReturn(expectedChat);

			Optional<Chat> actualChat = chatService.findOrCreateChatRoom(sender.getId(), recipient.getId(), true);

			assertThat(actualChat).isPresent().contains(expectedChat);
			verify(chatRepository).save(any(Chat.class));
			verify(hashUUIDGenerator).encodeString(sender.getId() + recipient.getId());
		}

		@Test
		@DisplayName("Should not create new chat when not exists and createNewRoom flag is false")
		void findOrCreateChatRoom_ShouldNotCreateNewChat_WhenNotExistsAndCreateNewRoomFlagIsFalse() {
			when(chatRepository.findBySenderIdAndRecipientId(sender.getId(), recipient.getId()))
					.thenReturn(Optional.empty());

			Optional<Chat> actualChat = chatService.findOrCreateChatRoom(sender.getId(), recipient.getId(), false);

			assertThat(actualChat).isNotPresent();
			verify(chatRepository).findBySenderIdAndRecipientId(sender.getId(), recipient.getId());
		}
	}

	@Nested
	@DisplayName("createNewChatRoom Method")
	class CreateNewChatRoomTests {

		@Test
		@DisplayName("Should throw UserNotFoundException when sender does not exist")
		void createNewChatRoom_ShouldThrowUserNotFoundException_WhenSenderDoesNotExist() {
			when(userRepository.findById(sender.getId())).thenReturn(Optional.empty());

			assertThatThrownBy(() -> chatService.createNewChatRoom(sender.getId(), recipient.getId()))
					.isInstanceOf(UserNotFoundException.class)
					.hasMessageContaining("User not found! ID::senderId");
		}

		@Test
		@DisplayName("Should throw UserNotFoundException when recipient does not exist")
		void createNewChatRoom_ShouldThrowUserNotFoundException_WhenRecipientDoesNotExist() {
			when(userRepository.findById(sender.getId())).thenReturn(Optional.of(sender));
			when(userRepository.findById(recipient.getId())).thenReturn(Optional.empty());

			assertThatThrownBy(() -> chatService.createNewChatRoom(sender.getId(), recipient.getId()))
					.isInstanceOf(UserNotFoundException.class)
					.hasMessageContaining("User not found! ID::recipientId");
		}

		@Test
		@DisplayName("Should create new chat when users exist")
		void createNewChatRoom_ShouldCreateNewChat_WhenUsersExist() {
			when(userRepository.findById(sender.getId())).thenReturn(Optional.of(sender));
			when(userRepository.findById(recipient.getId())).thenReturn(Optional.of(recipient));
			when(hashUUIDGenerator.encodeString(sender.getId() + recipient.getId())).thenReturn("chatId");
			when(chatRepository.save(any(Chat.class))).thenReturn(expectedChat);

			Chat actualChat = chatService.createNewChatRoom(sender.getId(), recipient.getId());

			assertThat(actualChat)
					.isNotNull()
					.extracting(Chat::getId, Chat::getSender, Chat::getRecipient)
					.containsExactly("chatId", sender, recipient);

			verify(chatRepository).save(any(Chat.class));
			verify(userRepository).findById(sender.getId());
			verify(userRepository).findById(recipient.getId());
			verify(hashUUIDGenerator).encodeString(sender.getId() + recipient.getId());
		}
	}

	@Nested
	@DisplayName("createChatId Method")
	class CreateChatIdTests {

		@Test
		@DisplayName("Should return encoded chat ID")
		void createChatId_ShouldReturnEncodedChatId() {
			String expectedChatId = "encodedChatId";
			String combinedIds = sender.getId() + recipient.getId();

			when(hashUUIDGenerator.encodeString(combinedIds)).thenReturn(expectedChatId);

			String actualChatId = chatService.createChatId(sender.getId(), recipient.getId());

			assertThat(actualChatId).isEqualTo(expectedChatId);
		}

		@Test
		@DisplayName("Should return same encoded chat ID when IDs are reversed")
		void createChatId_ShouldReturnSameEncodedChatId_WhenIdsAreReversed() {
			String expectedChatId = "encodedChatId";
			String combinedIds = recipient.getId() + sender.getId();

			when(hashUUIDGenerator.encodeString(combinedIds)).thenReturn(expectedChatId);

			String actualChatId = chatService.createChatId(recipient.getId(), sender.getId());

			assertThat(actualChatId).isEqualTo(expectedChatId);
		}
	}

	@Nested
	@DisplayName("getUserOrThrow Method")
	class GetUserOrThrowTests {

		@Test
		@DisplayName("Should return user when user exists")
		void getUserOrThrow_ShouldReturnUser_WhenUserExists() {
			when(userRepository.findById(sender.getId())).thenReturn(Optional.of(sender));

			User actualUser = chatService.getUserOrThrow(sender.getId());

			assertThat(actualUser).isEqualTo(sender);
		}

		@Test
		@DisplayName("Should throw UserNotFoundException when user does not exist")
		void getUserOrThrow_ShouldThrowUserNotFoundException_WhenUserDoesNotExist() {
			when(userRepository.findById(sender.getId())).thenReturn(Optional.empty());

			assertThatThrownBy(() -> chatService.getUserOrThrow(sender.getId()))
					.isInstanceOf(UserNotFoundException.class)
					.hasMessageContaining("User not found! ID::senderId");
		}
	}

	@Nested
	@DisplayName("createAndSaveChatRoom Method")
	class CreateAndSaveChatRoomTests {

		@Test
		@DisplayName("Should create and save chat room with success")
		void createAndSaveChatRoom_ShouldCreateAndSaveChatRoom_WithSuccess() {
			when(chatRepository.save(any(Chat.class))).thenReturn(expectedChat);

			Chat actualChat = chatService.createAndSaveChatRoom("chatId", sender, recipient);

			assertThat(actualChat)
					.isNotNull()
					.hasFieldOrPropertyWithValue("id", expectedChat.getId())
					.hasFieldOrPropertyWithValue("sender.id", expectedChat.getSender().getId())
					.hasFieldOrPropertyWithValue("recipient.id", expectedChat.getRecipient().getId());

			verify(chatRepository).save(any(Chat.class));
		}

		@Test
		@DisplayName("Should throw ChatRoomException when sender is null")
		void createAndSaveChatRoom_ShouldThrowChatRoomException_WhenSenderIsNull() {
			assertThatThrownBy(() -> chatService.createAndSaveChatRoom("chatId", null, recipient))
					.isInstanceOf(ChatRoomException.class)
					.hasMessageContaining("Chat attributes cannot be null");
		}

		@Test
		@DisplayName("Should throw ChatRoomException when recipient is null")
		void createAndSaveChatRoom_ShouldThrowChatRoomException_WhenRecipientIsNull() {
			assertThatThrownBy(() -> chatService.createAndSaveChatRoom("chatId", sender, null))
					.isInstanceOf(ChatRoomException.class)
					.hasMessageContaining("Chat attributes cannot be null");
		}

		@Test
		@DisplayName("Should throw IllegalArgumentException when chat ID is null")
		void createAndSaveChatRoom_ShouldThrowIllegalArgumentException_WhenChatIdIsNull() {
			assertThatThrownBy(() -> chatService.createAndSaveChatRoom(null, sender, recipient))
					.isInstanceOf(IllegalArgumentException.class);
		}
	}


	@Nested
	@DisplayName("changeMuteState Method")
	class ChangeMuteStateTests {

		@Test
		@DisplayName("Should change mute state successfully")
		void changeMuteState_ShouldChangeStateSuccessfully() {
			String senderId = "senderId";
			String recipientId = "recipientId";
			boolean muteState = true;

			Chat chatRoom = Chat.builder()
					                .id("chatId")
					                .sender(sender)
					                .recipient(recipient)
					                .muted(false)
					                .build();

			when(chatRepository.findBySenderIdAndRecipientId(senderId, recipientId)).thenReturn(Optional.of(chatRoom));
			when(chatRepository.save(chatRoom)).thenReturn(chatRoom);

			chatService.changeMuteState(senderId, recipientId, muteState);

			assertThat(chatRoom.isMuted()).isTrue();
			verify(chatRepository, times(1)).save(chatRoom);
		}

		@Test
		@DisplayName("Should throw ChatRoomNotFoundException when chat room not found")
		void changeMuteState_ShouldThrowChatRoomNotFoundException_WhenChatRoomNotFound() {
			String senderId = "senderId";
			String recipientId = "recipientId";
			boolean muteState = true;

			when(chatRepository.findBySenderIdAndRecipientId(senderId, recipientId)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> chatService.changeMuteState(senderId, recipientId, muteState))
					.isInstanceOf(ChatRoomNotFoundException.class)
					.hasMessageContaining("Chat room could not be found.");
		}
	}
}