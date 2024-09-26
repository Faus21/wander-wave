package com.dama.wanderwave.report;

import com.dama.wanderwave.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "reports")
public class Report {

	@Id
	@GeneratedValue(generator = "hash_generator")
	@GenericGenerator(name = "hash_generator", type = com.dama.wanderwave.hash.HashUUIDGenerator.class)
	@Column(name = "report_id", nullable = false, updatable = false)
	private String id;

	@Size(max = 1024)
	@NotBlank(message = "Description cannot be blank")
	@Column(nullable = false, length = 1024)
	private String description;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "user_sender_id", nullable = false, referencedColumnName = "user_id", foreignKey = @ForeignKey(name = "fk_user_sender_report"))
	private User sender;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "user_id", referencedColumnName = "user_id", foreignKey = @ForeignKey(name = "fk_reported_user_report"))
	private User reportedUser;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "report_type_id", nullable = false, referencedColumnName = "report_type_id", foreignKey = @ForeignKey(name = "fk_report_type_report"))
	private ReportType type;

	@Size(max = 8, message = "Object id should be less or equals 8 characters")
	@Column(nullable = false, name = "object_id")
	private String objectId;

	@CreatedDate
	@Column(name = "created_at", updatable = false, nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "reviewed_at")
	private LocalDateTime reviewedAt;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "reviewed_by", referencedColumnName = "user_id", foreignKey = @ForeignKey(name = "fk_reviewed_by_report"))
	private User reviewedBy;

	@Size(max = 255, message = "Comment length should be less or equal 255")
	@Column(name = "comment")
	private String comment;
}
