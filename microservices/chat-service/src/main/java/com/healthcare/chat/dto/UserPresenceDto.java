package com.healthcare.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPresenceDto {
    private String userId;
    private String username;
    private String role;
    private String status; // ONLINE, AWAY, OFFLINE
    private String activeRoomId;
    private Instant lastHeartbeat;
}
