package com.dama.wanderwave.report.comment;

import com.dama.wanderwave.comment.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentReportRepository extends JpaRepository<CommentReport, String> {

    List<CommentReport> findAllByComment(Comment comment);

}
