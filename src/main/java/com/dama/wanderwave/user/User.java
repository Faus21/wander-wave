package com.dama.wanderwave.user;

import com.dama.wanderwave.role.Role;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users",
		uniqueConstraints = {
				@UniqueConstraint(columnNames = {"user_id"}),
				@UniqueConstraint(columnNames = {"username"}),
				@UniqueConstraint(columnNames = {"email"})
		})
public class User {

	@Id
	@GeneratedValue(generator = "hash_generator")
	@GenericGenerator(name = "hash_generator", strategy = "com.dama.wanderwave.hash.HashUUIDGenerator")
	@Column(name = "user_id", nullable = false, updatable = false)
	private String id;

	@Size(max = 50, message = "Username length must be less than or equal to 50 characters")
	@NotBlank(message = "Username cannot be blank")
	@Column(unique = true, nullable = false, length = 50)
	private String username;

	@Email(message = "Invalid email format")
	@NotBlank(message = "Email cannot be blank")
	@Column(unique = true, nullable = false)
	private String email;

	@Size(max = 100, message = "Password length must be less than or equal to 100 characters")
	@NotBlank(message = "Password cannot be blank")
	@Column(nullable = false, length = 100)
	private String password;

	@Size(max = 255, message = "Profile description length must be less than or equal to 255 characters")
	@NotBlank(message = "Profile description cannot be blank")
	@Column(nullable = false)
	private String description;

	@Size(max = 300, message = "Access token length must be less than or equal to 300 characters")
	@Column(name = "access_token")
	private String accessToken;

	@Size(max = 300, message = "Refresh token length must be less than or equal to 300 characters")
	@Column(name = "refresh_token")
	private String refreshToken;

	@Type(JsonBinaryType.class)
	@Column(name = "black_list", columnDefinition = "jsonb")
	private BlackList blackList;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "user_roles",
			joinColumns = @JoinColumn(name = "user_id"),
			inverseJoinColumns = @JoinColumn(name = "role_id"))
	private Set<Role> roles = new HashSet<>();

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "subscribers", joinColumns = @JoinColumn(name = "follower_id"))
	@Column(name = "followed_id")
	private Set<String> subscriptions = new HashSet<>();

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "subscribers", joinColumns = @JoinColumn(name = "followed_id"))
	@Column(name = "follower_id")
	private Set<String> subscribers = new HashSet<>();


	@Column(name = "image_url", columnDefinition = "TEXT")
	private String imageUrl;
}
