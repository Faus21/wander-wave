package com.dama.wanderwave.report.entity;

import com.dama.wanderwave.user.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "user_reports")
public class UserReport extends Report {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false, referencedColumnName = "user_id", foreignKey = @ForeignKey(name = "fk_user_report"))
    private User user;
}
