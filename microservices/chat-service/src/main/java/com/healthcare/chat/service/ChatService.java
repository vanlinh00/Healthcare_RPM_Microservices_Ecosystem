package com.healthcare.chat.service;

import com.healthcare.chat.dto.ChatMessagePayload;
import com.healthcare.chat.dto.CreateRoomRequest;
import com.healthcare.chat.model.ChatMessage;
import com.healthcare.chat.model.ChatRoom;
import com.healthcare.chat.model.MessageType;
import com.healthcare.chat.model.RoomType;
import com.healthcare.chat.repository.ChatMessageRepository;
import com.healthcare.chat.repository.ChatRoomRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ChatService {

    private final ChatRoomRepository roomRepository;
    private final ChatMessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final Optional<KafkaTemplate<String, String>> kafkaTemplate;
    private final Counter messageCounter;

    public ChatService(ChatRoomRepository roomRepository,
                       ChatMessageRepository messageRepository,
                       SimpMessagingTemplate messagingTemplate,
                       Optional<KafkaTemplate<String, String>> kafkaTemplate,
                       MeterRegistry meterRegistry) {
        this.roomRepository = roomRepository;
        this.messageRepository = messageRepository;
        this.messagingTemplate = messagingTemplate;
        this.kafkaTemplate = kafkaTemplate;

        this.messageCounter = Counter.builder("chat_messages_total")
                .description("Total chat messages processed across all active consultation rooms")
                .tag("service", "chat-service")
                .register(meterRegistry);
    }

    @Transactional
    public ChatRoom createRoom(CreateRoomRequest request) {
        ChatRoom room = ChatRoom.builder()
                .name(request.getName())
                .type(request.getType() != null ? request.getType() : RoomType.DOCTOR_PATIENT)
                .appointmentId(request.getAppointmentId())
                .patientId(request.getPatientId())
                .doctorId(request.getDoctorId())
                .description(request.getDescription())
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        ChatRoom saved = roomRepository.save(room);
        log.info("Created chat room: {} of type {}", saved.getId(), saved.getType());
        return saved;
    }

    public List<ChatRoom> getAllActiveRooms() {
        return roomRepository.findByActiveTrueOrderByUpdatedAtDesc();
    }

    public Optional<ChatRoom> getRoomById(String roomId) {
        return roomRepository.findById(roomId);
    }

    public List<ChatRoom> getRoomsForUser(String userId) {
        return roomRepository.findByPatientIdOrDoctorIdOrderByUpdatedAtDesc(userId, userId);
    }

    @Transactional
    public ChatMessage processAndBroadcastMessage(ChatMessagePayload payload) {
        // 1. Persist message to database
        ChatMessage entity = ChatMessage.builder()
                .roomId(payload.getRoomId())
                .senderId(payload.getSenderId())
                .senderName(payload.getSenderName())
                .senderRole(payload.getSenderRole())
                .content(payload.getContent())
                .type(payload.getType() != null ? payload.getType() : MessageType.CHAT)
                .timestamp(Instant.now())
                .delivered(true)
                .read(false)
                .build();

        ChatMessage saved = messageRepository.save(entity);

        // 2. Update room timestamp
        roomRepository.findById(payload.getRoomId()).ifPresent(room -> {
            room.setUpdatedAt(Instant.now());
            roomRepository.save(room);
        });

        // 3. Increment Prometheus counter
        messageCounter.increment();

        // 4. Broadcast via WebSocket STOMP
        payload.setId(saved.getId());
        payload.setTimestamp(saved.getTimestamp());
        messagingTemplate.convertAndSend("/topic/room." + payload.getRoomId(), payload);

        // 5. Asynchronous Kafka event publication for audit / notification trigger
        kafkaTemplate.ifPresent(kt -> {
            try {
                String kafkaPayload = String.format("{\"messageId\":\"%s\",\"roomId\":\"%s\",\"senderId\":\"%s\",\"type\":\"%s\"}",
                        saved.getId(), saved.getRoomId(), saved.getSenderId(), saved.getType());
                kt.send("chat-messages-topic", saved.getRoomId(), kafkaPayload);
            } catch (Exception e) {
                log.warn("Failed to publish chat message to Kafka: {}", e.getMessage());
            }
        });

        log.debug("Broadcasted message {} in room {}", saved.getId(), saved.getRoomId());
        return saved;
    }

    public List<ChatMessage> getRecentMessages(String roomId) {
        return messageRepository.findTop50ByRoomIdOrderByTimestampAsc(roomId);
    }

    public Page<ChatMessage> getPagedMessages(String roomId, int page, int size) {
        return messageRepository.findByRoomIdOrderByTimestampDesc(roomId, PageRequest.of(page, size));
    }
}
