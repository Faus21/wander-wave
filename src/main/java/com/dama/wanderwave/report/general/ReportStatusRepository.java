package com.dama.wanderwave.report.general;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReportStatusRepository extends JpaRepository<ReportStatus, String> {

    Optional<ReportStatus> findByName(String name);

}
