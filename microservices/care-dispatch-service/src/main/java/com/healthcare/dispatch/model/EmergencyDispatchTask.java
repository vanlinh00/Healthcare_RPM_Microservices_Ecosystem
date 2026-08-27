package com.healthcare.dispatch.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;

@Entity
@Table(name = "emergency_dispatch_tasks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyDispatchTask {

    @Id
    @Column(length = 64, nullable = false)
    private String id;

    @Column(name = "patient_id", length = 64, nullable = false)
    private String patientId;

    @Column(name = "assigned_responder_id", length = 64)
    private String assignedResponderId;

    @Column(name = "responder_type", length = 32) // DOCTOR, NURSE, AMBULANCE_PARAMEDIC
    private String responderType;

    @Column(name = "severity_code", length = 32) // CODE_BLUE, CODE_RED, URGENT, ROUTINE
    private String severityCode;

    @Column(name = "patient_location_lat")
    private Double patientLocationLat;

    @Column(name = "patient_location_lon")
    private Double patientLocationLon;

    @Column(length = 32)
    private String status; // CREATED, DISPATCHED, EN_ROUTE, ON_SCENE, COMPLETED, CANCELLED

    @Column(name = "eta_minutes")
    private Integer etaMinutes;

    @CreationTimestamp
    @Column(updatable = false)
    private ZonedDateTime createdAt;
}
