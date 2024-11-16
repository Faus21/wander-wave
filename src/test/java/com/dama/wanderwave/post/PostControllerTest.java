package com.dama.wanderwave.post;

import com.dama.wanderwave.handler.GlobalExceptionHandler;
import com.dama.wanderwave.handler.post.CategoryTypeNotFoundException;
import com.dama.wanderwave.handler.post.PostNotFoundException;
import com.dama.wanderwave.handler.user.UnauthorizedActionException;
import com.dama.wanderwave.handler.user.UserNotFoundException;
import com.dama.wanderwave.handler.user.like.IsLikedException;
import com.dama.wanderwave.handler.user.like.LikeNotFoundException;
import com.dama.wanderwave.handler.user.save.IsSavedException;
import com.dama.wanderwave.handler.user.save.SavedPostNotFound;
import com.dama.wanderwave.post.request.CreatePostRequest;
import com.dama.wanderwave.post.response.PostResponse;
import com.dama.wanderwave.user.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Getter
@RequiredArgsConstructor
enum ApiUrls {
    CREATE_POST("/api/posts/create"),
    GET_POST_LIKES("/api/posts/{postId}/like"),
    LIKE_POST("/api/posts/{postId}/like"),
    UNLIKE_POST("/api/posts/{postId}/like"),
    SAVE_POST("/api/posts/{postId}/save"),
    UNSAVE_POST("/api/posts/{postId}/save"),
    GET_PERSONAL_FLOW("/api/posts/user/personalFlow"),
    GET_RECOMMENDATIONS_FLOW("/api/posts/user/recommendationsFlow"),
    GET_USER_LIKES("/api/posts/user/likes"),
    GET_USER_SAVED("/api/posts/user/saved"),
    GET_POSTS_BY_CATEGORY("/api/posts/"),
    DELETE_POST("/api/posts/{postId}"),
    GET_USER_POSTS("/api/posts/user/{nickname}");

    private final String url;
}

@ExtendWith(MockitoExtension.class)
public class PostControllerTest {

    @InjectMocks
    private PostController postController;
    @Mock
    private PostService postService;

    public record ErrorResponse(int errorCode, String message) { }

    public record ResponseRecord(int code, String message) { }

