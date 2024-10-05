package com.dama.wanderwave.report.general;

import com.dama.wanderwave.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

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

}
