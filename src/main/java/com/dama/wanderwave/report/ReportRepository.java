package com.dama.wanderwave.report;

import com.dama.wanderwave.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, String> {

    @Query("select r from Report r where r.createdAt < :to and r.createdAt > :from")
    List<Report> findByDates(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    List<Report> findAllBySender(User sender);

}
