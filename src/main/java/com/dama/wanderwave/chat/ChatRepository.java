package com.dama.wanderwave.chat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRepository extends JpaRepository<Chat, String> {

    Optional<Chat> findBySenderIdAndRecipientId( String senderId, String receiverId);
}