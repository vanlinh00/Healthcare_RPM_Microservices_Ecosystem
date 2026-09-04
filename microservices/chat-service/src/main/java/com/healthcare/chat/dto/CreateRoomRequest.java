package com.healthcare.chat.dto;

import com.healthcare.chat.model.RoomType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoomRequest {
    private String name;
    private RoomType type;
    private String appointmentId;
    private String patientId;
    private String doctorId;
    private String description;
}
