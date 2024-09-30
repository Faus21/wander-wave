package com.dama.wanderwave.report.post;

import com.dama.wanderwave.post.Post;
import com.dama.wanderwave.report.general.Report;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "post_reports")
public class PostReport extends Report {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "post_id", nullable = false, referencedColumnName = "post_id", foreignKey = @ForeignKey(name = "fk_post_report"))
    private Post post;
}
