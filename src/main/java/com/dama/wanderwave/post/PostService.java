package com.dama.wanderwave.post;

import com.dama.wanderwave.categoryType.CategoryTypeRepository;
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
import com.dama.wanderwave.post.request.CreatePostRequest;
import com.dama.wanderwave.place.PlaceRequest;
import com.dama.wanderwave.post.request.PostRequest;
import com.dama.wanderwave.post.response.PostResponse;
import com.dama.wanderwave.user.User;
import com.dama.wanderwave.user.UserRepository;
import com.dama.wanderwave.user.UserService;
import com.dama.wanderwave.user.like.Like;
import com.dama.wanderwave.user.like.LikeId;
import com.dama.wanderwave.user.like.LikeRepository;
import com.dama.wanderwave.user.saved_post.SavedPost;
import com.dama.wanderwave.user.saved_post.SavedPostId;
import com.dama.wanderwave.user.saved_post.SavedPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PostService {
    private final Integer PAGE_SIZE = 50;

    private final PostRepository  postRepository;
    private final UserService userService;
    private final UserRepository userRepository;
    private final HashTagRepository hashTagRepository;
    private final CategoryTypeRepository categoryTypeRepository;
    private final LikeRepository likeRepository;
    private final SavedPostRepository savedPostRepository;
    private final PlaceRepository placeRepository;

    public Page<PostResponse> getUserPosts(Pageable pageRequest, String nickname) {
        log.info("getUserPosts called with nickname: {}", nickname);
        User user = userRepository.findByNickname(nickname)
                .orElseThrow(() -> new UserNotFoundException(nickname));

        Page<Post> result = postRepository.findByUserWithHashtags(user, pageRequest);
        Page<PostResponse> postResponses = getPostResponseListFromPostList(pageRequest, result);

        log.info("getUserPosts returned {} posts for nickname: {}", postResponses.getSize(), nickname);
        return postResponses;
    }

    @Transactional
    public String modifyPost(String postId, PostRequest request) {
        log.info("modifyPost called with request: {}", request);

        Post post = postRepository.findById(postId).orElseThrow(() -> new PostNotFoundException(postId));

        if (Objects.nonNull(request.getTitle()))
            post.setTitle(request.getTitle());

        if (Objects.nonNull(request.getDescription()))
            post.setDescription(request.getDescription());

        if (!request.getPros().isEmpty())
            post.setPros(request.getPros().toArray(new String[0]));

        if (!request.getCons().isEmpty())
            post.setCons(request.getCons().toArray(new String[0]));

        postRepository.save(post);

        return "Post modified successfully";
    }

    @Transactional
    public String createPost(CreatePostRequest createPostRequest) {
        log.info("createPost called with request: {}", createPostRequest);
        User user = userService.getAuthenticatedUser();

        Post post = new Post();
        post.setTitle(createPostRequest.getTitle());
        post.setDescription(createPostRequest.getDescription());
        post.setUser(user);
        post.setCreatedAt(LocalDateTime.now());
        post.setPros(createPostRequest.getPros().toArray(new String[0]));
        post.setCons(createPostRequest.getCons().toArray(new String[0]));

        Set<HashTag> hashTags = new HashSet<>();
        for (String hashtag : createPostRequest.getHashtags()) {
            HashTag hashTag = hashTagRepository.findByTitle(hashtag)
                    .orElseGet(() -> {
                        HashTag newHashtag = new HashTag();
                        newHashtag.setTitle(hashtag);
                        return hashTagRepository.save(newHashtag);
                    });

            hashTags.add(hashTag);
        }

        post.setHashtags(hashTags);
        post.setCategoryType(categoryTypeRepository.findByName(createPostRequest.getCategoryName())
                .orElseThrow(() -> new CategoryTypeNotFoundException("CategoryType is not found!")));

        postRepository.save(post);

        for (PlaceRequest placeRequest : createPostRequest.getPlaces()) {
            Place place = new Place();
            place.setDescription(placeRequest.getDescription());
            place.setLongitude(placeRequest.getLongitude());
            place.setLatitude(placeRequest.getLatitude());
            place.setRating(placeRequest.getRating());
            place.setImageUrl(placeRequest.getImageUrl());
            place.setPost(post);

            placeRepository.save(place);
        }

        log.info("createPost successfully created post with title: {}", createPostRequest.getTitle());
        return "Post is created successfully!";
    }

    @Transactional
    public String likePost(String postId) {
        log.info("likePost called with postId: {}", postId);
        User user = userService.getAuthenticatedUser();

        Post post = postRepository.findByIdWithLikes(postId)
                .orElseThrow(() -> new PostNotFoundException("Post with id " + postId + " is not found!"));

        boolean isLiked = post.getLikes().stream().anyMatch(e -> e.getUser().getId().equals(user.getId()));

        if (isLiked)
            throw new IsLikedException("Post is already liked by user!");

        LikeId likeId = new LikeId();
        likeId.setUser_id(user.getId());
        likeId.setPost_id(post.getId());

        Like like = new Like();
        like.setId(likeId);
        like.setUser(user);
        like.setPost(post);
        like.setCreatedAt(LocalDateTime.now());

        likeRepository.save(like);
        log.info("likePost successfully liked post with id: {}", postId);
        return "Liked successfully!";
    }

    @Transactional
    public String unlikePost(String postId) {
        log.info("unlikePost called with postId: {}", postId);
        User user = userService.getAuthenticatedUser();

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post with id " + postId + " is not found!"));

        Like like = likeRepository.findByUserAndPost(user, post)
                .orElseThrow(() -> new LikeNotFoundException("This post isn't liked by user!"));

        likeRepository.delete(like);
        log.info("unlikePost successfully unliked post with id: {}", postId);
        return "Post is unliked successfully!";
    }

    public Integer getPostLikesCount(String postId) {
        log.info("getPostLikesCount called with postId: {}", postId);
        Post post = postRepository.findByIdWithLikes(postId)
                .orElseThrow(() -> new PostNotFoundException("Post with id " + postId + " is not found!"));

        int count = post.getLikes().size();
        log.info("getPostLikesCount returned count: {} for postId: {}", count, postId);

        return count;
    }

    @Transactional
    public String savePost(String postId) {
        log.info("savePost called with postId: {}", postId);
        User user = userService.getAuthenticatedUser();

        Post post = postRepository.findByIdSaved(postId)
                .orElseThrow(() -> new PostNotFoundException("Post with id " + postId + " is not found!"));

        if (post.getSavedPosts().stream().anyMatch(e -> e.getUser().getId().equals(user.getId())))
            throw new IsSavedException("Post is already saved by user!");

        SavedPostId savedPostId = new SavedPostId();
        savedPostId.setUser_id(user.getId());
        savedPostId.setPost_id(post.getId());

        SavedPost savedPost = new SavedPost();
        savedPost.setId(savedPostId);
        savedPost.setUser(user);
        savedPost.setPost(post);

        savedPostRepository.save(savedPost);
        log.info("savePost successfully saved post with id: {}", postId);
        return "Saved successfully!";
    }

    @Transactional
    public String unsavePost(String postId) {
        log.info("unsavePost called with postId: {}", postId);
        User user = userService.getAuthenticatedUser();

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post with id " + postId + " is not found!"));

        SavedPost savedPost = savedPostRepository.findByUserAndPost(user, post)
                .orElseThrow(() -> new SavedPostNotFound("This post isn't saved by user!"));

        savedPostRepository.delete(savedPost);
        log.info("unsavePost successfully unsaved post with id: {}", postId);
        return "Post is unsaved successfully!";
    }

    @Transactional
    public Page<PostResponse> personalFlow(Pageable pageRequest) {
        log.info("personalFlow called");
        User user = userService.getAuthenticatedUser();

        List<PostResponse> response = new ArrayList<>();
        for (String us : user.getSubscriptions()) {
            User subscription = userRepository.findById(us).get();

            Page<Post> posts = postRepository.findByUserWithHashtags(subscription, pageRequest);
            response.addAll(getPostResponseListFromPostList(pageRequest, posts).getContent());
        }

        response = response.stream().sorted(Comparator.comparing(PostResponse::getCreatedAt)).toList();
        log.info("personalFlow returned {} posts", response.size());
        return new PageImpl<>(response, pageRequest, response.size());
    }

    public Page<PostResponse> recommendationFlow(Pageable pageRequest) {
        log.info("recommendationFlow called");
        User user = userService.getAuthenticatedUser();

        List<PostResponse> response = new ArrayList<>();
        Page<Post> likedPosts = getLikedPosts(pageRequest, user);
        Page<Post> savedPosts = getSavedPosts(pageRequest, user);

        List<HashTag> hashtags = new ArrayList<>();
        int maxLikesIndex = Math.min(likedPosts.getSize(), PAGE_SIZE);
        List<Post> likedPostSubList = likedPosts.getContent().subList(0, Math.min(likedPosts.getContent().size(), maxLikesIndex));
        for (Post post : likedPostSubList) {
            hashtags.addAll(post.getHashtags());
        }

        int maxSavedIndex = Math.min(savedPosts.getSize(), PAGE_SIZE);
        List<Post> savedPostSubList = savedPosts.getContent().subList(0, Math.min(savedPosts.getContent().size(), maxSavedIndex));
        for (Post post : savedPostSubList) {
            hashtags.addAll(post.getHashtags());
        }

        List<Post> posts = new ArrayList<>();
        for (HashTag h : hashtags) {
            Page<Post> resp = postRepository.findByHashtag(h.getId(), pageRequest);
            posts.addAll(resp.getContent());
        }

        Page<Post> mostPopularPosts = getRandomPopularPosts();
        Page<Post> postsPage = new PageImpl<>(posts, pageRequest, posts.size());

        response.addAll(getPostResponseListFromPostList(pageRequest, postsPage).getContent());
        response.addAll(getPostResponseListFromPostList(pageRequest, mostPopularPosts).getContent());

        Collections.shuffle(response);

        log.info("recommendationFlow returned {} posts", response.size());
        return new PageImpl<>(response, pageRequest, response.size());
    }

    public Page<PostResponse> getLikedPostsResponse(Pageable pageRequest) {
        log.info("getLikedPostsResponse called");
        User user = userService.getAuthenticatedUser();

        Page<PostResponse> response = getPostResponseListFromPostList(pageRequest, getLikedPosts(pageRequest, user));
        log.info("getLikedPostsResponse returned {} posts", response.getSize());
        return response;
    }


    public Page<PostResponse> getSavedPostsResponse(Pageable pageRequest) {
        log.info("getSavedPostsResponse called");
        User user = userService.getAuthenticatedUser();

        Page<PostResponse> response = getPostResponseListFromPostList(pageRequest, getSavedPosts(pageRequest,user));
        log.info("getSavedPostsResponse returned {} posts", response.getSize());
        return response;
    }


    public Page<PostResponse> getPostsByCategory(Pageable pageRequest, String category) {
        log.info("getPostsByCategory called with category: {}", category);
        Page<Post> posts = postRepository.findByCategory(category, pageRequest);

        Page<PostResponse> response = getPostResponseListFromPostList(pageRequest, posts);
        log.info("getPostsByCategory returned {} posts for category: {}", response.getSize(), category);
        return response;
    }



    @Transactional
    public String deletePost(String postId) {
        log.info("deletePost called with postId: {}", postId);
        User user = userService.getAuthenticatedUser();

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        if (!post.getUser().getId().equals(user.getId())) {
            log.warn("deletePost failed: user {} is not authorized to delete post {}", user.getId(), postId);
            return "You are not allowed to delete this post!";
        }

        postRepository.delete(post);
        log.info("deletePost successfully deleted post with id: {}", postId);
        return "Deleted successfully!";
    }

    @Transactional
    public PostResponse getPostById(String postId) {
        log.info("getPostById called with postId: {}", postId);
        Post p = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        log.info("getPostById successfully returned post: {}", postId);
        return getPostResponseFromPost(p);
    }

    private Page<Post> getLikedPosts(Pageable pageRequest, User user) {
        log.info("getLikedPosts called for user: {}", user.getId());
        Page<Post> likedPosts = postRepository.findByUserWithLikes(user, pageRequest);
        log.info("getLikedPosts returned {} liked posts for user: {}", likedPosts.getSize(), user.getId());
        return likedPosts;
    }

    private Page<Post> getSavedPosts(Pageable pageRequest, User user) {
        log.info("getSavedPosts called for user: {}", user.getId());
        Page<Post> savedPosts = postRepository.findByUserSaved(user, pageRequest);
        log.info("getSavedPosts returned {} saved posts for user: {}", savedPosts.getSize(), user.getId());
        return savedPosts;
    }

    private PostResponse getPostResponseFromPost(Post p){
        log.info("getPostResponseFromPost from post: {}", p.getId());
        PostResponse postResponse = new PostResponse();
        postResponse.setTitle(p.getTitle());
        postResponse.setDescription(p.getDescription());
        postResponse.setId(p.getId());
        postResponse.setCreatedAt(p.getCreatedAt());
        postResponse.setHashtags(p.getHashtags().stream().map(HashTag::getTitle).collect(Collectors.toSet()));
        postResponse.setNickname(p.getUser().getNickname());
        postResponse.setCons(p.getCons());
        postResponse.setPros(p.getPros());
        postResponse.setCategoryType(p.getCategoryType().getName());

        return postResponse;
    }

    private Page<PostResponse> getPostResponseListFromPostList(Pageable pageRequest, Page<Post> posts) {
        log.info("getPostResponseListFromPostList called for {} posts", posts.getSize());
        List<PostResponse> response = new ArrayList<>();
        for (Post p : posts) {
            response.add(getPostResponseFromPost(p));
        }
        log.info("getPostResponseListFromPostList returned {} responses", response.size());
        return new PageImpl<>(response, pageRequest, response.size());
    }

    private Page<Post> getRandomPopularPosts() {
        LocalDateTime lastWeek = LocalDateTime.now().minusWeeks(1);
        return postRepository.findPopularPosts(PageRequest.of(0, PAGE_SIZE), lastWeek);
    }

}

