package com.dama.wanderwave.report;

import com.dama.wanderwave.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "reports", uniqueConstraints = {@UniqueConstraint(columnNames = {"report_id"}), @UniqueConstraint(columnNames = {"object_id"}), @UniqueConstraint(columnNames = {"user_sender_id"})})
public class Report {

	@Id
	@GeneratedValue(generator = "hash_generator")
	@GenericGenerator(name = "hash_generator", strategy = "com.dama.wanderwave.hash.HashUUIDGenerator")
	@Column(name = "report_id", nullable = false, updatable = false)
	private String id;

	@Max(1024)
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

	@Size(max = 8)
	@Column(nullable = false, name = "object_id")
	private String objectId;
}
