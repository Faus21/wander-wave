package com.dama.wanderwave.token;

import com.dama.wanderwave.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tokens", uniqueConstraints = {@UniqueConstraint(columnNames = {"token_id"})})
public class Token {
	@Id
	@GeneratedValue(generator = "hash_generator")
	@GenericGenerator(name = "hash_generator", strategy = "com.dama.wanderwave.hash.HashUUIDGenerator")
	@Column(name = "token_id", nullable = false, updatable = false)
	private String id;


	@Column(nullable = false)
	private String content;

	@CreatedDate
	@Column(name = "created_at", updatable = false, nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "viewed_at", updatable = false, nullable = false)
	private LocalDateTime viewedAt;

	@Column(name = "validated_at", updatable = false, nullable = false)
	private LocalDateTime validatedAt;


	@ManyToOne
	@JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_token_user"))
	private User user;

}