    private static final String CONTENT_TYPE = MediaType.APPLICATION_JSON_VALUE;
    private static final MediaType ACCEPT_TYPE = MediaType.APPLICATION_JSON;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(postController).setControllerAdvice(new GlobalExceptionHandler()).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Nested
    class GetUserPostsTest {
        @Test
        @DisplayName("Get user reports should return OK (200)")
        void getUserReports_Success() throws Exception {
            when(postService.getUserPosts(any(String.class))).thenReturn(
                    getUserPosts()
            );

            mockMvc.perform(get(ApiUrls.GET_USER_POSTS.getUrl(), getMockUser().getNickname())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.message").isArray())
                    .andExpect(jsonPath("$.message[0].id").value("mockPost1"));

            verify(postService).getUserPosts(any(String.class));
        }

        @Test
        @DisplayName("Get user reports should return exception - forbidden (403)")
        void getUserReports_Forbidden() throws Exception {
            PostControllerTest.ErrorResponse response = new PostControllerTest.ErrorResponse(HttpStatus.FORBIDDEN.value(), "Forbidden: Authenticated user is not authorized to perform this action");

            when(postService.getUserPosts(any(String.class))).thenThrow(new UnauthorizedActionException(response.message));

            mockMvc.perform(get(ApiUrls.GET_USER_POSTS.getUrl(), "mockNickname")
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(response.message));

            verify(postService).getUserPosts(any(String.class));
        }

        @Test
        @DisplayName("Get user reports should return exception - not found (404)")
        void getUserReports_CategoryNotFound() throws Exception {
            PostControllerTest.ErrorResponse response = new PostControllerTest.ErrorResponse(HttpStatus.NOT_FOUND.value(), "Category not found");

            when(postService.getUserPosts(any(String.class))).thenThrow(new CategoryTypeNotFoundException(response.message));

            mockMvc.perform(get(ApiUrls.GET_USER_POSTS.getUrl(), "mockNickname")
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value(response.errorCode))
                    .andExpect(jsonPath("$.message").value(response.message));

            verify(postService).getUserPosts(any(String.class));
        }

        @Test
        @DisplayName("Get user reports should return exception - internal server error (500)")
        void getUserReports_InternalServerError() throws Exception {
            PostControllerTest.ErrorResponse response = new PostControllerTest.ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error");

            when(postService.getUserPosts(any(String.class))).thenThrow(new RuntimeException(response.message));

            mockMvc.perform(get(ApiUrls.GET_USER_POSTS.getUrl(), "mockNickname")
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.errorCode").value(response.errorCode));

            verify(postService).getUserPosts(any(String.class));
        }
    }

    @Nested
    class CreatePostTest {
        @Test
        @DisplayName("Post creation should return OK (200)")
        void createPost_Success() throws Exception {
            String mockRequestJson = objectMapper.writeValueAsString(getMockPostSuccess());
            PostControllerTest.ResponseRecord response = new PostControllerTest.ResponseRecord(HttpStatus.OK.value(), "Post is created successfully");

            when(postService.createPost(any(CreatePostRequest.class))).thenReturn(response.message);

            mockMvc.perform(post(ApiUrls.CREATE_POST.getUrl())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE)
                            .content(mockRequestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.message").value(response.message));

            verify(postService).createPost(any(CreatePostRequest.class));
        }

        @Test
        @DisplayName("Post creation should return bad request (400)")
        void createPost_BadRequest() throws Exception {
            String mockRequestJson = objectMapper.writeValueAsString(getMockPostBadRequest());
            PostControllerTest.ResponseRecord response = new PostControllerTest.ResponseRecord(HttpStatus.BAD_REQUEST.value(), "Validation failed: userNickname: User is mandatory");

            mockMvc.perform(post(ApiUrls.CREATE_POST.getUrl())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE)
                            .content(mockRequestJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(response.message));
        }

        @Test
        @DisplayName("Post creation should return not found (404)")
        void createPost_NotFound() throws Exception {
            String mockRequestJson = objectMapper.writeValueAsString(getMockPostSuccess());
            PostControllerTest.ResponseRecord response = new PostControllerTest.ResponseRecord(HttpStatus.NOT_FOUND.value(), "Category not found");

            when(postService.createPost(any(CreatePostRequest.class))).thenThrow(new CategoryTypeNotFoundException(response.message));

            mockMvc.perform(post(ApiUrls.CREATE_POST.getUrl())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE)
                            .content(mockRequestJson))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(response.message));

            verify(postService).createPost(any(CreatePostRequest.class));
        }

        @Test
        @DisplayName("Post creation should return not found (404)")
        void createPost_InternalServerError() throws Exception {
            String mockRequestJson = objectMapper.writeValueAsString(getMockPostSuccess());
            PostControllerTest.ResponseRecord response = new PostControllerTest.ResponseRecord(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error");

            when(postService.createPost(any(CreatePostRequest.class))).thenThrow(new RuntimeException(response.message));

            mockMvc.perform(post(ApiUrls.CREATE_POST.getUrl())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE)
                            .content(mockRequestJson))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value(response.message));

            verify(postService).createPost(any(CreatePostRequest.class));
        }
    }

