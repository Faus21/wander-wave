package com.dama.wanderwave.report.repository;

import com.dama.wanderwave.comment.Comment;
import com.dama.wanderwave.report.entity.CommentReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentReportRepository extends JpaRepository<CommentReport, String> {

    List<CommentReport> findAllByComment(Comment comment);

}
