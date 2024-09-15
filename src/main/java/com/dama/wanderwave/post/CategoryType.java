package com.dama.wanderwave.post;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "category_types", uniqueConstraints = {@UniqueConstraint(columnNames = {"category_type_id"})})
public class CategoryType {

    @Id
    @GeneratedValue(generator = "hash_generator")
    @GenericGenerator(name = "hash_generator", strategy = "com.dama.wanderwave.hash.HashUUIDGenerator")
    @Column(name = "category_type_id", nullable = false, updatable = false)
    private String id;

    @Size(max = 50, message = "Category name must be less than or equal to 50 characters")
    @Column(nullable = false, unique = true, length = 50)
    private String name;
}