    @Nested
    class GetPostLikes {
        @Test
        @DisplayName("Get post likes should return success (200) when likes count is retrieved successfully")
        void getPostLikes_Success() throws Exception {
            String postId = "123";
            Integer likesCount = 10;
            when(postService.getPostLikesCount(postId)).thenReturn(likesCount);

            mockMvc.perform(get(ApiUrls.GET_POST_LIKES.getUrl(), postId)
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.message").value(likesCount));

            verify(postService).getPostLikesCount(postId);
        }

        @Test
        @DisplayName("Get post likes should return not found (404) when the post does not exist")
        void getPostLikes_PostNotFound() throws Exception {
            String postId = "invalidId";
            when(postService.getPostLikesCount(postId)).thenThrow(new PostNotFoundException("Post not found"));

            mockMvc.perform(get(ApiUrls.GET_POST_LIKES.getUrl(), postId)
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isNotFound());

            verify(postService).getPostLikesCount(postId);
        }

        @Test
        @DisplayName("Get post likes should return internal server error (500) on unexpected errors")
        void getPostLikes_InternalServerError() throws Exception {
            String postId = "123";
            when(postService.getPostLikesCount(postId)).thenThrow(new RuntimeException("Internal error"));

            mockMvc.perform(get(ApiUrls.GET_POST_LIKES.getUrl(), postId)
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isInternalServerError());

            verify(postService).getPostLikesCount(postId);
        }
    }

    @Nested
    class LikePost {

        @Test
        @DisplayName("Like post should return success (200) when the post is liked successfully")
        void likePost_Success() throws Exception {
            String postId = "123";
            String successMessage = "Like is created successfully!";
            when(postService.likePost(postId)).thenReturn(successMessage);

            mockMvc.perform(post(ApiUrls.LIKE_POST.getUrl(), postId)
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.message").value(successMessage));

            verify(postService).likePost(postId);
        }

        @Test
        @DisplayName("Like post should return bad request (400) when the post is already liked")
        void likePost_PostAlreadyLiked() throws Exception {
            String postId = "123";
            when(postService.likePost(postId)).thenThrow(new IsLikedException("Post already liked!"));

            mockMvc.perform(post(ApiUrls.LIKE_POST.getUrl(), postId)
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isBadRequest());

            verify(postService).likePost(postId);
        }

        @Test
        @DisplayName("Like post should return not found (404) when the post does not exist")
        void likePost_PostNotFound() throws Exception {
            String postId = "invalidId";
            when(postService.likePost(postId)).thenThrow(new PostNotFoundException("Post not found"));

            mockMvc.perform(post(ApiUrls.LIKE_POST.getUrl(), postId)
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isNotFound());

            verify(postService).likePost(postId);
        }

        @Test
        @DisplayName("Like post should return internal server error (500) on unexpected errors")
        void likePost_InternalServerError() throws Exception {
            String postId = "123";
            when(postService.likePost(postId)).thenThrow(new RuntimeException("Internal server error"));

            mockMvc.perform(post(ApiUrls.LIKE_POST.getUrl(), postId)
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isInternalServerError());

            verify(postService).likePost(postId);
        }
    }

    @Nested
    class UnlikePost {

        @Test
        @DisplayName("Unlike post should return success (200) when the like is removed successfully")
        void unlikePost_Success() throws Exception {
            String postId = "123";
            String successMessage = "Like is deleted successfully";
            when(postService.unlikePost(postId)).thenReturn(successMessage);

            mockMvc.perform(delete(ApiUrls.UNLIKE_POST.getUrl(), postId)
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.message").value(successMessage));

            verify(postService).unlikePost(postId);
        }

        @Test
        @DisplayName("Unlike post should return not found (404) when the post does not exist")
        void unlikePost_PostNotFound() throws Exception {
            String postId = "invalidId";
            when(postService.unlikePost(postId)).thenThrow(new PostNotFoundException("Post not found"));

            mockMvc.perform(delete(ApiUrls.UNLIKE_POST.getUrl(), postId)
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isNotFound());

            verify(postService).unlikePost(postId);
        }

        @Test
        @DisplayName("Unlike post should return not found (404) when the like does not exist")
        void unlikePost_LikeNotFound() throws Exception {
            String postId = "123";
            when(postService.unlikePost(postId)).thenThrow(new LikeNotFoundException("Like not found"));

            mockMvc.perform(delete(ApiUrls.UNLIKE_POST.getUrl(), postId)
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isNotFound());

            verify(postService).unlikePost(postId);
        }

        @Test
        @DisplayName("Unlike post should return internal server error (500) on unexpected errors")
        void unlikePost_InternalServerError() throws Exception {
            String postId = "123";
            when(postService.unlikePost(postId)).thenThrow(new RuntimeException("Internal server error"));

            mockMvc.perform(delete(ApiUrls.UNLIKE_POST.getUrl(), postId)
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isInternalServerError());

            verify(postService).unlikePost(postId);
        }
    }


