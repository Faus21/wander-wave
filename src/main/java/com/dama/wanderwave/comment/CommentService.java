package com.dama.wanderwave.comment;

import com.dama.wanderwave.handler.comment.CommentNotFoundException;
import com.dama.wanderwave.handler.post.PostNotFoundException;
import com.dama.wanderwave.post.Post;
import com.dama.wanderwave.post.PostRepository;
import com.dama.wanderwave.post.request.CreateCommentRequest;
import com.dama.wanderwave.user.User;
import com.dama.wanderwave.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserService userService;

    @Transactional
    public Comment createComment(CreateCommentRequest createCommentRequest) {
        log.info("Creating a new comment for postId: {}", createCommentRequest.getPostId());

        Post post = findPostById(createCommentRequest.getPostId());

        User user = userService.getAuthenticatedUser();
        log.debug("Authenticated user: {}", user.getId());

        String content = createCommentRequest.getContent();
        log.debug("Comment content: {}", content);

        Comment comment = Comment.builder()
                .content(content)
                .user(user)
                .post(post)
                .build();

        Comment savedComment = commentRepository.save(comment);
        log.info("Comment created successfully with id: {}", savedComment.getId());

        return savedComment;
    }

    public List<Comment> getAllCommentsForPost(Pageable pageable, String postId) {
        log.info("Fetching all comments for post {} with page number: {}, page size: {}",
                postId, pageable.getPageNumber(), pageable.getPageSize());

        Post post = findPostById(postId);

        Page<Comment> commentsPage = commentRepository.findAllByPost(post, pageable);

        log.info("Retrieved {} comments on page {}, total elements: {}",
                commentsPage.getContent().size(),
                commentsPage.getNumber(),
                commentsPage.getTotalElements());

        return commentsPage.getContent();
    }

    @Transactional
    public Comment updateComment(String id, String content) {
        log.info("Updating comment with id: {}", id);

        return commentRepository.findById(id)
                .map(comment -> {
                    log.debug("Updating comment content to: {}", content);
                    comment.setContent(content);
                    Comment updatedComment = commentRepository.save(comment);
                    log.info("Comment updated successfully with id: {}", updatedComment.getId());
                    return updatedComment;
                })
                .orElseThrow(() -> {
                    log.error("Comment not found with id: {}", id);
                    return new RuntimeException("Comment not found with id: " + id);
                });
    }

    @Transactional
    public String deleteComment(String id) {
        log.info("Deleting comment with id: {}", id);

        if (!commentRepository.existsById(id)) {
            log.error("Comment with id {} not found", id);
            throw new CommentNotFoundException("Comment with id " + id + " not found");
        }

        commentRepository.deleteById(id);
        log.info("Comment deleted successfully with id: {}", id);

        return "Comment deleted successfully";
    }

    private Post findPostById(String postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> {
                    log.error("Post with id {} not found", postId);
                    return new PostNotFoundException("Post with id " + postId + " not found");
                });
    }
}