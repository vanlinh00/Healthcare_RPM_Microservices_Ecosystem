package com.healthcare.chat.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_msg_room_timestamp", columnList = "roomId, timestamp"),
        @Index(name = "idx_msg_sender", columnList = "senderId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String roomId;

    @Column(nullable = false)
    private String senderId;

    @Column(nullable = false)
    private String senderName;

    private String senderRole; // ROLE_DOCTOR, ROLE_PATIENT, ROLE_NURSE, ROLE_ADMIN, SYSTEM

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType type;

    @Builder.Default
    private Instant timestamp = Instant.now();

    @Builder.Default
    private boolean delivered = true;

    @Builder.Default
    private boolean read = false;
}
