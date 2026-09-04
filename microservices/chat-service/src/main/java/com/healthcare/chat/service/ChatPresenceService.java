package com.healthcare.chat.service;

import com.healthcare.chat.config.RedisChatConfig;
import com.healthcare.chat.dto.UserPresenceDto;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class ChatPresenceService {

    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final AtomicInteger currentCcuGauge = new AtomicInteger(0);

    // In-memory fallback tracking in case Redis is in single-node mock mode
    private final ConcurrentHashMap<String, UserPresenceDto> localPresenceMap = new ConcurrentHashMap<>();

    public ChatPresenceService(StringRedisTemplate redisTemplate,
                               SimpMessagingTemplate messagingTemplate,
                               MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.messagingTemplate = messagingTemplate;

        // Register custom Prometheus Gauge for CCU (Concurrent Connected Users)
        Gauge.builder("chat_active_ccu", currentCcuGauge, AtomicInteger::get)
                .description("Active Concurrent Connected Users (CCU) connected via WebSocket/STOMP")
                .tag("service", "chat-service")
                .tag("region", "ap-northeast-1")
                .register(meterRegistry);
    }

    public void userConnected(String userId, String username, String role, String roomId) {
        UserPresenceDto presence = UserPresenceDto.builder()
                .userId(userId)
                .username(username)
                .role(role)
                .status("ONLINE")
                .activeRoomId(roomId)
                .lastHeartbeat(Instant.now())
                .build();

        localPresenceMap.put(userId, presence);

        try {
            redisTemplate.opsForSet().add(RedisChatConfig.CHAT_ACTIVE_CCU_SET, userId);
            redisTemplate.opsForValue().set(
                    RedisChatConfig.CHAT_PRESENCE_KEY_PREFIX + userId,
                    "ONLINE:" + roomId + ":" + System.currentTimeMillis()
            );
        } catch (Exception e) {
            log.warn("Redis presence update failed, relying on in-memory presence: {}", e.getMessage());
        }

        updateCcuMetric();
        broadcastPresence(presence);
        log.info("User connected to chat: {} ({}) in room: {}. Total CCU: {}", username, userId, roomId, getActiveCcu());
    }

    public void userDisconnected(String userId) {
        UserPresenceDto presence = localPresenceMap.remove(userId);
        if (presence != null) {
            presence.setStatus("OFFLINE");
            presence.setLastHeartbeat(Instant.now());
            broadcastPresence(presence);
        }

        try {
            redisTemplate.opsForSet().remove(RedisChatConfig.CHAT_ACTIVE_CCU_SET, userId);
            redisTemplate.delete(RedisChatConfig.CHAT_PRESENCE_KEY_PREFIX + userId);
        } catch (Exception e) {
            log.warn("Redis presence removal failed: {}", e.getMessage());
        }

        updateCcuMetric();
        log.info("User disconnected from chat: {}. Total CCU: {}", userId, getActiveCcu());
    }

    public void updateHeartbeat(String userId) {
        UserPresenceDto presence = localPresenceMap.get(userId);
        if (presence != null) {
            presence.setLastHeartbeat(Instant.now());
        }
        try {
            redisTemplate.opsForValue().set(
                    RedisChatConfig.CHAT_PRESENCE_KEY_PREFIX + userId,
                    "ONLINE:HEARTBEAT:" + System.currentTimeMillis()
            );
        } catch (Exception ignored) {
        }
    }

    public int getActiveCcu() {
        try {
            Long size = redisTemplate.opsForSet().size(RedisChatConfig.CHAT_ACTIVE_CCU_SET);
            if (size != null && size > 0) {
                return size.intValue();
            }
        } catch (Exception ignored) {
        }
        return Math.max(localPresenceMap.size(), currentCcuGauge.get());
    }

    public void setSimulatedCcu(int ccu) {
        currentCcuGauge.set(ccu);
    }

    public Set<String> getActiveUserIds() {
        try {
            Set<String> members = redisTemplate.opsForSet().members(RedisChatConfig.CHAT_ACTIVE_CCU_SET);
            if (members != null && !members.isEmpty()) {
                return members;
            }
        } catch (Exception ignored) {
        }
        return Collections.unmodifiableSet(localPresenceMap.keySet());
    }

    private void updateCcuMetric() {
        currentCcuGauge.set(getActiveCcu());
    }

    private void broadcastPresence(UserPresenceDto presence) {
        messagingTemplate.convertAndSend("/topic/presence", presence);
        if (presence.getActiveRoomId() != null) {
            messagingTemplate.convertAndSend("/topic/room." + presence.getActiveRoomId() + ".presence", presence);
        }
    }

    @Scheduled(fixedRate = 30000)
    public void cleanupStaleSessions() {
        Instant threshold = Instant.now().minusSeconds(90);
        localPresenceMap.entrySet().removeIf(entry -> {
            boolean stale = entry.getValue().getLastHeartbeat().isBefore(threshold);
            if (stale) {
                try {
                    redisTemplate.opsForSet().remove(RedisChatConfig.CHAT_ACTIVE_CCU_SET, entry.getKey());
                } catch (Exception ignored) {
                }
            }
            return stale;
        });
        updateCcuMetric();
    }
}
