package com.healthcare.chat.controller;

import com.healthcare.chat.dto.ChatMessagePayload;
import com.healthcare.chat.dto.UserPresenceDto;
import com.healthcare.chat.model.MessageType;
import com.healthcare.chat.service.ChatPresenceService;
import com.healthcare.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.Instant;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final ChatService chatService;
    private final ChatPresenceService presenceService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessagePayload payload) {
        log.debug("Received chat message for room {}: {}", payload.getRoomId(), payload.getContent());
        chatService.processAndBroadcastMessage(payload);
    }

    @MessageMapping("/chat.addUser")
    public void addUser(@Payload UserPresenceDto presence, SimpMessageHeaderAccessor headerAccessor) {
        if (headerAccessor.getSessionAttributes() != null) {
            headerAccessor.getSessionAttributes().put("userId", presence.getUserId());
            headerAccessor.getSessionAttributes().put("roomId", presence.getActiveRoomId());
        }

        presenceService.userConnected(
                presence.getUserId(),
                presence.getUsername(),
                presence.getRole(),
                presence.getActiveRoomId()
        );

        // Send a join notification into the chat room
        ChatMessagePayload joinNotice = ChatMessagePayload.builder()
                .roomId(presence.getActiveRoomId())
                .senderId("SYSTEM")
                .senderName("System")
                .senderRole("SYSTEM")
                .content(presence.getUsername() + " đã tham gia phòng chat.")
                .type(MessageType.JOIN)
                .timestamp(Instant.now())
                .build();

        chatService.processAndBroadcastMessage(joinNotice);
    }

    @MessageMapping("/chat.typing")
    public void userTyping(@Payload ChatMessagePayload payload) {
        payload.setType(MessageType.TYPING);
        messagingTemplate.convertAndSend("/topic/room." + payload.getRoomId() + ".typing", payload);
    }
}