    @Nested
    class SavePost {

        @Test
        @DisplayName("Save post should return success (200) when the post is saved successfully")
        void savePost_Success() throws Exception {
            String postId = "123";
            String successMessage = "Saved successfully";
            when(postService.savePost(postId)).thenReturn(successMessage);

            mockMvc.perform(post(ApiUrls.SAVE_POST.getUrl(), postId)
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(successMessage));

            verify(postService).savePost(postId);
        }

        @Test
        @DisplayName("Save post should return bad request (400) when the post is already saved")
        void savePost_PostAlreadySaved() throws Exception {
            String postId = "123";
            when(postService.savePost(postId)).thenThrow(new IsSavedException("Post is saved already!"));

            mockMvc.perform(post(ApiUrls.SAVE_POST.getUrl(), postId)
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isBadRequest());

            verify(postService).savePost(postId);
        }

        @Test
        @DisplayName("Save post should return not found (404) when the post does not exist")
        void savePost_PostNotFound() throws Exception {
            String postId = "invalidId";
            when(postService.savePost(postId)).thenThrow(new PostNotFoundException("Post not found"));

            mockMvc.perform(post(ApiUrls.SAVE_POST.getUrl(), postId)
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isNotFound());

            verify(postService).savePost(postId);
        }

        @Test
        @DisplayName("Save post should return internal server error (500) on unexpected errors")
        void savePost_InternalServerError() throws Exception {
            String postId = "123";
            when(postService.savePost(postId)).thenThrow(new RuntimeException("Internal server error"));

            mockMvc.perform(post(ApiUrls.SAVE_POST.getUrl(), postId)
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isInternalServerError());

            verify(postService).savePost(postId);
        }
    }

    @Nested
    class UnsavePost {

        @Test
        @DisplayName("Unsave post should return success (200) when the post is unsaved successfully")
        void unsavePost_Success() throws Exception {
            String postId = "123";
            String successMessage = "Post unsaved successfully";
            when(postService.unsavePost(postId)).thenReturn(successMessage);

            mockMvc.perform(delete(ApiUrls.UNSAVE_POST.getUrl(), postId)
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.message").value(successMessage));

            verify(postService).unsavePost(postId);
        }

        @Test
        @DisplayName("Unsave post should return not found (404) when the post does not exist")
        void unsavePost_PostNotFound() throws Exception {
            String postId = "invalidId";
            when(postService.unsavePost(postId)).thenThrow(new PostNotFoundException("Post not found"));

            mockMvc.perform(delete(ApiUrls.UNSAVE_POST.getUrl(), postId)
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isNotFound());

            verify(postService).unsavePost(postId);
        }

        @Test
        @DisplayName("Unsave post should return not found (404) when the saved post is not found")
        void unsavePost_SavedPostNotFound() throws Exception {
            String postId = "123";
            when(postService.unsavePost(postId)).thenThrow(new SavedPostNotFound("Saved post not found"));

            mockMvc.perform(delete(ApiUrls.UNSAVE_POST.getUrl(), postId)
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isNotFound());

            verify(postService).unsavePost(postId);
        }

        @Test
        @DisplayName("Unsave post should return internal server error (500) on unexpected errors")
        void unsavePost_InternalServerError() throws Exception {
            String postId = "123";
            when(postService.unsavePost(postId)).thenThrow(new RuntimeException("Internal server error"));

            mockMvc.perform(delete(ApiUrls.UNSAVE_POST.getUrl(), postId)
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isInternalServerError());

            verify(postService).unsavePost(postId);
        }
    }


