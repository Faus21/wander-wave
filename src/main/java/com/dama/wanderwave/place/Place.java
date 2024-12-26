package com.dama.wanderwave.place;

import com.dama.wanderwave.post.Post;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "places")
@Builder
public class Place {
	@Id
	@GeneratedValue(generator = "hash_generator")
	@GenericGenerator(name = "hash_generator", type = com.dama.wanderwave.hash.HashUUIDGenerator.class)
	@Column(name = "place_id")
	private String id;

	@Size(max = 64)
	@NotBlank
	@Column(nullable = false, length = 64)
	private String displayName;

	@Size(max = 64)
	@NotBlank
	@Column(nullable = false, length = 64)
	private String locationName;

	@Size(max = 500, message = "Place description length must be less than or equal to 500 characters")
	@NotBlank(message = "Place description cannot be blank")
	@Column(nullable = false, length = 500)
	private String description;

	@Min(1)
	@Max(5)
	@Column(nullable = false)
	private double rating;

	@Column(columnDefinition = "Numeric(9,6)", nullable = false)
	private BigDecimal longitude;

	@Column(columnDefinition = "Numeric(9,6)", nullable = false)
	private BigDecimal latitude;

	@ManyToOne
	@JoinColumn(name = "post_id",
			referencedColumnName = "post_id",
			nullable = false,
			foreignKey = @ForeignKey(name = "fk_place_post"))
	private Post post;
}
