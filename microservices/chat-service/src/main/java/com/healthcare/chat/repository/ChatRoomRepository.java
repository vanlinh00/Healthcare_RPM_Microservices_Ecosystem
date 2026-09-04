package com.healthcare.chat.repository;

import com.healthcare.chat.model.ChatRoom;
import com.healthcare.chat.model.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, String> {
    List<ChatRoom> findByActiveTrueOrderByUpdatedAtDesc();
    List<ChatRoom> findByPatientIdOrDoctorIdOrderByUpdatedAtDesc(String patientId, String doctorId);
    List<ChatRoom> findByType(RoomType type);
    Optional<ChatRoom> findByAppointmentId(String appointmentId);
}
