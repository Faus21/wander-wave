package com.dama.wanderwave.message;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, ChatMessageKey> {
	List<ChatMessage> findByChatId( String chatRoomId);
}