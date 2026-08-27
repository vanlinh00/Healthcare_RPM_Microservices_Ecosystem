package com.healthcare.user.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(name = "doctor_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorProfile {

    @Id
    @Column(name = "doctor_id", length = 64, nullable = false)
    private String doctorId;

    @Column(name = "medical_license_number", length = 64, nullable = false, unique = true)
    private String medicalLicenseNumber;

    @Column(nullable = false, length = 100)
    private String specialty;

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    @Column(name = "is_verified")
    private boolean verified;

    @Column(name = "verified_by_admin", length = 64)
    private String verifiedByAdmin;

    @Column(name = "verification_timestamp")
    private ZonedDateTime verificationTimestamp;

    @Column(name = "consultation_fee", precision = 10, scale = 2)
    private BigDecimal consultationFee;
}
