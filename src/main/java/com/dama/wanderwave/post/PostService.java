package com.dama.wanderwave.post;

import com.dama.wanderwave.handler.post.CategoryTypeNotFoundException;
import com.dama.wanderwave.handler.post.PostNotFoundException;
import com.dama.wanderwave.handler.user.UserNotFoundException;
import com.dama.wanderwave.handler.user.like.IsLikedException;
import com.dama.wanderwave.handler.user.like.LikeNotFoundException;
import com.dama.wanderwave.handler.user.save.IsSavedException;
import com.dama.wanderwave.handler.user.save.SavedPostNotFound;
import com.dama.wanderwave.hashtag.HashTag;
import com.dama.wanderwave.hashtag.HashTagRepository;
import com.dama.wanderwave.post.categoryType.CategoryTypeRepository;
import com.dama.wanderwave.post.request.CreatePostRequest;
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

    public List<PostResponse> getUserPosts(String nickname){
        User user = userRepository.findByNickname(nickname)
                .orElseThrow(() -> new UserNotFoundException(nickname));

        log.info("Successfully fetched posts for user '{}'.", nickname);

        List<Post> result = postRepository.findByUserWithHashtags(user);
        var postResponses = new ArrayList<PostResponse>();
        for (Post post : result) {
            PostResponse postResponse = new PostResponse();
            postResponse.setId(post.getId());
            postResponse.setTitle(post.getTitle());
            postResponse.setCategoryType(post.getCategoryType().getName());
            postResponse.setDescription(post.getDescription());
            postResponse.setCreatedAt(post.getCreatedAt());
            postResponse.setCons(post.getCons());
            postResponse.setPros(post.getPros());
            postResponse.setNickname(post.getUser().getNickname());
            postResponse.setHashtags(post.getHashtags().stream().map(HashTag::getTitle).collect(Collectors.toSet()));

            postResponses.add(postResponse);
        }

        return postResponses;
    }


    @Transactional
    public String createPost(CreatePostRequest createPostRequest){
        User user = userService.getAuthenticatedUser();

        Post post = new Post();
        post.setTitle(createPostRequest.getTitle());
        post.setDescription(createPostRequest.getDescription());
        post.setUser(user);
        post.setCreatedAt(LocalDateTime.now());
        post.setPros(createPostRequest.getPros().toArray(new String[0]));
        post.setCons(createPostRequest.getCons().toArray(new String[0]));

        Set<HashTag> hashTags = new HashSet<>();
        for (String hashtag : createPostRequest.getHashtags()){
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

        return "Post is created successfully!";
    }

    @Transactional
    public String likePost(String postId){
        User user = userService.getAuthenticatedUser();

        Post post = postRepository.findByIdWithLikes(postId)
                .orElseThrow(() -> new PostNotFoundException("Post with id " + postId + " is not found!"));

        if (post.getLikes().stream().anyMatch((e) -> e.getUser().getId().equals(user.getId())))
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

        return "Liked successfully!";
    }

    @Transactional
    public String unlikePost(String postId){
        User user = userService.getAuthenticatedUser();

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post with id " + postId + " is not found!"));

        Like like = likeRepository.findByUserAndPost(user, post)
                        .orElseThrow(() -> new LikeNotFoundException("This post isn't liked by user!"));

        likeRepository.delete(like);

        return "Post is unliked successfully!";
    }

    public Integer getPostLikesCount(String postId){
        Post post = postRepository.findByIdWithLikes(postId)
                .orElseThrow(() -> new PostNotFoundException("Post with id " + postId + " is not found!"));

        return post.getLikes().size();
    }

    @Transactional
    public String savePost(String postId){
        User user = userService.getAuthenticatedUser();

        Post post = postRepository.findByIdSaved(postId)
                .orElseThrow(() -> new PostNotFoundException("Post with id " + postId + " is not found!"));

        if (post.getSavedPosts().stream().anyMatch((e) -> e.getUser().getId().equals(user.getId())))
            throw new IsSavedException("Post is already saved by user!");

        SavedPostId savedPostId = new SavedPostId();
        savedPostId.setUser_id(user.getId());
        savedPostId.setPost_id(post.getId());

        SavedPost savedPost = new SavedPost();
        savedPost.setId(savedPostId);
        savedPost.setUser(user);
        savedPost.setPost(post);
        savedPost.setCreatedAt(LocalDateTime.now());

        savedPostRepository.save(savedPost);

        return "Saved successfully!";
    }

    @Transactional
    public String unsavePost(String postId){
        User user = userService.getAuthenticatedUser();

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post with id " + postId + " is not found!"));

        SavedPost savedPost = savedPostRepository.findByUserAndPost(user, post)
                .orElseThrow(() -> new SavedPostNotFound("This post isn't saved by user!"));

        savedPostRepository.delete(savedPost);

        return "Post is unsaved successfully!";
    }

    @Transactional
    public List<PostResponse> personalFlow(){
        User user = userService.getAuthenticatedUser();

        List<PostResponse> response = new ArrayList<>();

        for (String us : user.getSubscriptions()){
            User subscription = userRepository.findById(us)
                    .orElseThrow(() -> new UserNotFoundException("User is not found!"));

            List<Post> posts = postRepository.findByUserWithHashtags(subscription);
            response.addAll(getPostResponseListFromPostList(posts));
        }

        return response.stream().sorted(Comparator.comparing(PostResponse::getCreatedAt)).toList();
    }

    @Transactional
    public List<PostResponse> recommendationFlow(){
        User user = userService.getAuthenticatedUser();

        List<PostResponse> response = new ArrayList<>();
        List<Post> likedPosts = getLikedPosts(user);
        List<Post> savedPosts = getSavedPosts(user);

        List<HashTag> hashtags = new ArrayList<>();

        int maxLikesIndex = Math.min(likedPosts.size(), PAGE_SIZE);
        for (Post post : likedPosts.subList(0, maxLikesIndex)){
            hashtags.addAll(post.getHashtags());
        }

        int maxSavedIndex = Math.min(savedPosts.size(), PAGE_SIZE);
        for (Post post : savedPosts.subList(0, maxSavedIndex)){
            hashtags.addAll(post.getHashtags());
        }

        Pageable pageable = getPageSize();

        List<Post> posts = new ArrayList<>();
        for (HashTag h : hashtags){
            List<Post> resp = postRepository.findByHashtag(h.getId(), pageable).getContent();
            posts.addAll(resp);
        }

        List<Post> mostPopularPosts = postRepository.findMostPopularPostsByLikes(getPageSize()).getContent();
        response.addAll(getPostResponseListFromPostList(posts));
        response.addAll(getPostResponseListFromPostList(mostPopularPosts));
        return response;
    }

    @Transactional
    public List<PostResponse> getLikedPostsResponse(){
        User user = userService.getAuthenticatedUser();

        return getPostResponseListFromPostList(getLikedPosts(user));
    }

    @Transactional
    public List<PostResponse> getSavedPostsResponse(){
        User user = userService.getAuthenticatedUser();

        return getPostResponseListFromPostList(getSavedPosts(user));
    }

    @Transactional
    public List<PostResponse> getPostsByCategory(String category){
        List<Post> posts = postRepository.findByCategory(category, getPageSize()).getContent();
        return getPostResponseListFromPostList(posts);
    }

    public String deletePost(String postId){
        User user = userService.getAuthenticatedUser();
        Post post = postRepository.findById(postId).orElseThrow(() -> new PostNotFoundException(postId));

        if (!post.getUser().getId().equals(user.getId())){
            return "You are not allowed to delete this post!";
        }

        postRepository.delete(post);
        return "Deleted successfully!";
    }

    private List<Post> getLikedPosts(User user){
        return postRepository.findByUserWithLikes(user);
    }

    private List<Post> getSavedPosts(User user){
        return postRepository.findByUserSaved(user);
    }

    private Pageable getPageSize(){
        return PageRequest.of(0, PAGE_SIZE);
    }

    private List<PostResponse> getPostResponseListFromPostList(List<Post> posts){
        List<PostResponse> response = new ArrayList<>();
        for (Post p : posts){
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
            response.add(postResponse);
        }

        return response;
    }
}
