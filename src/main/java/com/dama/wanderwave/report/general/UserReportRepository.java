package com.dama.wanderwave.report.general;

import com.dama.wanderwave.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.NonNull;

import java.util.List;

public interface UserReportRepository extends JpaRepository<UserReport, String>, JpaSpecificationExecutor<UserReport> {
    @NonNull
    Page<UserReport> findAll(@NonNull Pageable pageable);
    Page<UserReport> findAllBySender(@NonNull Pageable pageable, User sender);
    List<UserReport> findAllByReported(User reported);

}
