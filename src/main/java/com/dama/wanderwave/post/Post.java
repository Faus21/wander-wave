package com.dama.wanderwave.post;

import com.dama.wanderwave.categoryType.CategoryType;
import com.dama.wanderwave.comment.Comment;
import com.dama.wanderwave.report.post.PostReport;
import com.dama.wanderwave.user.User;
import com.dama.wanderwave.hashtag.HashTag;
import com.dama.wanderwave.user.like.Like;
import com.dama.wanderwave.user.saved_post.SavedPost;
import com.dama.wanderwave.user.viewed_post.ViewedPost;
import com.vladmihalcea.hibernate.type.array.StringArrayType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "posts" )
@Builder
public class Post {

	@Id
	@GeneratedValue(generator = "hash_generator")
	@GenericGenerator(name = "hash_generator", type = com.dama.wanderwave.hash.HashUUIDGenerator.class)
	@Column(name = "post_id", nullable = false, updatable = false, unique = true)
	private String id;

	@NotBlank(message = "Title cannot be blank")
	@Size(max = 100, message = "Title length must be less than or equal to 100 characters")
	@Column(nullable = false, length = 100)
	private String title;

	@CreatedDate
	@Column(name = "created_at", updatable = false, nullable = false)
	private LocalDateTime createdAt;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "user_id", nullable = false, referencedColumnName = "user_id", foreignKey = @ForeignKey(name = "fk_post_user"))
	private User user;

	@Size(max = 2048, message = "Description length must be less than or equal to 2048 characters")
	@Column(columnDefinition = "TEXT")
	private String description;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "category_type_id", nullable = false, referencedColumnName = "category_type_id", foreignKey = @ForeignKey(name = "fk_post_category_type"))
	private CategoryType categoryType;

	@Type(StringArrayType.class)
	@Column(name = "pros", columnDefinition = "TEXT[]")
	private String[] pros;

	@Type(StringArrayType.class)
	@Column(name = "cons", columnDefinition = "TEXT[]")
	private String[] cons;

	@ManyToMany
	@JoinTable(
			name = "post_hashtags",
			joinColumns = @JoinColumn(name = "post_id"),
			inverseJoinColumns = @JoinColumn(name = "hashtag_id")
	)
	private Set<HashTag> hashtags;

	@OneToMany(mappedBy = "post", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	private Set<ViewedPost> viewedPosts = new HashSet<>();

	@OneToMany(mappedBy = "post", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	private Set<SavedPost> savedPosts = new HashSet<>();

	@Min(value = 0, message = "Likes count must be non-negative")
	@Column(name = "post_likes")
	@Builder.Default
	private Integer likesCount = 0;

	@OneToMany(mappedBy = "post", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	private Set<Like> likes = new HashSet<>();

	@OneToMany(mappedBy = "post", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	private Set<PostReport> reports;

	@OneToMany(mappedBy = "post", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	private List<Comment> comments;
}