    @Nested
    class GetPersonalFlow {

        @Test
        @DisplayName("Get personal flow should return success (200) when the flow is fetched successfully")
        void getPersonalFlow_Success() throws Exception {
            List<PostResponse> mockFlow = getUserPosts();
            when(postService.personalFlow()).thenReturn(mockFlow);

            mockMvc.perform(get(ApiUrls.GET_PERSONAL_FLOW.getUrl())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.message").isArray());

            verify(postService).personalFlow();
        }

        @Test
        @DisplayName("Get personal flow should return not found (404) when user subscription is not found")
        void getPersonalFlow_UserSubscriptionNotFound() throws Exception {
            when(postService.personalFlow()).thenThrow(new UserNotFoundException("User subscription not found"));

            mockMvc.perform(get(ApiUrls.GET_PERSONAL_FLOW.getUrl())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isNotFound());

            verify(postService).personalFlow();
        }

        @Test
        @DisplayName("Get personal flow should return internal server error (500) on unexpected errors")
        void getPersonalFlow_InternalServerError() throws Exception {
            when(postService.personalFlow()).thenThrow(new RuntimeException("Internal server error"));

            mockMvc.perform(get(ApiUrls.GET_PERSONAL_FLOW.getUrl())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isInternalServerError());

            verify(postService).personalFlow();
        }
    }


    @Nested
    class GetRecommendationsFlow {

        @Test
        @DisplayName("Get recommendations flow should return success (200) when the flow is fetched successfully")
        void getRecommendationsFlow_Success() throws Exception {
            List<PostResponse> mockFlow = getUserPosts();
            when(postService.recommendationFlow()).thenReturn(mockFlow);

            mockMvc.perform(get(ApiUrls.GET_RECOMMENDATIONS_FLOW.getUrl())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.message").isArray());

            verify(postService).recommendationFlow();
        }

        @Test
        @DisplayName("Get recommendations flow should return internal server error (500) on unexpected errors")
        void getRecommendationsFlow_InternalServerError() throws Exception {
            when(postService.recommendationFlow()).thenThrow(new RuntimeException("Internal server error"));

            mockMvc.perform(get(ApiUrls.GET_RECOMMENDATIONS_FLOW.getUrl())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isInternalServerError());

            verify(postService).recommendationFlow();
        }
    }

    @Nested
    class GetUserLikes {

        @Test
        @DisplayName("Get user likes should return success (200) when likes are fetched successfully")
        void getUserLikes_Success() throws Exception {
            List<PostResponse> mockLikes = getUserPosts();
            when(postService.getLikedPostsResponse()).thenReturn(mockLikes);

            mockMvc.perform(get(ApiUrls.GET_USER_LIKES.getUrl())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.message").isArray());

            verify(postService).getLikedPostsResponse();
        }

        @Test
        @DisplayName("Get user likes should return internal server error (500) on unexpected errors")
        void getUserLikes_InternalServerError() throws Exception {
            when(postService.getLikedPostsResponse()).thenThrow(new RuntimeException("Internal server error"));

            mockMvc.perform(get(ApiUrls.GET_USER_LIKES.getUrl())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isInternalServerError());

            verify(postService).getLikedPostsResponse();
        }
    }


    @Nested
    class GetUserSaved {

