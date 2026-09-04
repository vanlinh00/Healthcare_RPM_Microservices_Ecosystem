package com.healthcare.chat.repository;

import com.healthcare.chat.model.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {
    List<ChatMessage> findTop50ByRoomIdOrderByTimestampAsc(String roomId);
    Page<ChatMessage> findByRoomIdOrderByTimestampDesc(String roomId, Pageable pageable);
    long countByRoomId(String roomId);
}
