package com.dama.wanderwave.user.viewed_post;

import com.dama.wanderwave.post.Post;
import com.dama.wanderwave.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_viewed_posts")
public class ViewedPost {

	@EmbeddedId
	private ViewedPostId id;

	@MapsId("user_id")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", referencedColumnName = "user_id")
	@NotNull(message = "User must not be null")
	private User user;

	@MapsId("post_id")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "post_id", referencedColumnName = "post_id")
	@NotNull(message = "Post must not be null")
	private Post post;

	@CreatedDate
	@Column(name = "created_at", updatable = false, nullable = false)
	private LocalDateTime createdAt;
}
