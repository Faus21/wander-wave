package com.dama.wanderwave.user;

import com.dama.wanderwave.azure.AzureService;
import com.dama.wanderwave.handler.GlobalExceptionHandler;
import com.dama.wanderwave.handler.azure.FileTypeException;
import com.dama.wanderwave.handler.user.UserNotFoundException;
import com.dama.wanderwave.user.request.BlockRequest;
import com.dama.wanderwave.user.request.SubscribeRequest;
import com.dama.wanderwave.user.response.UserResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Getter
@RequiredArgsConstructor
enum ApiUrl {
    PROFILE("/api/users/profile/id/{id}"),
    SUBSCRIBE("/api/users/subscribe"),
    UNSUBSCRIBE("/api/users/unsubscribe"),
    BAN("/api/users/ban/{id}"),
    UNBAN("/api/users/unban/{id}"),
    BLOCK("/api/users/block"),
    UNBLOCK("/api/users/unblock"),
    SUBSCRIPTIONS("/api/users/subscriptions/{userId}"),
    SUBSCRIBERS("/api/users/subscribers/{userId}"),
    UPLOAD_AVATAR("/api/users/upload-avatar"),
    RECOMMENDATIONS("/api/users/recommendations");

    private final String url;
}

@ExtendWith(MockitoExtension.class)
class UserControllerTest {


    @Mock
    private UserService userService;

