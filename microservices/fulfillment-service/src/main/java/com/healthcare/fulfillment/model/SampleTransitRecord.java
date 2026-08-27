package com.healthcare.fulfillment.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;

@Entity
@Table(name = "sample_transit_records")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SampleTransitRecord {

    @Id
    @Column(length = 64, nullable = false)
    private String id;

    @Column(name = "sample_type", length = 64, nullable = false) // BLOOD, VACCINE_VIAL, BIOPSY, URINE
    private String sampleType;

    @Column(name = "patient_id", length = 64, nullable = false)
    private String patientId;

    @Column(name = "current_temperature_celsius")
    private Double currentTemperatureCelsius;

    @Column(name = "min_allowed_temp_celsius")
    private Double minAllowedTempCelsius;

    @Column(name = "max_allowed_temp_celsius")
    private Double maxAllowedTempCelsius;

    @Column(name = "is_cold_chain_breached")
    private boolean coldChainBreached;

    @Column(name = "courier_courier_id", length = 64)
    private String courierId;

    @Column(length = 32)
    private String status; // IN_TRANSIT, DELIVERED_TO_LAB, ANOMALY_QUARANTINED

    @Column(name = "digital_signature", length = 512)
    private String digitalSignature;

    @CreationTimestamp
    @Column(updatable = false)
    private ZonedDateTime loggedAt;
}
