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
import com.dama.wanderwave.post.request.PostRequest;
import com.dama.wanderwave.post.response.PostResponse;
import com.dama.wanderwave.post.response.AccountInfoResponse;
import com.dama.wanderwave.post.response.ShortPostResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
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
    GET_USER_POSTS("/api/posts/user/{nickname}"),
    MODIFY_POST("/api/posts/"),
    GET_POST_BY_ID("/api/posts/{postId}");

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
        @DisplayName("Get user posts should return OK (200)")
        void getUserPosts_Success() throws Exception {
            when(postService.getUserPosts(any(Pageable.class), any(String.class))).thenReturn(
                    getShortUserPosts()
            );

            mockMvc.perform(get(ApiUrls.GET_USER_POSTS.getUrl(), getMockUser().getNickname())
                            .param("pageNumber", "0")
                            .param("pageSize", "2")
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.message.content").isArray())
                    .andExpect(jsonPath("$.message.content[0].id").value("mockPost1"));

            verify(postService).getUserPosts(any(Pageable.class), any(String.class));
        }

        @Test
        @DisplayName("Get user reports should return exception - forbidden (403)")
        void getUserPosts_Forbidden() throws Exception {
            PostControllerTest.ErrorResponse response = new PostControllerTest.ErrorResponse(HttpStatus.FORBIDDEN.value(), "Forbidden: Authenticated user is not authorized to perform this action");

            when(postService.getUserPosts(any(Pageable.class), any(String.class))).thenThrow(new UnauthorizedActionException(response.message));

            mockMvc.perform(get(ApiUrls.GET_USER_POSTS.getUrl(), "mockNickname")
                            .param("pageNumber", "0")
                            .param("pageSize", "2")
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(response.message));

            verify(postService).getUserPosts(any(Pageable.class), any(String.class));
        }

        @Test
        @DisplayName("Get user posts should return exception - not found (404)")
        void  getUserPosts_CategoryNotFound() throws Exception {
            PostControllerTest.ErrorResponse response = new PostControllerTest.ErrorResponse(HttpStatus.NOT_FOUND.value(), "Category not found");

            when(postService.getUserPosts(any(Pageable.class), any(String.class))).thenThrow(new CategoryTypeNotFoundException(response.message));

            mockMvc.perform(get(ApiUrls.GET_USER_POSTS.getUrl(), "mockNickname")
                            .param("pageNumber", "0")
                            .param("pageSize", "2")
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value(response.errorCode))
                    .andExpect(jsonPath("$.message").value(response.message));

            verify(postService).getUserPosts(any(Pageable.class), any(String.class));
        }

        @Test
        @DisplayName("Get user posts should return exception - internal server error (500)")
        void  getUserPosts_InternalServerError() throws Exception {
            PostControllerTest.ErrorResponse response = new PostControllerTest.ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error");

            when(postService.getUserPosts(any(Pageable.class), any(String.class))).thenThrow(new RuntimeException(response.message));

            mockMvc.perform(get(ApiUrls.GET_USER_POSTS.getUrl(), "mockNickname")
                            .param("pageNumber", "0")
                            .param("pageSize", "2")
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.errorCode").value(response.errorCode));

            verify(postService).getUserPosts(any(Pageable.class), any(String.class));
        }
    }

    @Nested
    class CreatePostTest {
        @Test
        @DisplayName("Post creation should return OK (200)")
        void createPost_Success() throws Exception {
            PostRequest mockPostRequest = getMockPost();
            PostControllerTest.ResponseRecord response = new PostControllerTest.ResponseRecord(HttpStatus.OK.value(), mockPostRequest.getId());

            when(postService.createPost(any(PostRequest.class))).thenReturn(response.message);

            MockHttpServletRequestBuilder requestBuilder = createPostRequest(mockPostRequest);

            mockMvc.perform(requestBuilder)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.message").value(response.message));

            verify(postService).createPost(any(PostRequest.class));
        }

        @Test
        @DisplayName("Post creation should return not found (404)")
        void createPost_NotFound() throws Exception {
            PostRequest mockPostRequest = getMockPost();
            PostControllerTest.ResponseRecord response = new PostControllerTest.ResponseRecord(HttpStatus.NOT_FOUND.value(), "Category not found");

            when(postService.createPost(any(PostRequest.class))).thenThrow(new CategoryTypeNotFoundException(response.message));

            MockHttpServletRequestBuilder requestBuilder = createPostRequest(mockPostRequest);

            mockMvc.perform(requestBuilder)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(response.message));

            verify(postService).createPost(any(PostRequest.class));
        }

        @Test
        @DisplayName("Post creation should return internal server error (500)")
        void createPost_InternalServerError() throws Exception {
            PostRequest mockPostRequest = getMockPost();
            PostControllerTest.ResponseRecord response = new PostControllerTest.ResponseRecord(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error");

            when(postService.createPost(any(PostRequest.class))).thenThrow(new RuntimeException(response.message));

            MockHttpServletRequestBuilder requestBuilder = createPostRequest(mockPostRequest);

            mockMvc.perform(requestBuilder)
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value(response.message));

            verify(postService).createPost(any(PostRequest.class));
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

            when(postService.personalFlow(any(PageRequest.class))).thenReturn(getShortUserPosts());

            mockMvc.perform(get(ApiUrls.GET_PERSONAL_FLOW.getUrl())
                            .param("pageNumber", "0")
                            .param("pageSize", "2")
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.message.content").isArray());

            verify(postService).personalFlow(any(PageRequest.class));
        }

        @Test
        @DisplayName("Get personal flow should return not found (404) when user subscription is not found")
        void getPersonalFlow_UserSubscriptionNotFound() throws Exception {
            when(postService.personalFlow(any(PageRequest.class))).thenThrow(new UserNotFoundException("User subscription not found"));

            mockMvc.perform(get(ApiUrls.GET_PERSONAL_FLOW.getUrl())
                            .param("pageNumber", "0")
                            .param("pageSize", "2")
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isNotFound());

            verify(postService).personalFlow(any(PageRequest.class));
        }

        @Test
        @DisplayName("Get personal flow should return internal server error (500) on unexpected errors")
        void getPersonalFlow_InternalServerError() throws Exception {
            when(postService.personalFlow(any(PageRequest.class))).thenThrow(new RuntimeException("Internal server error"));

            mockMvc.perform(get(ApiUrls.GET_PERSONAL_FLOW.getUrl())
                            .param("pageNumber", "0")
                            .param("pageSize", "2")
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isInternalServerError());

            verify(postService).personalFlow(any(PageRequest.class));
        }
    }


    @Nested
    class GetRecommendationsFlow {

        @Test
        @DisplayName("Get recommendations flow should return success (200) when the flow is fetched successfully")
        void getRecommendationsFlow_Success() throws Exception {

            when(postService.recommendationFlow(any(Pageable.class))).thenReturn(getShortUserPostsList());

            mockMvc.perform(get(ApiUrls.GET_RECOMMENDATIONS_FLOW.getUrl())
                            .param("pageNumber", "0")
                            .param("pageSize", "2")
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.message").isArray());

            verify(postService).recommendationFlow(any(Pageable.class));
        }

        @Test
        @DisplayName("Get recommendations flow should return internal server error (500) on unexpected errors")
        void getRecommendationsFlow_InternalServerError() throws Exception {
            when(postService.recommendationFlow(any(Pageable.class))).thenThrow(new RuntimeException("Internal server error"));

            mockMvc.perform(get(ApiUrls.GET_RECOMMENDATIONS_FLOW.getUrl())
                            .param("pageNumber", "0")
                            .param("pageSize", "2")
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isInternalServerError());

            verify(postService).recommendationFlow(any(Pageable.class));
        }
    }

    @Nested
    class GetUserLikes {

        @Test
        @DisplayName("Get user likes should return success (200) when likes are fetched successfully")
        void getUserLikes_Success() throws Exception {
            when(postService.getLikedPostsResponse(any(Pageable.class))).thenReturn(getShortUserPosts());

            mockMvc.perform(get(ApiUrls.GET_USER_LIKES.getUrl())
                            .param("pageNumber", "0")
                            .param("pageSize", "2")
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.message.content").isArray());

            verify(postService).getLikedPostsResponse(any(Pageable.class));
        }

        @Test
        @DisplayName("Get user likes should return internal server error (500) on unexpected errors")
        void getUserLikes_InternalServerError() throws Exception {
            when(postService.getLikedPostsResponse(any(Pageable.class))).thenThrow(new RuntimeException("Internal server error"));

            mockMvc.perform(get(ApiUrls.GET_USER_LIKES.getUrl())
                            .param("pageNumber", "0")
                            .param("pageSize", "2")
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isInternalServerError());

            verify(postService).getLikedPostsResponse(any(Pageable.class));
        }
    }


    @Nested
    class GetUserSaved {

        @Test
        @DisplayName("Get user saved posts should return success (200) when saved posts are fetched successfully")
        void getUserSaved_Success() throws Exception {

            when(postService.getSavedPostsResponse(any(Pageable.class))).thenReturn(getShortUserPosts());

            mockMvc.perform(get(ApiUrls.GET_USER_SAVED.getUrl())
                            .param("pageNumber", "0")
                            .param("pageSize", "2")
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.message.content").isArray());

            verify(postService).getSavedPostsResponse(any(Pageable.class));
        }

        @Test
        @DisplayName("Get user saved posts should return internal server error (500) on unexpected errors")
        void getUserSaved_InternalServerError() throws Exception {
            when(postService.getSavedPostsResponse(any(Pageable.class))).thenThrow(new RuntimeException("Internal server error"));

            mockMvc.perform(get(ApiUrls.GET_USER_SAVED.getUrl())
                            .param("pageNumber", "0")
                            .param("pageSize", "2")
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isInternalServerError());

            verify(postService).getSavedPostsResponse(any(Pageable.class));
        }
    }


    @Nested
    class GetPostsByCategory {

        @Test
        @DisplayName("Get posts by category should return success (200) when posts are retrieved successfully")
        void getPostsByCategory_Success() throws Exception {
            when(postService.getPostsByCategory(any(Pageable.class), any(String.class))).thenReturn(getShortUserPosts());

            mockMvc.perform(get(ApiUrls.GET_POSTS_BY_CATEGORY.getUrl())
                            .param("pageNumber", "0")
                            .param("pageSize", "2")
                            .param("category", "category")
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.message.content").isArray());

            verify(postService).getPostsByCategory(any(Pageable.class), any(String.class));
        }

        @Test
        @DisplayName("Get posts by category should return internal server error (500) on unexpected errors")
        void getPostsByCategory_InternalServerError() throws Exception {

            when(postService.getPostsByCategory(any(Pageable.class), any(String.class))).thenThrow(new RuntimeException("Internal server error"));

            mockMvc.perform(get(ApiUrls.GET_POSTS_BY_CATEGORY.getUrl())
                            .param("pageNumber", "0")
                            .param("pageSize", "2")
                            .param("category", "category")
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isInternalServerError());

            verify(postService).getPostsByCategory(any(Pageable.class), any(String.class));
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

    @Nested
    class ModifyPostTest {

        @Test
        @DisplayName("Modify post should return OK (200)")
        void modifyPost_Success() throws Exception {
            var request = getMockPost();
            var responseMessage = "Post modified successfully";

            when(postService.modifyPost(any(PostRequest.class))).thenReturn(responseMessage);

            MockHttpServletRequestBuilder requestBuilder = modifyRequest(request);

            mockMvc.perform(requestBuilder)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.message").value(responseMessage));

            verify(postService).modifyPost(any(PostRequest.class));
        }

        @Test
        @DisplayName("Modify post should return NOT FOUND (404)")
        void modifyPost_PostNotFound() throws Exception {
            var postId = "12345";
            var request = getMockPost();

            when(postService.modifyPost(any(PostRequest.class))).thenThrow(new PostNotFoundException(postId));

            MockHttpServletRequestBuilder requestBuilder = modifyRequest(request);

            mockMvc.perform(requestBuilder)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value(HttpStatus.NOT_FOUND.value()));

            verify(postService).modifyPost(any(PostRequest.class));
        }

        @Test
        @DisplayName("Modify post should return INTERNAL SERVER ERROR (500)")
        void modifyPost_InternalServerError() throws Exception {
            var request = getMockPost();

            when(postService.modifyPost(any(PostRequest.class))).thenThrow(new RuntimeException("Internal server error"));

            MockHttpServletRequestBuilder requestBuilder = modifyRequest(request);

            mockMvc.perform(requestBuilder)
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.errorCode").value(HttpStatus.INTERNAL_SERVER_ERROR.value()));

            verify(postService).modifyPost(any(PostRequest.class));
        }

    }


    private User getMockUser() {
        return User.builder()
                .id("mockId")
                .email("mock@mail.com")
                .nickname("mockNickname")
                .build();
    }

    private PostRequest getMockPost() {
        PostRequest p = new PostRequest();
        p.setTitle("mockPost");
        p.setCategory("mockCategory");
        return p;
    }

    private Page<PostResponse> getUserPosts(){
        User user = getMockUser();

        AccountInfoResponse accountInfo = AccountInfoResponse
                .builder()
                .nickname(user.getNickname())
                .build();

        var post1 = new PostResponse();
        post1.setTitle("title1");
        post1.setId("mockPost1");
        post1.setAccountInfo(accountInfo);

        var post2 = new PostResponse();
        post2.setTitle("title2");
        post2.setId("mockPost2");
        post2.setAccountInfo(accountInfo);

        return new PageImpl<>(List.of(
                post1, post2
        ));
    }

    private Page<ShortPostResponse> getShortUserPosts(){
        User user = getMockUser();

        AccountInfoResponse accountInfo = AccountInfoResponse
                .builder()
                .nickname(user.getNickname())
                .build();

        var post1 = new ShortPostResponse();
        post1.setTitle("title1");
        post1.setId("mockPost1");
        post1.setAccountInfo(accountInfo);

        var post2 = new ShortPostResponse();
        post2.setTitle("title2");
        post2.setId("mockPost2");
        post2.setAccountInfo(accountInfo);

        return new PageImpl<>(List.of(
                post1, post2
        ));
    }

    private Set<ShortPostResponse> getShortUserPostsList(){
        User user = getMockUser();

        AccountInfoResponse accountInfo = AccountInfoResponse
                .builder()
                .nickname(user.getNickname())
                .build();

        var post1 = new ShortPostResponse();
        post1.setTitle("title1");
        post1.setId("mockPost1");
        post1.setAccountInfo(accountInfo);

        var post2 = new ShortPostResponse();
        post2.setTitle("title2");
        post2.setId("mockPost2");
        post2.setAccountInfo(accountInfo);

        return Set.of(post1, post2);
    }

    private MockHttpServletRequestBuilder createJsonRequest(String url, PostRequest postRequest) throws Exception {
        String postRequestJson = objectMapper.writeValueAsString(postRequest);

        return post(url)
                .content(postRequestJson)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(ACCEPT_TYPE);
    }

    private MockHttpServletRequestBuilder createPostRequest(PostRequest postRequest) throws Exception {
        return createJsonRequest(ApiUrls.CREATE_POST.getUrl(), postRequest);
    }
    private MockHttpServletRequestBuilder modifyRequest(PostRequest postRequest) throws Exception {
        return createJsonRequest(ApiUrls.MODIFY_POST.getUrl(), postRequest)
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                });
    }
    private List<MockMultipartFile> createMockImages() {
        MockMultipartFile image1 = new MockMultipartFile("images", "image1.jpg", "image/jpeg", "image1".getBytes());
        MockMultipartFile image2 = new MockMultipartFile("images", "image2.jpg", "image/jpeg", "image2".getBytes());
        return List.of(image1, image2);
    }
}
