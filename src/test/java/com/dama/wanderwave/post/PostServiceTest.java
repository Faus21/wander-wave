package com.dama.wanderwave.post;

import com.dama.wanderwave.categoryType.CategoryType;
import com.dama.wanderwave.categoryType.CategoryTypeRepository;
import com.dama.wanderwave.comment.CommentRepository;
import com.dama.wanderwave.handler.post.CategoryTypeNotFoundException;
import com.dama.wanderwave.handler.post.PostNotFoundException;
import com.dama.wanderwave.handler.user.UserNotFoundException;
import com.dama.wanderwave.handler.user.like.IsLikedException;
import com.dama.wanderwave.handler.user.like.LikeNotFoundException;
import com.dama.wanderwave.handler.user.save.IsSavedException;
import com.dama.wanderwave.handler.user.save.SavedPostNotFound;
import com.dama.wanderwave.hashtag.HashTag;
import com.dama.wanderwave.hashtag.HashTagRepository;
import com.dama.wanderwave.place.Place;
import com.dama.wanderwave.place.PlaceRepository;
import com.dama.wanderwave.place.PlaceRequest;
import com.dama.wanderwave.post.request.CreatePostRequest;
import com.dama.wanderwave.post.response.PostResponse;
import com.dama.wanderwave.post.response.ShortPostResponse;
import com.dama.wanderwave.user.User;
import com.dama.wanderwave.user.UserRepository;
import com.dama.wanderwave.user.UserService;
import com.dama.wanderwave.user.like.Like;
import com.dama.wanderwave.user.like.LikeId;
import com.dama.wanderwave.user.like.LikeRepository;
import com.dama.wanderwave.user.saved_post.SavedPost;
import com.dama.wanderwave.user.saved_post.SavedPostId;
import com.dama.wanderwave.user.saved_post.SavedPostRepository;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PostServiceTest {
    @InjectMocks
    private PostService postService;
    @Mock
    private PostRepository postRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserService userService;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private HashTagRepository  hashTagRepository;
    @Mock
    private CategoryTypeRepository categoryTypeRepository;
    @Mock
    private LikeRepository likeRepository;
    @Mock
    private SavedPostRepository savedPostRepository;
    @Mock
    private PlaceRepository placeRepository;

    private Authentication authentication;

    @BeforeEach
    void setUp() {
        authentication = mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Nested
    class GetUserPosts{

        @Test
        @DisplayName("Get user posts should be ok")
        void getUserPosts_Success(){
            var mockUser = getMockUser();
            var mockPosts = getUserPosts();

            when(userRepository.findByNickname(mockUser.getNickname())).thenReturn(Optional.of(mockUser));
            when(postRepository.findByUserWithHashtags(mockUser, getPageRequest())).thenReturn(new PageImpl<>(mockPosts));

            Page<ShortPostResponse> result = postService.getUserPosts(getPageRequest(), mockUser.getNickname());

            assertNotNull(result);
            assertEquals(mockPosts.size(), result.getTotalElements());
            assertEquals(mockPosts.getFirst().getTitle(), result.getContent().getFirst().getTitle());

            verify(userRepository).findByNickname(mockUser.getNickname());
            verify(postRepository).findByUserWithHashtags(mockUser, getPageRequest());
        }

        @Test
        @DisplayName("Get zero user posts should be ok")
        void getZeroUserPosts_Success(){
            var mockUser = getMockUser();

            when(userRepository.findByNickname(mockUser.getNickname())).thenReturn(Optional.of(mockUser));
            when(postRepository.findByUserWithHashtags(mockUser, getPageRequest())).thenReturn(new PageImpl<>(new ArrayList<>()));

            Page<ShortPostResponse> result = postService.getUserPosts(getPageRequest(), mockUser.getNickname());

            assertNotNull(result);
            assertEquals(0, result.getTotalElements());

            verify(userRepository).findByNickname(mockUser.getNickname());
            verify(postRepository).findByUserWithHashtags(mockUser, getPageRequest());
        }

        @Test
        @DisplayName("Get zero user posts should be ok")
        void getUserPosts_UserNotFound(){
            var mockUser = getMockUser();

            when(userRepository.findByNickname(mockUser.getNickname())).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> postService.getUserPosts(getPageRequest(), mockUser.getNickname()));

            verify(userRepository).findByNickname(mockUser.getNickname());
            verify(postRepository, never()).findByUserWithHashtags(any(User.class), any(Pageable.class));
        }
    }

    @Nested
    class CreatePost{

        @Test
        void createPost_Success(){
            User mockUser = getMockUser();

            when(userService.getAuthenticatedUser()).thenReturn(mockUser);
            when(hashTagRepository.findByTitle(any(String.class))).thenReturn(Optional.of(getMockHashtag()));
            when(categoryTypeRepository.findByName(any(String.class))).thenReturn(Optional.of(getMockCategoryType()));
            when(postRepository.save(any(Post.class))).thenReturn(getUserPosts().getFirst());
            when(placeRepository.save(any(Place.class))).thenReturn(new Place());

            var response = postService.createPost(getMockPostCreateRequest());
            assertNotNull(response);
            assertEquals("Post is created successfully!", response);

            verify(userService).getAuthenticatedUser();
            verify(hashTagRepository).findByTitle(any(String.class));
            verify(categoryTypeRepository).findByName(any(String.class));
            verify(postRepository).save(any(Post.class));
            verify(placeRepository).save(any(Place.class));
        }

        @Test
        void createPost_CategoryNotFound(){
            User mockUser = getMockUser();

            when(userService.getAuthenticatedUser()).thenReturn(mockUser);
            when(hashTagRepository.findByTitle(any(String.class))).thenReturn(Optional.of(getMockHashtag()));
            when(categoryTypeRepository.findByName(any(String.class))).thenThrow(CategoryTypeNotFoundException.class);

            assertThrows(CategoryTypeNotFoundException.class, () -> postService.createPost(getMockPostCreateRequest()));

            verify(userService).getAuthenticatedUser();
            verify(hashTagRepository).findByTitle(any(String.class));
            verify(categoryTypeRepository).findByName(any(String.class));
            verify(postRepository, never()).save(any(Post.class));
            verify(placeRepository, never()).save(any(Place.class));
        }

    }

    @Nested
    class LikePost{
        @Test
        void likePost_Success(){
            User mockUser = getMockUser();

            when(userService.getAuthenticatedUser()).thenReturn(mockUser);
            when(postRepository.findByIdWithLikes(any(String.class))).thenReturn(Optional.of(getUserPosts().getFirst()));
            when(likeRepository.save(any(Like.class))).thenReturn(new Like());

            String result = postService.likePost(getUserPosts().getFirst().getId());

            assertNotNull(result);
            assertEquals("Liked successfully!", result);

            verify(userService).getAuthenticatedUser();
            verify(postRepository).findByIdWithLikes(any(String.class));
            verify(likeRepository).save(any(Like.class));
        }

        @Test
        void likePost_BadRequest(){
            User mockUser = getMockUser();

            when(userService.getAuthenticatedUser()).thenReturn(mockUser);
            when(postRepository.findByIdWithLikes(any(String.class))).thenReturn(Optional.of(getLikedPost()));

            assertThrows(IsLikedException.class, () -> postService.likePost(getLikedPost().getId()));

            verify(userService).getAuthenticatedUser();
            verify(postRepository).findByIdWithLikes(any(String.class));
            verify(likeRepository, never()).save(any(Like.class));
        }

        @Test
        void likePost_NotFound(){
            User mockUser = getMockUser();

            when(userService.getAuthenticatedUser()).thenReturn(mockUser);
            when(postRepository.findByIdWithLikes(any(String.class))).thenThrow(PostNotFoundException.class);

            assertThrows(PostNotFoundException.class, () -> postService.likePost(getLikedPost().getId()));

            verify(userService).getAuthenticatedUser();
            verify(postRepository).findByIdWithLikes(any(String.class));
            verify(likeRepository, never()).save(any(Like.class));
        }
    }

    @Nested
    class UnlikePost{
        @Test
        void unlikePost_Success(){
            User mockUser = getMockUser();

            when(userService.getAuthenticatedUser()).thenReturn(mockUser);
            when(postRepository.findById(any(String.class))).thenReturn(Optional.of(getUserPosts().getFirst()));
            when(likeRepository.findByUserAndPost(any(User.class), any(Post.class))).thenReturn(Optional.of(getLike()));

            String result = postService.unlikePost(getUserPosts().getFirst().getId());

            assertNotNull(result);
            assertEquals("Post is unliked successfully!", result);

            verify(userService).getAuthenticatedUser();
            verify(postRepository).findById(any(String.class));
            verify(likeRepository).findByUserAndPost(any(User.class), any(Post.class));
            verify(likeRepository).delete(any(Like.class));
        }

        @Test
        void unlikePost_LikeNotFound(){
            User mockUser = getMockUser();

            when(userService.getAuthenticatedUser()).thenReturn(mockUser);
            when(postRepository.findById(any(String.class))).thenReturn(Optional.of(getUserPosts().getFirst()));
            when(likeRepository.findByUserAndPost(any(User.class), any(Post.class))).thenThrow(LikeNotFoundException.class);

            assertThrows(LikeNotFoundException.class, () -> postService.unlikePost(getUserPosts().getFirst().getId()));

            verify(userService).getAuthenticatedUser();
            verify(postRepository).findById(any(String.class));
            verify(likeRepository).findByUserAndPost(any(User.class), any(Post.class));
            verify(likeRepository, never()).delete(any(Like.class));
        }

        @Test
        void unlikePost_PostNotFound(){
            User mockUser = getMockUser();

            when(userService.getAuthenticatedUser()).thenReturn(mockUser);
            when(postRepository.findById(any(String.class))).thenThrow(PostNotFoundException.class);

            assertThrows(PostNotFoundException.class, () -> postService.unlikePost(getUserPosts().getFirst().getId()));

            verify(userService).getAuthenticatedUser();
            verify(postRepository).findById(any(String.class));
            verify(likeRepository,never()).findByUserAndPost(any(User.class), any(Post.class));
            verify(likeRepository, never()).delete(any(Like.class));
        }
    }

    @Nested
    class GetPostLikes{
        @Test
        void getPostLikes_Success(){
            Post post = getLikedPost();
            post.setLikesCount(1);
            when(postRepository.findByIdWithLikes(any(String.class))).thenReturn(Optional.of(post));

            Integer result = postService.getPostLikesCount(getLikedPost().getId());

            assertNotNull(result);
            assertEquals(1, result);

            verify(postRepository).findByIdWithLikes(any(String.class));
        }

        @Test
        void getPostLikes_NotFound(){
            when(postRepository.findByIdWithLikes(any(String.class))).thenThrow(PostNotFoundException.class);

            assertThrows(PostNotFoundException.class, () -> postService.getPostLikesCount(getLikedPost().getId()));

            verify(postRepository).findByIdWithLikes(any(String.class));
        }
    }

    @Nested
    class SavePost{
        @Test
        void savePost_Success(){
            User mockUser = getMockUser();

            when(userService.getAuthenticatedUser()).thenReturn(mockUser);
            when(postRepository.findByIdSaved(any(String.class))).thenReturn(Optional.of(getUserPosts().getFirst()));
            when(savedPostRepository.save(any(SavedPost.class))).thenReturn(new SavedPost());

            String result = postService.savePost(getUserPosts().getFirst().getId());

            assertNotNull(result);
            assertEquals("Saved successfully!", result);

            verify(userService).getAuthenticatedUser();
            verify(postRepository).findByIdSaved(any(String.class));
            verify(savedPostRepository).save(any(SavedPost.class));
        }

        @Test
        void savePost_BadRequest(){
            User mockUser = getMockUser();

            when(userService.getAuthenticatedUser()).thenReturn(mockUser);
            when(postRepository.findByIdSaved(any(String.class))).thenReturn(Optional.of(getSavedPost()));

            assertThrows(IsSavedException.class, () -> postService.savePost(getLikedPost().getId()));

            verify(userService).getAuthenticatedUser();
            verify(postRepository).findByIdSaved(any(String.class));
            verify(savedPostRepository, never()).save(any(SavedPost.class));
        }

        @Test
        void savedPost_NotFound(){
            User mockUser = getMockUser();

            when(userService.getAuthenticatedUser()).thenReturn(mockUser);
            when(postRepository.findByIdSaved(any(String.class))).thenThrow(PostNotFoundException.class);

            assertThrows(PostNotFoundException.class, () -> postService.savePost(getSavedPost().getId()));

            verify(userService).getAuthenticatedUser();
            verify(postRepository).findByIdSaved(any(String.class));
            verify(likeRepository, never()).save(any(Like.class));
        }
    }

    @Nested
    class UnsavePost{
        @Test
        void unsavePost_Success(){
            User mockUser = getMockUser();

            when(userService.getAuthenticatedUser()).thenReturn(mockUser);
            when(postRepository.findById(any(String.class))).thenReturn(Optional.of(getUserPosts().getFirst()));
            when(savedPostRepository.findByUserAndPost(any(User.class), any(Post.class))).thenReturn(Optional.of(getSave()));

            String result = postService.unsavePost(getUserPosts().getFirst().getId());

            assertNotNull(result);
            assertEquals("Post is unsaved successfully!", result);

            verify(userService).getAuthenticatedUser();
            verify(postRepository).findById(any(String.class));
            verify(savedPostRepository).findByUserAndPost(any(User.class), any(Post.class));
            verify(savedPostRepository).delete(any(SavedPost.class));
        }

        @Test
        void unsavePost_SaveNotFound(){
            User mockUser = getMockUser();

            when(userService.getAuthenticatedUser()).thenReturn(mockUser);
            when(postRepository.findById(any(String.class))).thenReturn(Optional.of(getUserPosts().getFirst()));
            when(savedPostRepository.findByUserAndPost(any(User.class), any(Post.class))).thenThrow(SavedPostNotFound.class);

            assertThrows(SavedPostNotFound.class, () -> postService.unsavePost(getUserPosts().getFirst().getId()));

            verify(userService).getAuthenticatedUser();
            verify(postRepository).findById(any(String.class));
            verify(savedPostRepository).findByUserAndPost(any(User.class), any(Post.class));
            verify(savedPostRepository, never()).delete(any(SavedPost.class));
        }

        @Test
        void unsavePost_PostNotFound(){
            User mockUser = getMockUser();

            when(userService.getAuthenticatedUser()).thenReturn(mockUser);
            when(postRepository.findById(any(String.class))).thenThrow(PostNotFoundException.class);

            assertThrows(PostNotFoundException.class, () -> postService.unsavePost(getUserPosts().getFirst().getId()));

            verify(userService).getAuthenticatedUser();
            verify(postRepository).findById(any(String.class));
            verify(savedPostRepository,never()).findByUserAndPost(any(User.class), any(Post.class));
            verify(savedPostRepository, never()).delete(any(SavedPost.class));
        }
    }

    @Nested
    class DeletePost {

        @Test
        void deletePost_Success() {
            User mockUser = getMockUser();
            Post mockPost = getUserPosts().getFirst();

            when(userService.getAuthenticatedUser()).thenReturn(mockUser);
            when(postRepository.findById(mockPost.getId())).thenReturn(Optional.of(mockPost));

            String result = postService.deletePost(mockPost.getId());

            assertNotNull(result);
            assertEquals("Deleted successfully!", result);
            verify(userService).getAuthenticatedUser();
            verify(postRepository).findById(mockPost.getId());
            verify(postRepository).delete(mockPost);
        }

        @Test
        void deletePost_NotAllowed() {
            User mockUser = getMockUser();
            User anotherUser = User.builder().id("anotherUser").build();
            Post mockPost = getUserPosts().getFirst();
            mockPost.setUser(anotherUser);

            when(userService.getAuthenticatedUser()).thenReturn(mockUser);
            when(postRepository.findById(mockPost.getId())).thenReturn(Optional.of(mockPost));

            String result = postService.deletePost(mockPost.getId());

            assertNotNull(result);
            assertEquals("You are not allowed to delete this post!", result);
            verify(userService).getAuthenticatedUser();
            verify(postRepository).findById(mockPost.getId());
            verify(postRepository, never()).delete(mockPost);
        }

        @Test
        void deletePost_PostNotFound() {
            when(userService.getAuthenticatedUser()).thenReturn(getMockUser());
            when(postRepository.findById(anyString())).thenThrow(PostNotFoundException.class);

            assertThrows(PostNotFoundException.class, () -> postService.deletePost("nonExistentPostId"));

            verify(userService).getAuthenticatedUser();
            verify(postRepository).findById(any(String.class));
            verify(postRepository, never()).delete(any(Post.class));
        }
    }

    @Nested
    class PersonalFlow {

        @Test
        void personalFlow_Success() {
            User mockUser = getMockUser();
            User mockSubscription = User.builder().id("subscriptionId").nickname("subscriptionNickname").build();
            mockUser.setSubscriptions(Set.of(mockSubscription.getId()));
            Post mockPost = getUserPosts().getFirst();

            when(userService.getAuthenticatedUser())
                    .thenReturn(mockUser);
            when(userRepository.findByIdAndFetchSubscriptions(mockUser.getId()))
                    .thenReturn(List.of(mockSubscription.getId()));
            when(userRepository.findById(mockSubscription.getId()))
                    .thenReturn(Optional.of(mockSubscription));
            when(postRepository.findByUserWithHashtags(mockSubscription, getPageRequest()))
                    .thenReturn(new PageImpl<>(List.of(mockPost)));

            Page<ShortPostResponse> result = postService.personalFlow(getPageRequest());

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(postRepository).findByUserWithHashtags(mockSubscription, getPageRequest());
        }
    }

    @Nested
    class RecommendationFlow {

        @Test
        void recommendationFlow_Success() {
            User mockUser = getMockUser();
            Post mockPost1 = getUserPosts().getFirst();
            HashTag mockHashTag = getMockHashtag();
            when(userService.getAuthenticatedUser()).thenReturn(mockUser);

            when(postRepository.findByUserWithLikes(any(User.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(mockPost1)));
            when(postRepository.findByUserSaved(any(), any(Pageable.class))).thenReturn(new PageImpl<>(new ArrayList<>()));

            when(postRepository.findByHashtag(mockHashTag.getId(), getPageRequest()))
                    .thenReturn(new PageImpl<>(List.of(mockPost1)));
            when(postRepository.findPopularPosts(any(Pageable.class), any(LocalDateTime.class))).thenReturn(new PageImpl<>(List.of(mockPost1)));

            Page<ShortPostResponse> result = postService.recommendationFlow(getPageRequest());

            assertNotNull(result);
            assertEquals(2, result.getTotalElements());
            verify(postRepository).findByHashtag(anyString(), any(Pageable.class));
        }
    }

    @Nested
    class GetLikedPostsResponse{
        @Test
        void getLikedPostsResponse_Success() {
            User mockUser = getMockUser();
            List<Post> posts = getUserPosts();

            when(userService.getAuthenticatedUser()).thenReturn(mockUser);
            when(postRepository.findByUserWithLikes(mockUser, getPageRequest()))
                    .thenReturn(new PageImpl<>(posts, getPageRequest(), posts.size()));

            Page<PostResponse> result = postService.getLikedPostsResponse(getPageRequest());

            assertNotNull(result);
            assertEquals(2, result.getTotalElements());

            verify(userService, times(3)).getAuthenticatedUser();
            verify(postRepository).findByUserWithLikes(mockUser, getPageRequest());
        }
    }

    @Nested
    class GetSavedPostsResponse{
        @Test
        void getSavedPostsResponse_Success() {
            User mockUser = getMockUser();
            List<Post> posts = getUserPosts();

            when(userService.getAuthenticatedUser()).thenReturn(mockUser);
            when(postRepository.findByUserSaved(mockUser, getPageRequest()))
                    .thenReturn(new PageImpl<>(posts, getPageRequest(), posts.size()));

            Page<ShortPostResponse> result = postService.getSavedPostsResponse(getPageRequest());

            assertNotNull(result);
            assertEquals(2, result.getTotalElements());

            verify(userService, times(3)).getAuthenticatedUser();
            verify(postRepository).findByUserSaved(mockUser, getPageRequest());
        }
    }

    @Nested
    class GetPostsByCategory{
        @Test
        void getPostsByCategory_Success() {

            when(postRepository.findByCategory(any(String.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(getUserPosts()));

            Page<ShortPostResponse> result = postService.getPostsByCategory(getPageRequest(), "category");
            assertNotNull(result);
            assertEquals(2, result.getTotalElements());

            verify(postRepository).findByCategory(anyString(), any(Pageable.class));
        }
    }

    @Nested
    class GetPostById {

        @Test
        @DisplayName("Get post by ID should return post successfully")
        void getPostById_Success() {
            var postId = "12345";
            var mockPost = getMockPost(postId);
            when(postRepository.findByIdAndFetchHashtags(postId)).thenReturn(Optional.of(mockPost));
            PostResponse result = postService.getPostById(postId);

            assertNotNull(result);
            assertEquals(mockPost.getId(), result.getId());
            assertEquals(mockPost.getTitle(), result.getTitle());

            verify(postRepository).findByIdAndFetchHashtags(postId);
        }

        @Test
        @DisplayName("Get post by ID should throw exception when post is not found")
        void getPostById_PostNotFound() {
            var postId = "12345";
            when(postRepository.findByIdAndFetchHashtags(postId)).thenReturn(Optional.empty());

            assertThrows(PostNotFoundException.class, () -> postService.getPostById(postId));

            verify(postRepository).findByIdAndFetchHashtags(postId);
        }

    }

    @Nested
    class ModifyPost {

        @Test
        @DisplayName("Modify post should update fields successfully")
        void modifyPost_Success() {
            var postId = "12345";
            var mockPost = getMockPost(postId);
            var request = getMockPostCreateRequest();

            when(postRepository.findById(postId)).thenReturn(Optional.of(mockPost));
            String result = postService.modifyPost(postId, request);

            assertEquals("Post modified successfully", result);
            assertEquals(request.getTitle(), mockPost.getTitle());
            assertEquals(request.getDescription(), mockPost.getDescription());
            assertArrayEquals(request.getPros().toArray(new String[0]), mockPost.getPros());
            assertArrayEquals(request.getCons().toArray(new String[0]), mockPost.getCons());

            verify(postRepository).findById(postId);
            verify(postRepository).save(mockPost);
        }

        @Test
        @DisplayName("Modify post should throw exception when post not found")
        void modifyPost_PostNotFound() {
            var postId = "12345";
            var request = getMockPostCreateRequest();

            when(postRepository.findById(postId)).thenReturn(Optional.empty());

            assertThrows(PostNotFoundException.class, () -> postService.modifyPost(postId, request));

            verify(postRepository).findById(postId);
            verify(postRepository, never()).save(any(Post.class));
        }

    }

    private Post getMockPost(String postId) {
        Post mockPost = new Post();
        mockPost.setId(postId);
        mockPost.setTitle("mock");
        mockPost.setHashtags(new HashSet<>());
        mockPost.setUser(new User());
        mockPost.setCategoryType(new CategoryType());
        return mockPost;
    }


    private User getMockUser() {
        return User.builder()
                .id("mockId")
                .email("mock@mail.com")
                .nickname("mockNickname")
                .build();
    }

    private Like getLike(){
        LikeId likeId = new LikeId();
        likeId.setPost_id("1");
        likeId.setUser_id("1");
        Like like = new Like();
        like.setId(likeId);
        return like;
    }

    private SavedPost getSave(){
        SavedPostId savedPostId = new SavedPostId();
        savedPostId.setPost_id("1");
        savedPostId.setUser_id("1");
        SavedPost savedPost = new SavedPost();
        savedPost.setId(savedPostId);
        return savedPost;
    }

    private Post getSavedPost(){
        User user = getMockUser();

        Post post1 = new Post();
        post1.setTitle("title1");
        post1.setId("mockPost1");
        post1.setUser(user);
        var saved = new HashSet<SavedPost>();
        SavedPost savedPost = new SavedPost();
        SavedPostId savedPostId = new SavedPostId();
        savedPostId.setPost_id("1");
        savedPostId.setUser_id("1");
        savedPost.setId(savedPostId);
        savedPost.setUser(user);
        savedPost.setPost(post1);

        saved.add(savedPost);

        post1.setSavedPosts(saved);
        return post1;
    }

    private CreatePostRequest getMockPostCreateRequest(){
        CreatePostRequest pr = CreatePostRequest.builder()
                .hashtags(Set.of(getMockHashtag().getTitle()))
                .categoryName(getMockCategoryType().getName())
                .places(List.of(new PlaceRequest()))
                .build();
        pr.setTitle("title1");
        pr.setPros(List.of("pros"));
        pr.setCons(List.of("cons"));
        pr.setDescription("description1");
        return pr;
    }

    private Post getLikedPost(){
        User user = getMockUser();

        Post post1 = new Post();
        post1.setTitle("title1");
        post1.setId("mockPost1");
        post1.setUser(user);

        var likeSet = new HashSet<Like>();
        var like1 = new Like();
        like1.setPost(post1);
        like1.setUser(user);
        likeSet.add(like1);
        post1.setLikes(likeSet);

        return post1;
    }

    private List<Post> getUserPosts(){
        User user = getMockUser();

        Post post1 = new Post();
        post1.setTitle("title1");
        post1.setId("mockPost1");
        post1.setUser(user);
        post1.setHashtags(Set.of(getMockHashtag()));
        post1.setCategoryType(getMockCategoryType());

        Post post2 = new Post();
        post2.setTitle("title2");
        post2.setId("mockPost2");
        post2.setUser(user);
        post2.setHashtags(Set.of(getMockHashtag()));
        post2.setCategoryType(getMockCategoryType());

        return List.of(
            post1, post2
        );
    }

    private HashTag getMockHashtag(){
        HashTag hashTag = new HashTag();
        hashTag.setTitle("title");
        hashTag.setId("mockHashtag");
        return hashTag;
    }

    private CategoryType getMockCategoryType(){
        CategoryType categoryType = new CategoryType();
        categoryType.setName("categoryType");
        categoryType.setId("mockHashtag");
        return categoryType;
    }

    private PageRequest getPageRequest() {
        return PageRequest.of(0, 10);
    }

}
