package com.dama.wanderwave.chat;



import com.dama.wanderwave.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "chat_rooms")
public class Chat {

	@Id
	@GeneratedValue(generator = "hash_generator")
	@GenericGenerator(name = "hash_generator", type = com.dama.wanderwave.hash.HashUUIDGenerator.class)
	@Column(name = "chat_id", nullable = false, updatable = false)
	private String id;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "sender_id", nullable = false)
	@NotNull(message = "Sender must be specified")
	private User sender;

	@OneToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "recipient_id", nullable = false)
	@NotNull(message = "Recipiet must be specified")
	private User recipient;

	@Builder.Default
	@Column( nullable = false)
	private boolean muted = false;


}

