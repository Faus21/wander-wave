package com.dama.wanderwave.report.repository;

import com.dama.wanderwave.report.entity.UserReport;
import com.dama.wanderwave.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserReportRepository extends JpaRepository<UserReport, String> {
    List<UserReport> findAllByUser(User user);
}
