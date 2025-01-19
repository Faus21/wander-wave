package com.dama.wanderwave.report.general;

import com.dama.wanderwave.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserReportRepository extends JpaRepository<UserReport, String>, JpaSpecificationExecutor<UserReport> {
    @NonNull
    Page<UserReport> findAll(@NonNull Pageable pageable);
    Page<UserReport> findAllBySender(@NonNull Pageable pageable, User sender);
    List<UserReport> findAllByReported(User reported);

    @Query("select r from UserReport r " +
            "left join CommentReport cr on r.id = cr.id " +
            "left join PostReport pr on r.id = pr.id " +
            "where (cr.comment.id = :objectId or pr.post.id = :objectId or r.reported.id = :objectId) " +
            "and r.sender.id = :senderId")
    Optional<UserReport> findUserReportByObjectAndSender(@Param("objectId") String objectId,
                                                         @Param("senderId") String senderId);

    @Query("SELECT r FROM UserReport r " +
            "WHERE (:from IS NULL OR r.sender.id IN :from) " +
            "AND (:on IS NULL OR r.reported.id IN :on) " +
            "AND (:admins IS NULL OR r.reviewedBy.id IN :admins) " +
            "AND (:category IS NULL OR r.type.name = :category) " +
            "AND (:status IS NULL OR r.status.name = :status) " +
            "AND (:startDate IS NULL OR r.createdAt >= :startDate) " +
            "AND (:endDate IS NULL OR r.createdAt <= :endDate)")
    Page<UserReport> filterReports(
            @Param("from") List<String> from,
            @Param("on") List<String> on,
            @Param("admins") List<String> admins,
            @Param("category") String category,
            @Param("status") String status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
}
