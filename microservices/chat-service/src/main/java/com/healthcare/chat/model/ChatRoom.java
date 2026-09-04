package com.healthcare.chat.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "chat_rooms", indexes = {
        @Index(name = "idx_room_appointment", columnList = "appointmentId"),
        @Index(name = "idx_room_patient", columnList = "patientId"),
        @Index(name = "idx_room_doctor", columnList = "doctorId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomType type;

    private String appointmentId;

    private String patientId;

    private String doctorId;

    @Column(length = 1000)
    private String description;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private Instant createdAt = Instant.now();

    @Builder.Default
    private Instant updatedAt = Instant.now();
}
