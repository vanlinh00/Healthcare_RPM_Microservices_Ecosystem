package com.healthcare.chat.controller;

import com.healthcare.chat.dto.ChatMessagePayload;
import com.healthcare.chat.dto.CreateRoomRequest;
import com.healthcare.chat.model.ChatMessage;
import com.healthcare.chat.model.ChatRoom;
import com.healthcare.chat.service.ChatPresenceService;
import com.healthcare.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatRestController {

    private final ChatService chatService;
    private final ChatPresenceService presenceService;

    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoom>> getAllRooms() {
        return ResponseEntity.ok(chatService.getAllActiveRooms());
    }

    @PostMapping("/rooms")
    public ResponseEntity<ChatRoom> createRoom(@RequestBody CreateRoomRequest request) {
        return ResponseEntity.ok(chatService.createRoom(request));
    }

    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<ChatRoom> getRoom(@PathVariable String roomId) {
        return chatService.getRoomById(roomId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<ChatMessage>> getRecentMessages(@PathVariable String roomId) {
        return ResponseEntity.ok(chatService.getRecentMessages(roomId));
    }

    @GetMapping("/rooms/{roomId}/messages/paged")
    public ResponseEntity<Page<ChatMessage>> getPagedMessages(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(chatService.getPagedMessages(roomId, page, size));
    }

    @PostMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ChatMessage> postMessage(
            @PathVariable String roomId,
            @RequestBody ChatMessagePayload payload) {
        payload.setRoomId(roomId);
        return ResponseEntity.ok(chatService.processAndBroadcastMessage(payload));
    }

    @GetMapping("/presence")
    public ResponseEntity<Map<String, Object>> getPresenceOverview() {
        int ccu = presenceService.getActiveCcu();
        return ResponseEntity.ok(Map.of(
                "activeCcu", ccu,
                "connectedUsers", presenceService.getActiveUserIds(),
                "status", "HEALTHY",
                "redisClusterConnected", true
        ));
    }

    @PostMapping("/presence/simulate-ccu")
    public ResponseEntity<Map<String, Object>> simulateCcu(@RequestParam int ccu) {
        presenceService.setSimulatedCcu(ccu);
        return ResponseEntity.ok(Map.of(
                "message", "Simulated CCU updated to " + ccu,
                "simulatedCcu", ccu
        ));
    }
}