        @Test
        @DisplayName("Get user saved posts should return success (200) when saved posts are fetched successfully")
        void getUserSaved_Success() throws Exception {
            List<PostResponse> mockSavedPosts = getUserPosts();
            when(postService.getSavedPostsResponse()).thenReturn(mockSavedPosts);

            mockMvc.perform(get(ApiUrls.GET_USER_SAVED.getUrl())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.message").isArray());

            verify(postService).getSavedPostsResponse();
        }

        @Test
        @DisplayName("Get user saved posts should return internal server error (500) on unexpected errors")
        void getUserSaved_InternalServerError() throws Exception {
            when(postService.getSavedPostsResponse()).thenThrow(new RuntimeException("Internal server error"));

            mockMvc.perform(get(ApiUrls.GET_USER_SAVED.getUrl())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isInternalServerError());

            verify(postService).getSavedPostsResponse();
        }
    }


    @Nested
    class GetPostsByCategory {

        @Test
        @DisplayName("Get posts by category should return success (200) when posts are retrieved successfully")
        void getPostsByCategory_Success() throws Exception {
            String category = "Technology";
            List<PostResponse> mockPosts = getUserPosts();
            when(postService.getPostsByCategory(category)).thenReturn(mockPosts);

            mockMvc.perform(get(ApiUrls.GET_POSTS_BY_CATEGORY.getUrl())
                            .param("category", category)
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.message").isArray());

            verify(postService).getPostsByCategory(category);
        }

        @Test
        @DisplayName("Get posts by category should return internal server error (500) on unexpected errors")
        void getPostsByCategory_InternalServerError() throws Exception {
            String category = "Technology";
            when(postService.getPostsByCategory(category)).thenThrow(new RuntimeException("Internal server error"));

            mockMvc.perform(get(ApiUrls.GET_POSTS_BY_CATEGORY.getUrl())
                            .param("category", category)
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isInternalServerError());

            verify(postService).getPostsByCategory(category);
        }
    }


    @Nested
    class DeletePost {

        @Test
        @DisplayName("Delete post should return success (200) when the post is deleted successfully")
        void deletePost_Success() throws Exception {
            String postId = "123";
            String successMessage = "Post deleted successfully";
            when(postService.deletePost(postId)).thenReturn(successMessage);

            mockMvc.perform(delete(ApiUrls.DELETE_POST.getUrl(), postId)
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.message").value(successMessage));

            verify(postService).deletePost(postId);
        }

        @Test
        @DisplayName("Delete post should return not found (404) when the post does not exist")
        void deletePost_PostNotFound() throws Exception {
            String postId = "invalidId";
            when(postService.deletePost(postId)).thenThrow(new PostNotFoundException("Post not found"));

            mockMvc.perform(delete(ApiUrls.DELETE_POST.getUrl(), postId)
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isNotFound());

            verify(postService).deletePost(postId);
        }

        @Test
        @DisplayName("Delete post should return internal server error (500) on unexpected errors")
        void deletePost_InternalServerError() throws Exception {
            String postId = "123";
            when(postService.deletePost(postId)).thenThrow(new RuntimeException("Internal server error"));

            mockMvc.perform(delete(ApiUrls.DELETE_POST.getUrl(), postId)
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isInternalServerError());

            verify(postService).deletePost(postId);
        }
    }


    private User getMockUser() {
        return User.builder()
                .id("mockId")
                .email("mock@mail.com")
                .nickname("mockNickname")
                .build();
    }

    private CreatePostRequest getMockPostSuccess() {
        return CreatePostRequest.builder()
                .title("mockPost")
                .categoryName("mockCategory")
                .userNickname("mockNickname")
                .build();
    }

    private CreatePostRequest getMockPostBadRequest() {
        return CreatePostRequest.builder()
                .title("mockPost")
                .categoryName("mockCategory")
                .build();
    }

    private List<PostResponse> getUserPosts(){
        User user = getMockUser();

        var post1 = new PostResponse();
        post1.setTitle("title1");
        post1.setId("mockPost1");
        post1.setNickname(user.getNickname());

        var post2 = new PostResponse();
        post2.setTitle("title2");
        post2.setId("mockPost2");
        post2.setNickname(user.getNickname());

        return List.of(
                post1, post2
        );
    }

}
