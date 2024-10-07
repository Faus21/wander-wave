package com.dama.wanderwave.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRepository extends JpaRepository<Chat, String> {

    Optional<Chat> findBySenderIdAndRecipientId( String senderId, String receiverId);

    @Query("SELECT new com.dama.wanderwave.chat.ChatListElement( " +
                   "CASE WHEN c.sender.id = :userId THEN c.recipient.id ELSE c.sender.id END, " +
                   "CASE WHEN c.sender.id = :userId THEN c.recipient.nickname ELSE c.sender.nickname END, " +
                   "CASE WHEN c.sender.id = :userId THEN c.recipient.imageUrl ELSE c.sender.imageUrl END, m.content) " +
                   "FROM Chat c " +
                   "JOIN ChatMessage m ON c.id = m.chat.id " +
                   "WHERE (c.sender.id = :userId OR c.recipient.id = :userId) " +
                   "AND m.createdAt = (SELECT MAX(m2.createdAt) FROM ChatMessage m2 WHERE m2.chat.id = c.id) " +
                   "ORDER BY m.createdAt DESC")
    List<ChatListElement> findChatListWithLastMessage(@Param("userId") String userId);

}