    @Mock
    private AzureService azureService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private static final String CONTENT_TYPE = MediaType.APPLICATION_JSON_VALUE;
    private static final MediaType ACCEPT_TYPE = MediaType.APPLICATION_JSON;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).setControllerAdvice(new GlobalExceptionHandler()).build();
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("Tests for retrieving user profile")
    class GetUserProfileTests {

        @Test
        @DisplayName("Should return user profile successfully")
        void testGetUserProfile_Success() throws Exception {
            String userId = "123";
            UserResponse userResponse = UserResponse.builder()
                    .id("123")
                    .nickname("JohnDoe")
                    .build();

            when(userService.getUserById(userId)).thenReturn(userResponse);

            mockMvc.perform(get(ApiUrl.PROFILE.getUrl(), userId)
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message.id").value(userResponse.getId()))
                    .andExpect(jsonPath("$.message.nickname").value(userResponse.getNickname()));

            verify(userService, times(1)).getUserById(userId);
        }

        @Test
        @DisplayName("Should return 404 if user is not found")
        void testGetUserProfile_NotFound() throws Exception {
            String userId = "123";
            when(userService.getUserById(userId)).thenThrow(new UserNotFoundException("User not found"));

            mockMvc.perform(get(ApiUrl.PROFILE.getUrl(), userId)
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("User not found"));

            verify(userService, times(1)).getUserById(userId);
        }

        @Test
        @DisplayName("Should return 500 if an internal server error occurs")
        void testGetUserProfile_InternalServerError() throws Exception {
            String userId = "123";

            when(userService.getUserById(userId)).thenThrow(new RuntimeException("Internal server error"));

            mockMvc.perform(get(ApiUrl.PROFILE.getUrl(), userId)
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("Internal server error"));

            verify(userService, times(1)).getUserById(userId);
        }
    }

    @Nested
    @DisplayName("Tests for banning and unbanning users")
    class UserBanTests {

        @Test
        @DisplayName("Should successfully ban a user")
        void testBanUser_Success() throws Exception {
            String userId = "123";
            String responseMessage = "User banned successfully";

            when(userService.updateBan(userId, true)).thenReturn(responseMessage);

            mockMvc.perform(post(ApiUrl.BAN.getUrl(), userId)
                            .accept(ACCEPT_TYPE)
                            .contentType(CONTENT_TYPE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(responseMessage));

            verify(userService, times(1)).updateBan(userId, true);
        }

        @Test
        @DisplayName("Should successfully unban a user")
        void testUnbanUser_Success() throws Exception {
            String userId = "123";
            String responseMessage = "User unbanned successfully";

            when(userService.updateBan(userId, false)).thenReturn(responseMessage);

            mockMvc.perform(post(ApiUrl.UNBAN.getUrl(), userId)
                            .accept(ACCEPT_TYPE)
                            .contentType(CONTENT_TYPE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(responseMessage));

            verify(userService, times(1)).updateBan(userId, false);
        }

        @Test
        @DisplayName("Should return 404 if user not found for ban")
        void testBanUser_UserNotFound() throws Exception {
            String userId = "123";
            when(userService.updateBan(userId, true)).thenThrow(new UserNotFoundException("User not found"));

            mockMvc.perform(post(ApiUrl.BAN.getUrl(), userId)
                            .accept(ACCEPT_TYPE)
                            .contentType(CONTENT_TYPE))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("User not found"));

            verify(userService, times(1)).updateBan(userId, true);
        }

        @Test
        @DisplayName("Should return 404 if user not found for unban")
        void testUnbanUser_UserNotFound() throws Exception {
            String userId = "123";
            when(userService.updateBan(userId, false)).thenThrow(new UserNotFoundException("User not found"));

            mockMvc.perform(post(ApiUrl.UNBAN.getUrl(), userId)
                            .accept(ACCEPT_TYPE)
                            .contentType(CONTENT_TYPE))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("User not found"));

            verify(userService, times(1)).updateBan(userId, false);
        }

        @Test
        @DisplayName("Should return 500 for internal server error during ban")
        void testBanUser_InternalServerError() throws Exception {
            String userId = "123";
            when(userService.updateBan(userId, true)).thenThrow(new RuntimeException("Internal server error"));

            mockMvc.perform(post(ApiUrl.BAN.getUrl(), userId)
                            .accept(ACCEPT_TYPE)
                            .contentType(CONTENT_TYPE))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("Internal server error"));

            verify(userService, times(1)).updateBan(userId, true);
        }

        @Test
        @DisplayName("Should return 500 for internal server error during unban")
        void testUnbanUser_InternalServerError() throws Exception {
            String userId = "123";
            when(userService.updateBan(userId, false)).thenThrow(new RuntimeException("Internal server error"));

            mockMvc.perform(post(ApiUrl.UNBAN.getUrl(), userId)
                            .accept(ACCEPT_TYPE)
                            .contentType(CONTENT_TYPE))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("Internal server error"));

            verify(userService, times(1)).updateBan(userId, false);
        }
    }

    @Nested
    @DisplayName("Tests for subscribing and unsubscribing users")
    class SubscribeUnsubscribeTests {

        @Test
        @DisplayName("Should subscribe a user successfully")
        void testSubscribe_Success() throws Exception {
            SubscribeRequest request = new SubscribeRequest("123", "456");

            when(userService.updateSubscription(request, true)).thenReturn("User subscribed successfully");

            mockMvc.perform(post(ApiUrl.SUBSCRIBE.getUrl())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User subscribed successfully"));

            verify(userService, times(1)).updateSubscription(request, true);
        }

        @Test
        @DisplayName("Should return 404 if user is not found while subscribing")
        void testSubscribe_NotFound() throws Exception {
            SubscribeRequest request = new SubscribeRequest("123", "456");

            when(userService.updateSubscription(request, true)).thenThrow(new UserNotFoundException("User not found"));

            mockMvc.perform(post(ApiUrl.SUBSCRIBE.getUrl())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("User not found"));

            verify(userService, times(1)).updateSubscription(request, true);
        }

        @Test
        @DisplayName("Should return 500 if an internal server error occurs during subscribe")
        void testSubscribe_InternalServerError() throws Exception {
            SubscribeRequest request = new SubscribeRequest("123", "456");

            when(userService.updateSubscription(request, true)).thenThrow(new RuntimeException("Internal server error"));

            mockMvc.perform(post(ApiUrl.SUBSCRIBE.getUrl())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("Internal server error"));

            verify(userService, times(1)).updateSubscription(request, true);
        }

        @Test
        @DisplayName("Should return already subscribed message")
        void testSubscribe_AlreadySubscribed() throws Exception {
            SubscribeRequest request = new SubscribeRequest("123", "456");

            when(userService.updateSubscription(request, true)).thenReturn("Already subscribed");

            mockMvc.perform(post(ApiUrl.SUBSCRIBE.getUrl())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Already subscribed"));

            verify(userService, times(1)).updateSubscription(request, true);
        }

        @Test
        @DisplayName("Should unsubscribe a user successfully")
        void testUnsubscribe_Success() throws Exception {
            SubscribeRequest request = new SubscribeRequest("123", "456");

            when(userService.updateSubscription(request, false)).thenReturn("User unsubscribed successfully");

            mockMvc.perform(post(ApiUrl.UNSUBSCRIBE.getUrl())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User unsubscribed successfully"));

            verify(userService, times(1)).updateSubscription(request, false);
        }

        @Test
        @DisplayName("Should return 404 if user is not found while unsubscribing")
        void testUnsubscribe_NotFound() throws Exception {
            SubscribeRequest request = new SubscribeRequest("123", "456");

            when(userService.updateSubscription(request, false)).thenThrow(new UserNotFoundException("User not found"));

            mockMvc.perform(post(ApiUrl.UNSUBSCRIBE.getUrl())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("User not found"));

            verify(userService, times(1)).updateSubscription(request, false);
        }

        @Test
        @DisplayName("Should return 500 if an internal server error occurs during unsubscribe")
        void testUnsubscribe_InternalServerError() throws Exception {
            SubscribeRequest request = new SubscribeRequest("123", "456");

            when(userService.updateSubscription(request, false)).thenThrow(new RuntimeException("Internal server error"));

            mockMvc.perform(post(ApiUrl.UNSUBSCRIBE.getUrl())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("Internal server error"));

            verify(userService, times(1)).updateSubscription(request, false);
        }

        @Test
        @DisplayName("Should return not subscribed message")
        void testUnsubscribe_NotSubscribed() throws Exception {
            SubscribeRequest request = new SubscribeRequest("123", "456");

            when(userService.updateSubscription(request, false)).thenReturn("Not subscribed, cannot unsubscribe");

            mockMvc.perform(post(ApiUrl.UNSUBSCRIBE.getUrl())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Not subscribed, cannot unsubscribe"));

            verify(userService, times(1)).updateSubscription(request, false);
        }
    }

    @Nested
    @DisplayName("Tests for blocking and unblocking users")
    class BlockUnblockUserTests {

        @Test
        @DisplayName("Should block user successfully")
        void testBlockUser_Success() throws Exception {
            BlockRequest blockRequest = new BlockRequest("blockerId", "blockedId");

            when(userService.updateBlacklist(blockRequest, true)).thenReturn("User blocked successfully");

            mockMvc.perform(post(ApiUrl.BLOCK.getUrl())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE)
                            .content(objectMapper.writeValueAsString(blockRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User blocked successfully"));

            verify(userService, times(1)).updateBlacklist(blockRequest, true);
        }

        @Test
        @DisplayName("Should return 404 if user not found when blocking")
        void testBlockUser_NotFound() throws Exception {
            BlockRequest blockRequest = new BlockRequest("blockerId", "blockedId");

            when(userService.updateBlacklist(blockRequest, true)).thenThrow(new UserNotFoundException("User not found"));

            mockMvc.perform(post(ApiUrl.BLOCK.getUrl())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE)
                            .content(objectMapper.writeValueAsString(blockRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("User not found"));

            verify(userService, times(1)).updateBlacklist(blockRequest, true);
        }

        @Test
        @DisplayName("Should return 500 if internal server error occurs during blocking")
        void testBlockUser_InternalServerError() throws Exception {
            BlockRequest blockRequest = new BlockRequest("blockerId", "blockedId");

            when(userService.updateBlacklist(blockRequest, true)).thenThrow(new RuntimeException("Internal server error"));

            mockMvc.perform(post(ApiUrl.BLOCK.getUrl())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE)
                            .content(objectMapper.writeValueAsString(blockRequest)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("Internal server error"));

            verify(userService, times(1)).updateBlacklist(blockRequest, true);
        }

        @Test
        @DisplayName("Should unblock user successfully")
        void testUnblockUser_Success() throws Exception {
            BlockRequest blockRequest = new BlockRequest("blockerId", "blockedId");

            when(userService.updateBlacklist(blockRequest, false)).thenReturn("User unblocked successfully");

            mockMvc.perform(post(ApiUrl.UNBLOCK.getUrl())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE)
                            .content(objectMapper.writeValueAsString(blockRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User unblocked successfully"));

            verify(userService, times(1)).updateBlacklist(blockRequest, false);
        }

        @Test
        @DisplayName("Should return 404 if user not found when unblocking")
        void testUnblockUser_NotFound() throws Exception {
            BlockRequest blockRequest = new BlockRequest("blockerId", "blockedId");

            when(userService.updateBlacklist(blockRequest, false)).thenThrow(new UserNotFoundException("User not found"));

            mockMvc.perform(post(ApiUrl.UNBLOCK.getUrl())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE)
                            .content(objectMapper.writeValueAsString(blockRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("User not found"));

            verify(userService, times(1)).updateBlacklist(blockRequest, false);
        }

        @Test
        @DisplayName("Should return 500 if internal server error occurs during unblocking")
        void testUnblockUser_InternalServerError() throws Exception {
            BlockRequest blockRequest = new BlockRequest("blockerId", "blockedId");

            when(userService.updateBlacklist(blockRequest, false)).thenThrow(new RuntimeException("Internal server error"));

            mockMvc.perform(post(ApiUrl.UNBLOCK.getUrl())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE)
                            .content(objectMapper.writeValueAsString(blockRequest)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("Internal server error"));

            verify(userService, times(1)).updateBlacklist(blockRequest, false);
        }
    }

    @Nested
    @DisplayName("Tests for retrieving user subscriptions and subscribers")
    class GetUserSubscriptionsAndSubscribersTests {

        @Test
        @DisplayName("Should return subscriptions successfully")
        void testGetUserSubscriptions_Success() throws Exception {
            String userId = "123";
            int page = 1;
            int size = 10;
            UserResponse subscription1 = UserResponse.builder()
                    .id("user3")
                    .nickname("JaneDoe1")
                    .build();
            UserResponse subscription2 = UserResponse.builder()
                    .id("user4")
                    .nickname("JaneDoe2")
                    .build();
            List<UserResponse> subscriptions = Arrays.asList(subscription1, subscription2);

            when(userService.getUserSubscriptions(userId, page, size)).thenReturn(subscriptions);

            mockMvc.perform(get(ApiUrl.SUBSCRIPTIONS.getUrl(), userId)
                            .param("page", String.valueOf(page))
                            .param("size", String.valueOf(size))
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message[0].id").value(subscription1.getId()))
                    .andExpect(jsonPath("$.message[0].nickname").value(subscription1.getNickname()))
                    .andExpect(jsonPath("$.message[1].id").value(subscription2.getId()))
                    .andExpect(jsonPath("$.message[1].nickname").value(subscription2.getNickname()));

            verify(userService, times(1)).getUserSubscriptions(userId, page, size);
        }

        @Test
        @DisplayName("Should return 404 if user is not found for subscriptions")
        void testGetUserSubscriptions_NotFound() throws Exception {
            String userId = "123";
            int page = 1;
            int size = 10;
            when(userService.getUserSubscriptions(userId, page, size)).thenThrow(new UserNotFoundException("User not found"));

            mockMvc.perform(get(ApiUrl.SUBSCRIPTIONS.getUrl(), userId)
                            .param("page", String.valueOf(page))
                            .param("size", String.valueOf(size))
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("User not found"));

            verify(userService, times(1)).getUserSubscriptions(userId, page, size);
        }

        @Test
        @DisplayName("Should return subscribers successfully")
        void testGetUserSubscribers_Success() throws Exception {
            String userId = "123";
            int page = 1;
            int size = 10;
            UserResponse subscriber1 = UserResponse.builder()
                    .id("user3")
                    .nickname("JaneDoe1")
                    .build();
            UserResponse subscriber2 = UserResponse.builder()
                    .id("user4")
                    .nickname("JaneDoe2")
                    .build();
            List<UserResponse> subscribers = Arrays.asList(subscriber1, subscriber2);

            when(userService.getUserSubscribers(userId, page, size)).thenReturn(subscribers);

            mockMvc.perform(get(ApiUrl.SUBSCRIBERS.getUrl(), userId)
                            .param("page", String.valueOf(page))
                            .param("size", String.valueOf(size))
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message[0].id").value(subscriber1.getId()))
                    .andExpect(jsonPath("$.message[0].nickname").value(subscriber1.getNickname()))
                    .andExpect(jsonPath("$.message[1].id").value(subscriber2.getId()))
                    .andExpect(jsonPath("$.message[1].nickname").value(subscriber2.getNickname()));

            verify(userService, times(1)).getUserSubscribers(userId, page, size);
        }

        @Test
        @DisplayName("Should return 404 if user is not found for subscribers")
        void testGetUserSubscribers_NotFound() throws Exception {
            String userId = "123";
            int page = 1;
            int size = 10;
            when(userService.getUserSubscribers(userId, page, size)).thenThrow(new UserNotFoundException("User not found"));

            mockMvc.perform(get(ApiUrl.SUBSCRIBERS.getUrl(), userId)
                            .param("page", String.valueOf(page))
                            .param("size", String.valueOf(size))
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("User not found"));

            verify(userService, times(1)).getUserSubscribers(userId, page, size);
        }
    }

    @Nested
    @DisplayName("Tests for uploading user avatar and retrieving friendship recommendations")
    class UploadAvatarTests {

        @Test
        @DisplayName("Should successfully upload avatar")
        void testUploadImage_Success() throws Exception {
            MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", "image content".getBytes());
            String expectedUrl = "https://azure.blobstorage.com/avatars/12345-avatar.jpg";

            when(azureService.uploadAvatar(anyString(), anyString(), any(byte[].class), anyString(), anyLong())).thenReturn(expectedUrl);
            doNothing().when(userService).changeAvatar(expectedUrl);

            mockMvc.perform(multipart(ApiUrl.UPLOAD_AVATAR.getUrl())
                            .file(file)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(expectedUrl));

            verify(azureService, times(1)).uploadAvatar(eq("avatars"), anyString(), any(byte[].class), eq("image/jpeg"), anyLong());
            verify(userService, times(1)).changeAvatar(expectedUrl);
        }

        @Test
        @DisplayName("Should return 400 for invalid file format")
        void testUploadImage_InvalidFileFormat() throws Exception {
            MockMultipartFile file = new MockMultipartFile("file", "avatar.txt", "text/plain", "invalid content".getBytes());

            when(azureService.uploadAvatar(anyString(), anyString(), any(byte[].class), anyString(), anyLong())).thenThrow(new FileTypeException("Invalid file format"));

            mockMvc.perform(multipart(ApiUrl.UPLOAD_AVATAR.getUrl())
                            .file(file)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid file format"));

            verify(azureService, times(1)).uploadAvatar(anyString(), anyString(), any(byte[].class), anyString(), anyLong());
            verify(userService, never()).changeAvatar(anyString());
        }

        @Test
        @DisplayName("Should return 500 for internal server error on file upload")
        void testUploadImage_InternalServerError() throws Exception {
            MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", "image content".getBytes());

            when(azureService.uploadAvatar(anyString(), anyString(), any(byte[].class), anyString(), anyLong())).thenThrow(new RuntimeException("Internal server error"));

            mockMvc.perform(multipart(ApiUrl.UPLOAD_AVATAR.getUrl())
                            .file(file)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("Internal server error"));

            verify(azureService, times(1)).uploadAvatar(eq("avatars"), anyString(), any(byte[].class), eq("image/jpeg"), anyLong());
            verify(userService, never()).changeAvatar(anyString());
        }
    }

    @Nested
    @DisplayName("Tests for retrieving friendship recommendations")
    class RecommendationsTests {
        @Test
        @DisplayName("Should successfully fetch friendship recommendations")
        void testGetUserFriendshipRecommendations_Success() throws Exception {
            UserResponse recommendedUser = UserResponse.builder()
                    .id("user1")
                    .nickname("JaneDoe1")
                    .build();
            UserResponse recommendedUser2 = UserResponse.builder()
                    .id("user2")
                    .nickname("JaneDoe2")
                    .build();
            List<UserResponse> recommendations = Arrays.asList(recommendedUser, recommendedUser2);

            when(userService.getUserFriendshipRecommendations()).thenReturn(recommendations);

            mockMvc.perform(get(ApiUrl.RECOMMENDATIONS.getUrl())
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message[0].id").value(recommendedUser.getId()))
                    .andExpect(jsonPath("$.message[0].nickname").value(recommendedUser.getNickname()))
                    .andExpect(jsonPath("$.message[1].id").value(recommendedUser2.getId()))
                    .andExpect(jsonPath("$.message[1].nickname").value(recommendedUser2.getNickname()));

            verify(userService, times(1)).getUserFriendshipRecommendations();
        }

        @Test
        @DisplayName("Should return 404 if user not found for friendship recommendations")
        void testGetUserFriendshipRecommendations_NotFound() throws Exception {
            when(userService.getUserFriendshipRecommendations()).thenThrow(new UserNotFoundException("User not found"));

            mockMvc.perform(get(ApiUrl.RECOMMENDATIONS.getUrl())
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("User not found"));

            verify(userService, times(1)).getUserFriendshipRecommendations();
        }
    }
}
