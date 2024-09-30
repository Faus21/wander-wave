package com.dama.wanderwave.report.general;

import com.dama.wanderwave.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.NonNull;

public interface ReportRepository extends JpaRepository<Report, String>, JpaSpecificationExecutor<Report> {

    @NonNull
    Page<Report> findAll(@NonNull Pageable pageable);
    Page<Report> findAllBySender(@NonNull Pageable pageable, User sender);

}
