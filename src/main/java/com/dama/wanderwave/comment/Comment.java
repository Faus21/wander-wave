package com.dama.wanderwave.comment;

import com.dama.wanderwave.post.Post;
import com.dama.wanderwave.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "comments")
public class Comment {

	@Id
	@GeneratedValue(generator = "hash_generator")
	@GenericGenerator(name = "hash_generator", strategy = "com.dama.wanderwave.hash.HashUUIDGenerator")
	@Column(name = "comment_id", nullable = false, updatable = false, unique = true)
	private String id;

	@Size(max = 255, message = "Content length must be less than or equal to 255 characters")
	@Column(nullable = false)
	private String content;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false, nullable = false)
	private LocalDateTime createdAt;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "user_id",
			referencedColumnName = "user_id",
			nullable = false,
			foreignKey = @ForeignKey(name = "fk_comment_user"))
	private User user;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "post_id",
			referencedColumnName = "post_id",
			nullable = false,
			foreignKey = @ForeignKey(name = "fk_comment_post"))
	private Post post;
}
