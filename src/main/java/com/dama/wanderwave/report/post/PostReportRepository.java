package com.dama.wanderwave.report.post;

import com.dama.wanderwave.post.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostReportRepository extends JpaRepository<PostReport, String> {
    List<PostReport> findAllByPost(Post post);
}
