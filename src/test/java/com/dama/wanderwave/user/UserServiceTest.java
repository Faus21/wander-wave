package com.dama.wanderwave.user;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.dama.wanderwave.handler.user.UserNotFoundException;
import com.dama.wanderwave.user.request.BlockRequest;
import com.dama.wanderwave.user.request.SubscribeRequest;
import com.dama.wanderwave.user.response.UserResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private final static int SUBSCRIPTIONS_PAGE = 10;
    private static final int ID_LENGTH = 16;

    @Mock
    private UserRepository userRepository;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        authentication = mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Nested
    class GetUserByIdTest {

        @Test
        @DisplayName("Get user by ID should return user response when user exists")
        void getUserById_UserExists() {
            String userId = "mockId";
            User mockUser = getMockUser();
            when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

            UserResponse result = userService.getUserById(userId);

            assertEquals(mockUser.getId(), result.getId());
            assertEquals("Mock User", result.getNickname());
            verify(userRepository).findById(userId);
        }

        @Test
        @DisplayName("Get user by ID should throw UserNotFoundException when user does not exist")
        void getUserById_UserNotFound() {
            String userId = "mockId";
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> userService.getUserById(userId));
        }
    }

    @Nested
    class UpdateSubscriptionTest {

        @Test
        @DisplayName("Update subscription should be successful when subscribing")
        void updateSubscription_Subscribe_Success() {
            SubscribeRequest request = new SubscribeRequest("followerId", "followedId");

            User mockAuthenticatedUser = getMockUser("followerId", "follower@example.com");
            User mockFollowedUser = getMockUser("followedId", "followed@example.com");

            when(userRepository.findById("followerId")).thenReturn(Optional.of(mockAuthenticatedUser));
            when(userRepository.findById("followedId")).thenReturn(Optional.of(mockFollowedUser));

            String response = userService.updateSubscription(request, true);

            assertEquals("User subscribed successfully", response);

            verify(userRepository).save(mockAuthenticatedUser);
            verify(userRepository).save(mockFollowedUser);
        }

        @Test
        @DisplayName("Update subscription should be successful when unsubscribing")
        void updateSubscription_Unsubscribe_Success() {
            SubscribeRequest request = new SubscribeRequest("followerId", "followedId");

            User mockAuthenticatedUser = getMockUser("followerId", "follower@example.com");
            User mockFollowedUser = getMockUser("followedId", "followed@example.com");

            mockAuthenticatedUser.getSubscriptions().add(mockFollowedUser.getId());

            when(userRepository.findById("followerId")).thenReturn(Optional.of(mockAuthenticatedUser));
            when(userRepository.findById("followedId")).thenReturn(Optional.of(mockFollowedUser));

            String response = userService.updateSubscription(request, false);

            assertEquals("User unsubscribed successfully", response);
            assertEquals(mockAuthenticatedUser.getSubscriptions().size(), 0);

            verify(userRepository).save(mockAuthenticatedUser);
            verify(userRepository).save(mockFollowedUser);
        }

        @Test
        @DisplayName("Update subscription should return 'Already subscribed' when already subscribed")
        void updateSubscription_AlreadySubscribed() {
            SubscribeRequest request = new SubscribeRequest("followerId", "followedId");

            User mockAuthenticatedUser = getMockUser("followerId", "follower@example.com");
            User mockFollowedUser = getMockUser("followedId", "followed@example.com");

            mockAuthenticatedUser.getSubscriptions().add("followedId");

            when(userRepository.findById("followerId")).thenReturn(Optional.of(mockAuthenticatedUser));
            when(userRepository.findById("followedId")).thenReturn(Optional.of(mockFollowedUser));

            String response = userService.updateSubscription(request, true);

            assertEquals("Already subscribed", response);
        }

        @Test
        @DisplayName("Update subscription should return 'Not subscribed, cannot unsubscribe' when not subscribed")
        void updateSubscription_NotSubscribed_CannotUnsubscribe() {
            SubscribeRequest request = new SubscribeRequest("followerId", "followedId");

            User mockAuthenticatedUser = getMockUser("followerId", "follower@example.com");
            User mockFollowedUser = getMockUser("followedId", "followed@example.com");

            when(userRepository.findById("followerId")).thenReturn(Optional.of(mockAuthenticatedUser));
            when(userRepository.findById("followedId")).thenReturn(Optional.of(mockFollowedUser));

            String response = userService.updateSubscription(request, false);

            assertEquals("Not subscribed, cannot unsubscribe", response);

            verify(userRepository, never()).save(mockAuthenticatedUser);
            verify(userRepository, never()).save(mockFollowedUser);
        }


        @Test
        @DisplayName("Update subscription should throw UserNotFoundException when follower not found")
        void updateSubscription_FollowerNotFound() {
            SubscribeRequest request = new SubscribeRequest("followerId", "followedId");

            when(userRepository.findById("followerId")).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> userService.updateSubscription(request, true));
        }

        @Test
        @DisplayName("Update subscription should throw UserNotFoundException when followed user not found")
        void updateSubscription_FollowedNotFound() {
            SubscribeRequest request = new SubscribeRequest("followerId", "followedId");

            User mockAuthenticatedUser = getMockUser("followerId", "follower@example.com");

            when(userRepository.findById("followerId")).thenReturn(Optional.of(mockAuthenticatedUser));
            when(userRepository.findById("followedId")).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> userService.updateSubscription(request, true));
        }
    }


    @Nested
    class UpdateBlacklistTest {

        @Test
        @DisplayName("Update blacklist should block user successfully")
        void updateBlacklist_BlockSuccess() {
            BlockRequest request = new BlockRequest("blockerId", "blockedId");

            User mockBlocker = getMockUser("blockerId", "blocker@example.com");
            User mockBlocked = getMockUser("blockedId", "blocked@example.com");

            when(userRepository.findById("blockerId")).thenReturn(Optional.of(mockBlocker));
            when(userRepository.findById("blockedId")).thenReturn(Optional.of(mockBlocked));

            String result = userService.updateBlacklist(request, true);

            assertEquals("User blocked successfully", result);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Update blacklist should unblock user successfully")
        void updateBlacklist_UnblockSuccess() {
            BlockRequest request = new BlockRequest("blockerId", "blockedId");
            User mockBlocker = getMockUser("blockerId", "blocker@example.com");
            User mockBlocked = getMockUser("blockedId", "blocked@example.com");
            mockBlocker.getBlackList().userIds().add(mockBlocked.getId());

            when(userRepository.findById("blockerId")).thenReturn(Optional.of(mockBlocker));
            when(userRepository.findById("blockedId")).thenReturn(Optional.of(mockBlocked));

            String result = userService.updateBlacklist(request, false);

            assertEquals("User unblocked successfully", result);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Update blacklist should throw UserNotFoundException when blocker does not exist")
        void updateBlacklist_BlockerNotFound() {
            BlockRequest request = new BlockRequest("blockerId", "blockedId");
            when(userRepository.findById("blockerId")).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> userService.updateBlacklist(request, true));
        }

        @Test
        @DisplayName("Update blacklist should throw UserNotFoundException when blocked does not exist")
        void updateBlacklist_BlockedNotFound() {
            BlockRequest request = new BlockRequest("blockerId", "blockedId");
            User mockBlocker = getMockUser();
            when(userRepository.findById("blockerId")).thenReturn(Optional.of(mockBlocker));
            when(userRepository.findById("blockedId")).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> userService.updateBlacklist(request, true));
        }
    }

    @Nested
    class UpdateBanTest {

        @Test
        @DisplayName("Update ban should ban user successfully")
        void updateBan_BanSuccess() {
            String userId = "mockId";
            User mockUser = getMockUser();
            when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

            String result = userService.updateBan(userId, true);

            assertEquals("User banned successfully", result);
            verify(userRepository).save(mockUser);
        }

        @Test
        @DisplayName("Update ban should unban user successfully")
        void updateBan_UnbanSuccess() {
            String userId = "mockId";
            User mockUser = getMockUser();
            when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

            String result = userService.updateBan(userId, false);

            assertEquals("User unbanned successfully", result);
            verify(userRepository).save(mockUser);
        }

        @Test
        @DisplayName("Update ban should throw UserNotFoundException when user does not exist")
        void updateBan_UserNotFound() {
            String userId = "mockId";
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> userService.updateBan(userId, true));
        }
    }

    @Nested
    class GetUserSubscriptionsTest {

        @Test
        @DisplayName("Get user subscriptions should return user subscriptions")
        void getUserSubscriptions_Success() {
            String userId = "mockId";
            int page = 0, size = 10;
            when(userRepository.findSubscriptionsIdsByUserId(userId, PageRequest.of(page, size)))
                    .thenReturn(Page.empty());

            List<UserResponse> result = userService.getUserSubscriptions(userId, page, size);

            assertNotNull(result);
            verify(userRepository).findSubscriptionsIdsByUserId(userId, PageRequest.of(page, size));
        }
    }

    @Nested
    class GetUserSubscribersTest {

        @Test
        @DisplayName("Get user subscribers should return user subscribers")
        void getUserSubscribers_Success() {
            String userId = "mockId";
            int page = 0, size = 10;
            when(userRepository.findSubscribersIdsByUserId(userId, PageRequest.of(page, size)))
                    .thenReturn(Page.empty());

            List<UserResponse> result = userService.getUserSubscribers(userId, page, size);

            assertNotNull(result);
            verify(userRepository).findSubscribersIdsByUserId(userId, PageRequest.of(page, size));
        }
    }

    @Nested
    class GetUserFriendshipRecommendationsTest {

        @Test
        @DisplayName("Should return random users when user has no subscriptions")
        void noSubscriptions_ReturnsRandomUsers() {
            String userId = "mockId";
            User mockUser = getMockUser(userId, "mock@mail.com");
            mockUser.setSubscriptionsCount(0);

            when(userRepository.findByEmail(isNull()))
                    .thenReturn(Optional.of(mockUser));

            when(userRepository.findAll(any(PageRequest.class)))
                    .thenReturn(new PageImpl<>(getRandomUserList(SUBSCRIPTIONS_PAGE)));

            List<UserResponse> result = userService.getUserFriendshipRecommendations();

            assertNotNull(result);
            assertEquals(SUBSCRIPTIONS_PAGE, result.size());
            verify(userRepository).findAll(any(PageRequest.class));
        }

        @Test
        @DisplayName("Should fill result with random users if recommendations are less than SUBSCRIPTIONS_PAGE")
        void subscriptionsLessThanPage_ReturnsSubscriptions() {
            String userId = "mockId";
            User mockUser = getMockUser(userId, "mock@mail.com");
            int count = 5;
            mockUser.setSubscriptionsCount(count);

            when(userRepository.findByEmail(isNull()))
                    .thenReturn(Optional.of(mockUser));

            when(userRepository.findAll(any(PageRequest.class)))
                    .thenReturn(new PageImpl<>(getRandomUserList(count)));

            when(userRepository.findAllByIdIn(ArgumentMatchers.anyList()))
                    .thenReturn(getRandomUserList(count));

            when(userRepository.findSubscriptionsIdsByUserId(eq(userId), any(PageRequest.class)))
                    .thenReturn(new PageImpl<>(getRandomUserIdList(count)));

            List<UserResponse> result = userService.getUserFriendshipRecommendations();

            assertNotNull(result);
            assertEquals(SUBSCRIPTIONS_PAGE, result.size());
            verify(userRepository).findSubscriptionsIdsByUserId(eq(userId), any(PageRequest.class));
        }

        @Test
        @DisplayName("Should return shuffled subscriptions when user has more subscriptions than SUBSCRIPTIONS_PAGE")
        void subscriptionsMoreThanPage_ReturnsShuffledSubscriptions() {
            String userId = "mockId";
            User mockUser = getMockUser(userId, "mock@mail.com");
            int count = 100;
            mockUser.setSubscriptionsCount(count);

            when(userRepository.findByEmail(isNull()))
                    .thenReturn(Optional.of(mockUser));

            when(userRepository.findAllByIdIn(ArgumentMatchers.anyList()))
                    .thenReturn(getRandomUserList(SUBSCRIPTIONS_PAGE));

            when(userRepository.findSubscriptionsIdsByUserId(eq(userId), any(PageRequest.class)))
                    .thenReturn(new PageImpl<>(getRandomUserIdList(count)));

            List<UserResponse> result = userService.getUserFriendshipRecommendations();

            assertNotNull(result);
            assertEquals(SUBSCRIPTIONS_PAGE, result.size());
            verify(userRepository).findSubscriptionsIdsByUserId(eq(userId), any(PageRequest.class));
        }
    }


    private User getMockUser(String id, String email) {
        return User.builder()
                .id(id)
                .nickname("nickname")
                .email(email)
                .password("password")
                .description("description")
                .blackList(new BlackList(new HashSet<>()))
                .subscriptions(new HashSet<>())
                .subscribers(new HashSet<>())
                .subscriberCount(0)
                .subscriptionsCount(0)
                .build();
    }

    private User getMockUser() {
        return User.builder()
                .id("mockId")
                .email("mock@mail.com")
                .nickname("Mock User")
                .build();
    }

    private List<String> getRandomUserIdList(int count) {
        List<String> res = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String uuid = UUID.randomUUID().toString();
            res.add(encodeString(uuid));
        }
        return res;
    }

    private List<User> getRandomUserList(int count) {
        List<User> res = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String uuid = UUID.randomUUID().toString();
            res.add(User
                    .builder()
                    .id(encodeString(uuid))
                    .build());
        }
        return res;
    }


    public String encodeString(String text) {
        byte[] uuidBytes = text.getBytes(StandardCharsets.UTF_8);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(uuidBytes)
                .substring(0, ID_LENGTH);
    }
}
