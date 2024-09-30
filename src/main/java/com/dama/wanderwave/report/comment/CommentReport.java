package com.dama.wanderwave.report.entity;

import com.dama.wanderwave.comment.Comment;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "comment_reports")
public class CommentReport extends Report {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "comment_id", nullable = false, referencedColumnName = "comment_id", foreignKey = @ForeignKey(name = "fk_comment_report"))
    private Comment comment;
}
