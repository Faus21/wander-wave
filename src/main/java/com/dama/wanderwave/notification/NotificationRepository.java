package com.dama.wanderwave.notification;

import com.dama.wanderwave.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, String> {
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(String recipientId, Pageable pageable);

    Page<Notification> findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(String recipientId, Pageable pageable);

    List<Notification> findByRecipientIdAndIsReadFalse(String recipientId);

    boolean existsByRecipientAndActionUserAndObjectId(User recipientId, User actionUser, String objectId);

    List<Notification> findAllByObjectId(String objectId);
}