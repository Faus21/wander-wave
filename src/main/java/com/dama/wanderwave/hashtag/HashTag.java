package com.dama.wanderwave.hashtag;

import com.dama.wanderwave.post.Post;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "hashtags")
public class HashTag {

	@Id
	@GeneratedValue(generator = "hash_generator")
	@GenericGenerator(name = "hash_generator", type = com.dama.wanderwave.hash.HashUUIDGenerator.class)
	@Column(name = "hashtag_id", nullable = false, updatable = false)
	private String id;

	@Size(max = 50, message = "Title length must be less than or equal to 50 characters")
	@Column(nullable = false, unique = true, length = 50)
	private String title;

	@ManyToMany(mappedBy = "hashtags")
	private Set<Post> posts;
}
