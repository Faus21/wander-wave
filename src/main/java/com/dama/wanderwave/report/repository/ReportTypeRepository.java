package com.dama.wanderwave.report.repository;

import com.dama.wanderwave.report.entity.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReportTypeRepository extends JpaRepository<ReportType, String> {

    Optional<ReportType> findByName(String name);

}
