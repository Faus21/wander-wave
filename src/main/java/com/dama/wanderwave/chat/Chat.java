package com.dama.wanderwave.chat;

import com.dama.wanderwave.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "chats")
public class Chat {

	@Id
	@GeneratedValue(generator = "hash_generator")
	@GenericGenerator(name = "hash_generator", type = com.dama.wanderwave.hash.HashUUIDGenerator.class)
	@Column(name = "chat_id", nullable = false, updatable = false)
	private String id;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "user1_id", nullable = false)
	@NotNull(message = "User1 must be specified")
	private User user1;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "user2_id", nullable = false)
	@NotNull(message = "User2 must be specified")
	private User user2;

	@Column(name = "user_1_mute", nullable = false)
	private boolean user1Mute = false;

	@Column(name = "user_2_mute", nullable = false)
	private boolean user2Mute = false;
}
