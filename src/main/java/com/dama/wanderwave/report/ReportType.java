package com.dama.wanderwave.report;

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
@Table(name = "report_types", uniqueConstraints = {@UniqueConstraint(columnNames = {"report_type_id"})})
public class ReportType {

    @Id
    @GeneratedValue(generator = "hash_generator")
    @GenericGenerator(name = "hash_generator", strategy = "com.dama.wanderwave.hash.HashUUIDGenerator")
    @Column(name = "report_type_id", nullable = false, updatable = false)
    private String id;

    @Size(max = 50, message = "Report type name must be less than or equal to 50 characters")
    @Column(nullable = false, unique = true, length = 50)
    private String name;
}
