package com.dama.wanderwave.user;

import com.dama.wanderwave.report.general.UserReport;
import com.dama.wanderwave.role.Role;
import com.dama.wanderwave.emailToken.EmailToken;
import com.dama.wanderwave.user.like.Like;
import com.dama.wanderwave.user.saved_post.SavedPost;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.Principal;
import java.util.*;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Table(name = "users")
public class User implements UserDetails, Principal {

	@Id
	@GeneratedValue(generator = "hash_generator")
	@GenericGenerator(name = "hash_generator", type = com.dama.wanderwave.hash.HashUUIDGenerator.class)
	@Column(name = "user_id", nullable = false)
	private String id;

	@Size(max = 50, message = "Username length must be less than or equal to 50 characters")
	@NotBlank(message = "Username cannot be blank")
	@Column(unique = true, nullable = false, length = 50)
	private String nickname;

	@Email(message = "Invalid email format")
	@NotBlank(message = "Email cannot be blank")
	@Column(unique = true, nullable = false)
	private String email;

	@Size(max = 100, message = "Password length must be less than or equal to 100 characters")
	@NotBlank(message = "Password cannot be blank")
	@Column(nullable = false, length = 100)
	private String password;

	@Size(max = 255, message = "Profile description length must be less than or equal to 255 characters")
	@Column(nullable = false)
	private String description;

	@Type(JsonBinaryType.class)
	@Column(name = "black_list", columnDefinition = "jsonb")
	private BlackList blackList;
	@Builder.Default
	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
	private Set<Role> roles = new HashSet<>();
	@Builder.Default
	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "subscribers", joinColumns = @JoinColumn(name = "follower_id"))
	@Column(name = "followed_id")
	private Set<String> subscriptions = new HashSet<>();
	@Builder.Default
	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "subscribers", joinColumns = @JoinColumn(name = "followed_id"))
	@Column(name = "follower_id")
	private Set<String> subscribers = new HashSet<>();

	@Min(0)
	@Column(name = "subscriber_count")
	private int subscriberCount;
	@Min(0)
	@Column(name = "subscriptions_count")
	private int subscriptionsCount;

	@Builder.Default
	@OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<EmailToken> emailTokens = new HashSet<>();

	@Column(name = "account_locked", nullable = false)
	private boolean accountLocked;

	@Column(nullable = false)
	private boolean enabled;

	@Column(name = "image_url", columnDefinition = "TEXT")
	private String imageUrl;
	@Builder.Default
	@OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	private List<SavedPost> savedPosts = new ArrayList<>();
	@Builder.Default
	@OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	private List<Like> likes = new ArrayList<>();
	@Builder.Default
	@OneToMany(mappedBy = "sender", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	private Set<UserReport> sentReports = new HashSet<>();
	@Builder.Default
	@OneToMany(mappedBy = "reported", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	private Set<UserReport> receivedReports = new HashSet<>();

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return this.roles.stream().map(r -> new SimpleGrantedAuthority(r.getName())).toList();
	}

	@Override
	public String getUsername() {
		return email;
	}


	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return !accountLocked;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public String getName() {
		return email;
	}
}
