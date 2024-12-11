package com.dama.wanderwave.post;

import com.dama.wanderwave.post.request.CreatePostRequest;
import com.dama.wanderwave.post.request.PostRequest;
import com.dama.wanderwave.post.response.PostResponse;
import com.dama.wanderwave.utils.ResponseRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Tag(name = "Post", description = "Endpoints for post")
@Validated
public class PostController {
    private final PostService postService;
    private static final int MAX_PAGE_SIZE = 50;

    @GetMapping("/user/{nickname}")
    @Operation(summary = "Get user posts", description = "Get all posts for a specific user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User posts are retrieved successfully", content = @Content()),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content()),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content())
    })
    public ResponseEntity<ResponseRecord> getUserPosts(@RequestParam int pageNumber,
                                                       @RequestParam @Max(MAX_PAGE_SIZE) Integer pageSize,
                                                       @PathVariable String nickname) {
        Pageable page = PageRequest.of(pageNumber, pageSize);
        Page<PostResponse> posts = postService.getUserPosts(page, nickname);
        return ResponseEntity.ok().body(new ResponseRecord(HttpStatus.OK.value(), posts));
    }

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Create post", description = "Create post for user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Post is created successfully", content = @Content()),
            @ApiResponse(responseCode = "400", description = "User is mandatory!", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Request parameters not found", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content())
    })
    public ResponseEntity<ResponseRecord> createPost(
            @RequestBody @Valid CreatePostRequest request
    ){
        String response = postService.createPost(request);
        return ResponseEntity.ok().body(new ResponseRecord(HttpStatus.OK.value(), response));
    }

    @PutMapping("/{postId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Modify post", description = "Create post for user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Post modified successfully", content = @Content()),
            @ApiResponse(responseCode = "400", description = "User is mandatory!", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Request parameters not found", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content())
    })
    public ResponseEntity<ResponseRecord> modifyPost(
            @PathVariable String postId,
            @RequestBody @Valid PostRequest request
    ){
        String response = postService.modifyPost(postId, request);
        return ResponseEntity.ok().body(new ResponseRecord(HttpStatus.OK.value(), response));
    }

    @GetMapping("/{postId}/like")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get likes", description = "Get all likes for post")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Likes is retrieved successfully", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Post not found", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content())
    })
    public ResponseEntity<ResponseRecord> getPostLikes(@PathVariable String postId){
        Integer response = postService.getPostLikesCount(postId);
        return ResponseEntity.ok().body(new ResponseRecord(HttpStatus.OK.value(), response));
    }

    @GetMapping("/{postId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get likes", description = "Get all likes for post")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Likes is retrieved successfully", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Post not found", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content())
    })
    public ResponseEntity<ResponseRecord> getPost(@PathVariable String postId){
        PostResponse response = postService.getPostById(postId);
        return ResponseEntity.ok().body(new ResponseRecord(HttpStatus.OK.value(), response));
    }



    @PostMapping("/{postId}/like")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Like post", description = "Create like for post")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Like is created successfully", content = @Content()),
            @ApiResponse(responseCode = "400", description = "Post is liked already!", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Like not found", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content())
    })
    public ResponseEntity<ResponseRecord> likePost(@PathVariable String postId){
        String response = postService.likePost(postId);
        return ResponseEntity.ok().body(new ResponseRecord(HttpStatus.OK.value(), response));
    }

    @DeleteMapping("/{postId}/like")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Unlike post", description = "Delete like for post")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Like is deleted successfully", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Post not found", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Like not found", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content())
    })
    public ResponseEntity<ResponseRecord> unlikePost(@PathVariable String postId){
        String response = postService.unlikePost(postId);
        return ResponseEntity.ok().body(new ResponseRecord(HttpStatus.OK.value(), response));
    }

    @PostMapping("/{postId}/save")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Save post", description = "Save post")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Saved successfully", content = @Content()),
            @ApiResponse(responseCode = "400", description = "Post is saved already!", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Post not found", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content())
    })
    public ResponseEntity<ResponseRecord> savePost(@PathVariable String postId){
        String response = postService.savePost(postId);
        return ResponseEntity.ok().body(new ResponseRecord(HttpStatus.OK.value(), response));
    }

    @DeleteMapping("/{postId}/save")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Unsave post", description = "Delete saved for post")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Post unsaved successfully", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Post not found", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Saved post not found", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content())
    })
    public ResponseEntity<ResponseRecord> unsavePost(@PathVariable String postId){
        String response = postService.unsavePost(postId);
        return ResponseEntity.ok().body(new ResponseRecord(HttpStatus.OK.value(), response));
    }


    @GetMapping("/user/personalFlow")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "User flow", description = "Get personal user flow")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Flow is fetched successfully", content = @Content()),
            @ApiResponse(responseCode = "404", description = "User subscription is not found", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content())
    })
    public ResponseEntity<ResponseRecord> getPersonalFlow(@RequestParam int pageNumber,
                                                          @RequestParam @Max(MAX_PAGE_SIZE) Integer pageSize){
        Pageable page = PageRequest.of(pageNumber, pageSize);
        Page<PostResponse> response = postService.personalFlow(page);

        return ResponseEntity.ok().body(new ResponseRecord(HttpStatus.OK.value(), response));
    }

    @GetMapping("/user/recommendationsFlow")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "User recommendations flow", description = "Get recommendations user flow")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Flow is fetched successfully", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content())
    })
    public ResponseEntity<ResponseRecord> getRecommendationsFlow(@RequestParam int pageNumber,
                                                                 @RequestParam @Max(MAX_PAGE_SIZE) Integer pageSize){
        Pageable page = PageRequest.of(pageNumber, pageSize);
        Page<PostResponse> response = postService.recommendationFlow(page);

        return ResponseEntity.ok().body(new ResponseRecord(HttpStatus.OK.value(), response));
    }

    @GetMapping("/user/likes")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "User likes", description = "Get user likes")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Likes are fetched successfully", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content())
    })
    public ResponseEntity<ResponseRecord> getUserLikes(@RequestParam int pageNumber,
                                                       @RequestParam @Max(MAX_PAGE_SIZE) Integer pageSize){
        Pageable page = PageRequest.of(pageNumber, pageSize);
        Page<PostResponse> response = postService.getLikedPostsResponse(page);
        return ResponseEntity.ok().body(new ResponseRecord(HttpStatus.OK.value(), response));
    }

    @GetMapping("/user/saved")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "User saved posts", description = "Get user saved posts")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Saved posts are fetched successfully", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content())
    })
    public ResponseEntity<ResponseRecord> getUserSaved(@RequestParam int pageNumber,
                                                       @RequestParam @Max(MAX_PAGE_SIZE) Integer pageSize){
        Pageable page = PageRequest.of(pageNumber, pageSize);
        Page<PostResponse> response = postService.getSavedPostsResponse(page);
        return ResponseEntity.ok().body(new ResponseRecord(HttpStatus.OK.value(), response));
    }


    @GetMapping("")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Fetch saved posts by category", description = "Retrieve a list of posts saved by the user, filtered by an optional category.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Saved posts retrieved successfully", content = @Content()),
            @ApiResponse(responseCode = "500", description = "An error occurred while retrieving saved posts", content = @Content())
    })
    public ResponseEntity<ResponseRecord> getPostsByCategory(@RequestParam int pageNumber,
                                                             @RequestParam @Max(MAX_PAGE_SIZE) Integer pageSize,
                                                             @RequestParam() String category) {
        Pageable page = PageRequest.of(pageNumber, pageSize);
        Page<PostResponse> response = postService.getPostsByCategory(page, category);
        return ResponseEntity.ok().body(new ResponseRecord(HttpStatus.OK.value(), response));
    }


    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Delete a post", description = "Delete a post specified by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Post deleted successfully", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Post not found", content = @Content()),
            @ApiResponse(responseCode = "500", description = "An error occurred while deleting the post", content = @Content())
    })
    public ResponseEntity<ResponseRecord> deletePost(@PathVariable String postId) {
        String response = postService.deletePost(postId);
        return ResponseEntity.ok().body(new ResponseRecord(HttpStatus.OK.value(), response));
    }



}
