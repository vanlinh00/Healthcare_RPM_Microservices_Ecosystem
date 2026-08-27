package com.healthcare.appointment.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(name = "appointment_orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentOrder {

    @Id
    @Column(length = 64, nullable = false)
    private String id;

    @Column(name = "patient_id", length = 64, nullable = false)
    private String patientId;

    @Column(name = "doctor_id", length = 64, nullable = false)
    private String doctorId;

    @Column(name = "appointment_time", nullable = false)
    private ZonedDateTime appointmentTime;

    @Column(name = "consultation_type", length = 32, nullable = false)
    private String consultationType; // GENERAL, SPECIALIST, EMERGENCY

    @Column(length = 32, nullable = false)
    private String status; // PENDING, RESERVED, CONFIRMED, CANCELLED, COMPLETED

    @Column(name = "base_fee", precision = 10, scale = 2)
    private BigDecimal baseFee;

    @Column(name = "copay_amount", precision = 10, scale = 2)
    private BigDecimal copayAmount;

    @Column(name = "insurance_covered_amount", precision = 10, scale = 2)
    private BigDecimal insuranceCoveredAmount;

    @Column(name = "pricing_strategy_applied", length = 64)
    private String pricingStrategyApplied;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;
}
