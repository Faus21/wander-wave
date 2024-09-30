package com.dama.wanderwave.report.repository;

import com.dama.wanderwave.post.Post;
import com.dama.wanderwave.report.entity.PostReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostReportRepository extends JpaRepository<PostReport, String> {
    List<PostReport> findAllByPost(Post post);
}
