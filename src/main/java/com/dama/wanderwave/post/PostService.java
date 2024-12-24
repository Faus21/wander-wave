package com.dama.wanderwave.post;

import com.dama.wanderwave.categoryType.CategoryType;
import com.dama.wanderwave.categoryType.CategoryTypeRepository;
import com.dama.wanderwave.comment.Comment;
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
import com.dama.wanderwave.post.request.CreatePostRequest;
import com.dama.wanderwave.place.PlaceRequest;
import com.dama.wanderwave.post.request.PostRequest;
import com.dama.wanderwave.post.response.*;
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
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PostService {
    private final Integer PAGE_SIZE = 50;

    private final PostRepository postRepository;
    private final UserService userService;
    private final UserRepository userRepository;
    private final HashTagRepository hashTagRepository;
    private final CategoryTypeRepository categoryTypeRepository;
    private final LikeRepository likeRepository;
    private final SavedPostRepository savedPostRepository;
    private final PlaceRepository placeRepository;
    private final ModelMapper modelMapper;
    private final CommentRepository commentRepository;

    public Page<ShortPostResponse> getUserPosts(Pageable pageRequest, String nickname) {
        log.info("getUserPosts called with nickname: {}", nickname);
        User user = userRepository.findByNickname(nickname)
                .orElseThrow(() -> new UserNotFoundException(nickname));

        Page<Post> result = postRepository.findByUserWithHashtags(user, pageRequest);
        Page<ShortPostResponse> postResponses = getShortPostResponseListFromPostList(pageRequest, result);

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

        Post post = mapToPost(createPostRequest, user);
        postRepository.save(post);

        createPostRequest.getPlaces().stream()
                .map(placeRequest -> mapToPlace(placeRequest, post))
                .forEach(placeRepository::save);

        log.info("createPost successfully created post with title: {}", createPostRequest.getTitle());
        return "Post is created successfully!";
    }

    private Set<HashTag> createHashTags(Set<String> hashtags) {
        return hashtags.stream()
                .map(hashtag -> hashTagRepository.findByTitle(hashtag)
                        .orElseGet(() -> hashTagRepository.save(HashTag.builder().title(hashtag).build())))
                .collect(Collectors.toSet());
    }

    @Transactional
    public String likePost(String postId) {
        log.info("likePost called with postId: {}", postId);
        User user = userService.getAuthenticatedUser();

        Post post = postRepository.findByIdWithLikes(postId)
                .orElseThrow(() -> new PostNotFoundException("Post with id " + postId + " is not found!"));

        if (isPostAlreadyLikedByUser(post, user)) {
            throw new IsLikedException("Post is already liked by user!");
        }

        Like like = createLike(post, user);
        likeRepository.save(like);

        post.setLikesCount(post.getLikesCount() + 1);
        postRepository.save(post);

        log.info("likePost successfully liked post with id: {}", postId);
        return "Liked successfully!";
    }

    private boolean isPostAlreadyLikedByUser(Post post, User user) {
        return post.getLikes().stream()
                .anyMatch(like -> like.getUser().getId().equals(user.getId()));
    }

    private Like createLike(Post post, User user) {
        LikeId likeId = LikeId.builder()
                .user_id(user.getId())
                .post_id(post.getId())
                .build();

        return Like.builder()
                .id(likeId)
                .user(user)
                .post(post)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Transactional
    public String unlikePost(String postId) {
        log.info("unlikePost called with postId: {}", postId);
        User user = userService.getAuthenticatedUser();

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post with id " + postId + " is not found!"));

        Like like = likeRepository.findByUserAndPost(user, post)
                .orElseThrow(() -> new LikeNotFoundException("This post isn't liked by user!"));

        post.setLikesCount(post.getLikesCount() - 1);

        postRepository.save(post);
        likeRepository.delete(like);
        log.info("unlikePost successfully unliked post with id: {}", postId);
        return "Post is unliked successfully!";
    }

    public Integer getPostLikesCount(String postId) {
        log.info("getPostLikesCount called with postId: {}", postId);
        Post post = postRepository.findByIdWithLikes(postId)
                .orElseThrow(() -> new PostNotFoundException("Post with id " + postId + " is not found!"));

        int count = post.getLikesCount();
        log.info("getPostLikesCount returned count: {} for postId: {}", count, postId);
        return count;
    }

    @Transactional
    public String savePost(String postId) {
        log.info("savePost called with postId: {}", postId);
        User user = userService.getAuthenticatedUser();

        Post post = postRepository.findByIdSaved(postId)
                .orElseThrow(() -> new PostNotFoundException("Post with id " + postId + " is not found!"));

        if (post.getSavedPosts().stream().anyMatch(e -> e.getUser().getId().equals(user.getId()))) {
            throw new IsSavedException("Post is already saved by user!");
        }

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

    public Page<ShortPostResponse> personalFlow(Pageable pageRequest) {
        log.info("personalFlow called");
        User user = userService.getAuthenticatedUser();

        List<String> subscriptions = userRepository.findByIdAndFetchSubscriptions(user.getId());

        List<ShortPostResponse> response = new ArrayList<>();
        for (String us : subscriptions) {
            User subscription = userRepository.findById(us)
                    .orElseGet(() -> {
                        log.warn("User with ID {} not found. Skipping this subscription.", us);
                        return null;
                    });

            if (subscription == null) {
                continue;
            }

            Page<Post> posts = postRepository.findByUserWithHashtags(subscription, pageRequest);
            response.addAll(getShortPostResponseListFromPostList(pageRequest, posts).getContent());
        }

        response = response.stream().sorted(Comparator.comparing(ShortPostResponse::getCreationDate)).toList();
        log.info("personalFlow returned {} posts", response.size());
        return new PageImpl<>(response, pageRequest, response.size());
    }

    public Page<PostResponse> recommendationFlow(Pageable pageRequest) {
        log.info("recommendationFlow called");
        User user = userService.getAuthenticatedUser();

        List<Post> combinedPosts = Stream.concat(
                        getLikedPosts(pageRequest, user).getContent().stream(),
                        getSavedPosts(pageRequest, user).getContent().stream())
                .toList();

        Set<HashTag> uniqueHashtags = combinedPosts.stream()
                .flatMap(post -> post.getHashtags().stream())
                .collect(Collectors.toSet());

        List<Post> hashtagPosts = new ArrayList<>();
        if (!uniqueHashtags.isEmpty()) {
            hashtagPosts = uniqueHashtags.stream()
                    .flatMap(h -> postRepository.findByHashtag(h.getId(), pageRequest).getContent().stream())
                    .toList();
        }

        Page<Post> mostPopularPosts = getRandomPopularPosts();

        List<Post> allPosts = new ArrayList<>();
        allPosts.addAll(hashtagPosts);
        allPosts.addAll(mostPopularPosts.getContent());

        Collections.shuffle(allPosts);

        List<PostResponse> response = new ArrayList<>(
                getPostResponseListFromPostList(
                        pageRequest, new PageImpl<>(allPosts, pageRequest, allPosts.size())
                ).getContent()
        );

        Page<PostResponse> finalPage = new PageImpl<>(response, pageRequest, response.size());

        log.info("recommendationFlow returned {} posts", finalPage.getTotalElements());
        return finalPage;
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

        Page<PostResponse> response = getPostResponseListFromPostList(pageRequest, getSavedPosts(pageRequest, user));
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

    public PostResponse getPostById(String postId) {
        log.info("getPostById called with postId: {}", postId);
        Post p = postRepository.findByIdAndFetchHashtags(postId)
                .orElseThrow(() -> new PostNotFoundException("Post with id + " + postId + " +  not found"));

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

    private PostResponse getPostResponseFromPost(Post p) {
        log.info("getPostResponseFromPost from post: {}", p.getId());
        User user = userService.getAuthenticatedUser();

        AccountInfoResponse accountInfo = buildAccountInfo(p.getUser());
        CategoryResponse category = buildCategoryResponse(p.getCategoryType());

        List<PlaceResponse> places = fetchAndMapPlaces(p);

        List<CommentResponse> commentsContent = fetchAndMapComments(p);

        return PostResponse.builder()
                .id(p.getId())
                .title(p.getTitle())
                .text(p.getDescription())
                .creationDate(p.getCreatedAt())
                .hashtags(p.getHashtags().stream().map(HashTag::getTitle).collect(Collectors.toSet()))
                .accountInfo(accountInfo)
                .places(places)
                .isLiked(isPostLikedByUser(p, user))
                .isSaved(isPostSavedByUser(p, user))
                .comments(commentsContent)
                .likes(p.getLikesCount())
                .cons(p.getCons())
                .pros(p.getPros())
                .category(category)
                .build();
    }

    private ShortPostResponse getShortPostResponseFromPost(Post p) {
        log.info("getShortPostResponseFromPost from post: {}", p.getId());
        User user = userService.getAuthenticatedUser();

        AccountInfoResponse accountInfo = buildAccountInfo(p.getUser());
        CategoryResponse category = buildCategoryResponse(p.getCategoryType());

        List<PlaceResponse> places = fetchAndMapPlaces(p);

        PlaceResponse first = !places.isEmpty() ? places.getFirst() : null;

        return ShortPostResponse.builder()
                .id(p.getId())
                .creationDate(p.getCreatedAt())
                .category(category)
                .title(p.getTitle())
                .place(first)
                .rating(calculateRating(places))
                .accountInfo(accountInfo)
                .likes(p.getLikesCount())
                .isLiked(isPostLikedByUser(p, user))
                .isSaved(isPostSavedByUser(p, user))
                .build();
    }

    private Double calculateRating(List<PlaceResponse> places) {
        if (places == null || places.isEmpty()) {
            return 0.0;
        }

        double totalRating = places.stream()
                .mapToDouble(PlaceResponse::getRating)
                .sum();

        return totalRating / places.size();
    }

    private AccountInfoResponse buildAccountInfo(User user) {
        return AccountInfoResponse.builder()
                .nickname(user.getNickname())
                .imageUrl(user.getImageUrl())
                .build();
    }

    private CategoryResponse buildCategoryResponse(CategoryType categoryType) {
        return CategoryResponse.builder()
                .name(categoryType.getName())
                .imageUrl(categoryType.getImageUrl())
                .build();
    }

    private List<PlaceResponse> fetchAndMapPlaces(Post p) {
        return Optional.ofNullable(placeRepository.findAllByPost(p))
                .orElse(Collections.emptyList())
                .stream()
                .map(place -> modelMapper.map(place, PlaceResponse.class))
                .toList();
    }

    private List<CommentResponse> fetchAndMapComments(Post p) {
        Page<Comment> commentsPage = Optional.ofNullable(commentRepository.findAllByPost(p, PageRequest.of(0, 10)))
                .orElse(Page.empty());

        return Optional.of(commentsPage.getContent())
                .orElse(Collections.emptyList())
                .stream()
                .map(this::mapCommentToCommentResponse)
                .toList();
    }

    private CommentResponse mapCommentToCommentResponse(Comment comment) {
        AccountInfoResponse accountInfo = buildAccountInfo(comment.getUser());
        return CommentResponse.builder()
                .accountInfo(accountInfo)
                .text(comment.getContent())
                .creationDate(comment.getCreatedAt())
                .build();
    }

    private <T> Page<T> getResponseListFromPostList(Pageable pageRequest, Page<Post> posts, Function<Post, T> mapper) {
        log.info("getResponseListFromPostList called for {} posts", posts.getSize());
        List<T> response = posts.getContent().stream()
                .map(mapper)
                .collect(Collectors.toList());

        log.info("getResponseListFromPostList returned {} responses", response.size());
        return new PageImpl<>(response, pageRequest, posts.getTotalElements());
    }

    private Page<PostResponse> getPostResponseListFromPostList(Pageable pageRequest, Page<Post> posts) {
        return getResponseListFromPostList(pageRequest, posts, this::getPostResponseFromPost);
    }

    private Page<ShortPostResponse> getShortPostResponseListFromPostList(Pageable pageRequest, Page<Post> posts) {
        return getResponseListFromPostList(pageRequest, posts, this::getShortPostResponseFromPost);
    }


    private Page<Post> getRandomPopularPosts() {
        LocalDateTime lastWeek = LocalDateTime.now().minusWeeks(1);
        return postRepository.findPopularPosts(PageRequest.of(0, PAGE_SIZE), lastWeek);
    }

    private boolean isPostLikedByUser(Post post, User user) {
        return likeRepository.findByUserAndPost(user, post).isPresent();
    }

    private boolean isPostSavedByUser(Post post, User user) {
        return savedPostRepository.findByUserAndPost(user, post).isPresent();
    }

    private Post mapToPost(CreatePostRequest createPostRequest, User user) {
        return Post.builder()
                .title(createPostRequest.getTitle())
                .description(createPostRequest.getDescription())
                .user(user)
                .createdAt(LocalDateTime.now())
                .pros(createPostRequest.getPros().toArray(new String[0]))
                .cons(createPostRequest.getCons().toArray(new String[0]))
                .hashtags(createHashTags(createPostRequest.getHashtags()))
                .categoryType(categoryTypeRepository.findByName(createPostRequest.getCategoryName())
                        .orElseThrow(() -> new CategoryTypeNotFoundException("CategoryType is not found!")))
                .build();
    }

    private Place mapToPlace(PlaceRequest placeRequest, Post post) {
        return Place.builder()
                .description(placeRequest.getDescription())
                .longitude(placeRequest.getLongitude())
                .latitude(placeRequest.getLatitude())
                .rating(placeRequest.getRating())
                .imgUrl(placeRequest.getImageUrl())
                .post(post)
                .build();
    }
}

