package com.healthcare.chat.dto;

import com.healthcare.chat.model.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessagePayload {
    private String id;
    private String roomId;
    private String senderId;
    private String senderName;
    private String senderRole;
    private String content;
    private MessageType type;
    @Builder.Default
    private Instant timestamp = Instant.now();
}
