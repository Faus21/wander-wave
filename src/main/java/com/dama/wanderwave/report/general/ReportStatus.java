package com.dama.wanderwave.report.general;


import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(name = "report_status", uniqueConstraints = {@UniqueConstraint(columnNames = {"name"})})
public class ReportStatus {

    @Id
    @GeneratedValue(generator = "hash_generator")
    @GenericGenerator(name = "hash_generator", type = com.dama.wanderwave.hash.HashUUIDGenerator.class)
    @Column(name = "report_status_id", nullable = false, updatable = false)
    private String id;

    @Size(max = 50, message = "Report type name must be less than or equal to 50 characters")
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;
}
