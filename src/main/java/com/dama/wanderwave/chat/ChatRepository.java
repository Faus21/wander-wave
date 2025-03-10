package com.dama.wanderwave.chat;

import jakarta.persistence.Tuple;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRepository extends JpaRepository<Chat, String> {

    @Query("SELECT c FROM Chat c WHERE (c.sender.id = :senderId AND c.recipient.id = :receiverId) OR (c.sender.id = :receiverId AND c.recipient.id = :senderId)")
    Optional<Chat> findBySenderIdAndRecipientId(@Param("senderId") String senderId, @Param("receiverId") String receiverId);

    @Query(value = """
    SELECT DISTINCT ON (u.user_id)
        u.user_id AS userId,
        u.nickname AS name,
        u.image_url AS imgUrl,
        m.content AS content,
        m.created_at AS createdAt
    FROM
        users u
    LEFT JOIN
        messages m ON (m.sender_id = u.user_id AND m.recipient_id = :userId)
        OR (m.sender_id = :userId AND m.recipient_id = u.user_id)
    WHERE
        u.user_id IN (
            SELECT DISTINCT
                CASE
                    WHEN m.sender_id = :userId THEN m.recipient_id
                    WHEN m.recipient_id = :userId THEN m.sender_id
                END AS chat_partner_id
            FROM
                messages m
            WHERE
                m.sender_id = :userId OR m.recipient_id = :userId
        )
    ORDER BY
        u.user_id, COALESCE(m.created_at, CURRENT_TIMESTAMP) DESC
    """, nativeQuery = true)
    List<Tuple> findChatListWithLastMessage(@Param("userId") String userId);

}