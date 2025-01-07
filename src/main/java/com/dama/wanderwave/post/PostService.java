package com.dama.wanderwave.post;

import com.dama.wanderwave.azure.AzureService;
import com.dama.wanderwave.categoryType.CategoryType;
import com.dama.wanderwave.categoryType.CategoryTypeRepository;
import com.dama.wanderwave.comment.Comment;
import com.dama.wanderwave.comment.CommentRepository;
import com.dama.wanderwave.handler.post.CategoryTypeNotFoundException;
import com.dama.wanderwave.handler.post.PostNotFoundException;
import com.dama.wanderwave.handler.user.UnauthorizedActionException;
import com.dama.wanderwave.handler.user.UserNotFoundException;
import com.dama.wanderwave.handler.user.like.IsLikedException;
import com.dama.wanderwave.handler.user.like.LikeNotFoundException;
import com.dama.wanderwave.handler.user.save.IsSavedException;
import com.dama.wanderwave.handler.user.save.SavedPostNotFound;
import com.dama.wanderwave.hashtag.HashTag;
import com.dama.wanderwave.hashtag.HashTagRepository;
import com.dama.wanderwave.place.Place;
import com.dama.wanderwave.place.PlaceRepository;
import com.dama.wanderwave.place.request.PlaceRequest;
import com.dama.wanderwave.post.request.PostRequest;
import com.dama.wanderwave.post.response.*;
import com.dama.wanderwave.route.Route;
import com.dama.wanderwave.route.RouteRepository;
import com.dama.wanderwave.user.User;
import com.dama.wanderwave.user.UserRepository;
import com.dama.wanderwave.user.UserService;
import com.dama.wanderwave.user.like.Like;
import com.dama.wanderwave.user.like.LikeId;
import com.dama.wanderwave.user.like.LikeRepository;
import com.dama.wanderwave.user.saved_post.SavedPost;
import com.dama.wanderwave.user.saved_post.SavedPostId;
import com.dama.wanderwave.user.saved_post.SavedPostRepository;
import com.github.benmanes.caffeine.cache.Cache;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    private final AzureService azureService;
    private final RouteRepository routeRepository;

    private final Cache<String, Set<String>> userRecommendedPostsCache;

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
    public String modifyPost(PostRequest request) {
        log.info("modifyPost called with request: {}", request);
        User user = userService.getAuthenticatedUser();

        String postId = request.getId();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post not found with id: " + postId));

        if (!post.getUser().equals(user)) {
            throw new UnauthorizedActionException("User not authorized to perform this action");
        }

        updatePostFields(request, post);

        postRepository.save(post);

        return "Post modified successfully";
    }

    private void updatePostFields(PostRequest request, Post post) {
        if (request.getCategory() != null) {
            CategoryType categoryType = getCategoryType(request.getCategory());
            post.setCategoryType(categoryType);
        }

        if (request.getTitle() != null) {
            post.setTitle(request.getTitle());
        }

        if (request.getText() != null) {
            post.setDescription(request.getText());
        }

        if (request.getIsDisabledComments() != null) {
            post.setIsDisabledComments(request.getIsDisabledComments());
        }

        if (request.getHashtags() != null && !request.getHashtags().isEmpty()) {
            Set<HashTag> hashtagSet = processHashtags(request.getHashtags(), post);
            post.setHashtags(hashtagSet);
        }

        if (request.getPlaces() != null && !request.getPlaces().isEmpty()) {
            List<Place> placeList = processPlaces(request.getPlaces(), post);
            placeRepository.saveAll(placeList);
        }

        if (request.getRoute() != null) {
            post.setRoute(Route.fromRouteRequest(request.getRoute()));
        }

        if (request.getPros() != null && !request.getPros().isEmpty()) {
            post.setPros(request.getPros().toArray(new String[0]));
        }

        if (request.getCons() != null && !request.getCons().isEmpty()) {
            post.setCons(request.getCons().toArray(new String[0]));
        }
    }

    private CategoryType getCategoryType(String categoryName) {
        return categoryTypeRepository.findByName(categoryName)
                .orElseThrow(() -> new CategoryTypeNotFoundException("Category type not found with name: " + categoryName));
    }

    private Set<HashTag> processHashtags(Set<String> hashtags, Post post) {
        return Optional.ofNullable(hashtags)
                .filter(h -> !h.isEmpty())
                .orElse(Collections.emptySet())
                .stream()
                .map(h -> hashTagRepository.findByTitle(h)
                        .orElseGet(() -> {
                            HashTag newHashtag = HashTag.builder()
                                    .title(h)
                                    .posts(new HashSet<>())
                                    .build();
                            hashTagRepository.save(newHashtag);
                            return newHashtag;
                        }))
                .peek(h -> h.getPosts().add(post))
                .collect(Collectors.toSet());
    }

    public String[] uploadImages(String postId, List<MultipartFile> images, long timestamp) {
        log.info("Starting image upload for postId: {}", postId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post not found with id: " + postId));

        String[] uploadedImageUrls = Optional.ofNullable(images)
                .filter(i -> !i.isEmpty())
                .orElse(Collections.emptyList())
                .parallelStream()
                .map(image -> uploadImage(image, timestamp))
                .filter(Objects::nonNull)
                .toArray(String[]::new);

        post.setImages(uploadedImageUrls);
        postRepository.save(post);

        log.info("Successfully uploaded {} images for postId: {}", uploadedImageUrls.length, postId);
        return uploadedImageUrls;
    }

    private List<Place> processPlaces(List<PlaceRequest> places, Post post) {
        return Optional.ofNullable(places)
                .filter(p -> !p.isEmpty())
                .orElse(Collections.emptyList())
                .stream()
                .map(placeRequest -> Place.fromPlaceRequest(placeRequest, post))
                .collect(Collectors.toList());
    }

    @Transactional
    public String createPost(PostRequest createPostRequest) {
        log.info("createPost called with request: {}", createPostRequest);
        User user = userService.getAuthenticatedUser();

        Route route = Route.fromRouteRequest(createPostRequest.getRoute());
        routeRepository.save(route);

        Post post = mapToPost(createPostRequest, user, route);

        Post saved = postRepository.save(post);

        createPostRequest.getPlaces().stream()
                .map(placeRequest -> Place.fromPlaceRequest(placeRequest, post))
                .forEach(placeRepository::save);

        log.info("createPost successfully created post with title: {}", createPostRequest.getTitle());
        return saved.getId();
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

    public Set<ShortPostResponse> recommendationFlow(Pageable pageRequest) {
        log.info("recommendationFlow called");
        User user = userService.getAuthenticatedUser();

        Set<String> recommendedPostIds = userRecommendedPostsCache.get(user.getId(), k -> new HashSet<>());

        List<Post> combinedPosts = Stream.of(
                        getLikedPosts(pageRequest, user).getContent(),
                        getSavedPosts(pageRequest, user).getContent())
                .flatMap(List::stream)
                .filter(post -> !recommendedPostIds.contains(post.getId()))
                .toList();

        Set<HashTag> uniqueHashtags = combinedPosts.stream()
                .flatMap(post -> post.getHashtags().stream())
                .collect(Collectors.toSet());

        List<Post> hashtagPosts = uniqueHashtags.isEmpty() ? List.of() :
                uniqueHashtags.stream()
                        .flatMap(h -> postRepository.findByHashtagsContaining(h, pageRequest).getContent().stream())
                        .filter(post -> !recommendedPostIds.contains(post.getId()))
                        .toList();

        List<Post> popularPosts = getRandomPopularPosts().getContent().stream()
                .filter(post -> !recommendedPostIds.contains(post.getId()))
                .toList();

        List<Post> allPosts = Stream.concat(hashtagPosts.stream(), popularPosts.stream())
                .collect(Collectors.toList());
        Collections.shuffle(allPosts);

        List<ShortPostResponse> response = getShortPostResponseListFromPostList(
                pageRequest, new PageImpl<>(allPosts, pageRequest, allPosts.size())
        ).getContent();

        response = response.size() > 5 ? response.subList(0, 5) : response;

        response.forEach(post -> recommendedPostIds.add(post.getId()));

        userRecommendedPostsCache.put(user.getId(), recommendedPostIds);

        LinkedHashSet<ShortPostResponse> finalResult = new LinkedHashSet<>(response);

        log.info("recommendationFlow returned {} posts", finalResult.size());

        return finalResult;
    }

    public Page<ShortPostResponse> getLikedPostsResponse(Pageable pageRequest) {
        log.info("getLikedPostsResponse called");
        User user = userService.getAuthenticatedUser();

        Page<ShortPostResponse> response = getShortPostResponseListFromPostList(pageRequest, getLikedPosts(pageRequest, user));
        log.info("getLikedPostsResponse returned {} posts", response.getSize());
        return response;
    }


    public Page<ShortPostResponse> getSavedPostsResponse(Pageable pageRequest) {
        log.info("getSavedPostsResponse called");
        User user = userService.getAuthenticatedUser();

        Page<ShortPostResponse> response = getShortPostResponseListFromPostList(pageRequest, getSavedPosts(pageRequest, user));
        log.info("getSavedPostsResponse returned {} posts", response.getSize());
        return response;
    }


    public Page<ShortPostResponse> getPostsByCategory(Pageable pageRequest, String category) {
        log.info("getPostsByCategory called with category: {}", category);
        Page<Post> posts = postRepository.findByCategory(category, pageRequest);

        Page<ShortPostResponse> response = getShortPostResponseListFromPostList(pageRequest, posts);
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
        Post p = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post with id " + postId + " not found"));

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

        return PostResponse.builder()
                .id(p.getId())
                .title(p.getTitle())
                .text(p.getDescription())
                .creationDate(p.getCreatedAt())
                .hashtags(p.getHashtags().stream().map(HashTag::getTitle).collect(Collectors.toSet()))
                .accountInfo(accountInfo)
                .places(places)
                .route(p.getRoute())
                .isLiked(isPostLikedByUser(p, user))
                .isSaved(isPostSavedByUser(p, user))
                .comments(p.getCommentsCount())
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

        List<Place> places = fetchPlaces(p);

        Place first = !places.isEmpty() ? places.getFirst() : null;
        ShortPlaceResponse shortPlaceResponse = new ShortPlaceResponse();
        if (first != null) {
            shortPlaceResponse = ShortPlaceResponse
                    .builder()
                    .displayName(first.getDisplayName())
                    .rating(first.getRating())
                    .build();
        }
        String image = "";
        if (p.getImages() != null) {
            image = p.getImages().length > 0 ? p.getImages()[0] : null;
        }

        return ShortPostResponse.builder()
                .id(p.getId())
                .creationDate(p.getCreatedAt())
                .category(category)
                .title(p.getTitle())
                .place(shortPlaceResponse)
                .rating(calculateRating(places))
                .accountInfo(accountInfo)
                .likes(p.getLikesCount())
                .previewImage(image)
                .commentsCount(p.getCommentsCount())
                .isLiked(isPostLikedByUser(p, user))
                .isSaved(isPostSavedByUser(p, user))
                .build();
    }

    private Double calculateRating(List<Place> places) {
        if (places == null || places.isEmpty()) {
            return 0.0;
        }

        double totalRating = places.stream()
                .mapToDouble(Place::getRating)
                .sum();

        return totalRating / places.size();
    }

    private AccountInfoResponse buildAccountInfo(User user) {
        return AccountInfoResponse.builder()
                .nickname(user.getNickname())
                .imageUrl(user.getImageUrl())
                .id(user.getId())
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

    private List<Place> fetchPlaces(Post p) {
        return Optional.ofNullable(placeRepository.findAllByPost(p))
                .orElse(Collections.emptyList())
                .stream()
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
                .id(comment.getId())
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

    private Post mapToPost(PostRequest postRequest, User author, Route route) {
        return Post.builder()
                .title(postRequest.getTitle())
                .description(postRequest.getText())
                .user(author)
                .route(route)
                .isDisabledComments(postRequest.getIsDisabledComments())
                .createdAt(LocalDateTime.now())
                .pros(postRequest.getPros().toArray(new String[0]))
                .cons(postRequest.getCons().toArray(new String[0]))
                .hashtags(createHashTags(postRequest.getHashtags()))
                .categoryType(getCategoryType(postRequest.getCategory()))
                .build();
    }

    private String uploadImage(MultipartFile file, long timestamp) {
        try {
            String fileName = timestamp + "-" + file.getOriginalFilename();
            return azureService.uploadAvatar(
                    "posts",
                    fileName,
                    file.getBytes(),
                    file.getContentType(),
                    file.getSize()
            );
        } catch (IOException e) {
            log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
            return null;
        }
    }
}

