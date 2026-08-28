package com.healthcare.user.dto;

import com.healthcare.user.model.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for new user registration with Keycloak IAM credentials and HIPAA profile attributes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    /**
     * Optional custom username; defaults to email if omitted.
     */
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    /**
     * Target user role (PATIENT, DOCTOR, NURSE, PHARMACIST, LAB_TECH, ADMIN).
     * Defaults to PATIENT if not specified.
     */
    @Builder.Default
    private UserRole role = UserRole.PATIENT;

    private String phoneNumber;

    /**
     * If true, enables TOTP 2FA for HIPAA compliance and generates secret + QR code URI.
     */
    private boolean enableTotp;

    // Optional physician-specific registration metadata (when role == DOCTOR)
    private String medicalLicenseNumber;
    private String specialty;
    private Integer yearsOfExperience;
    private BigDecimal consultationFee;

    // Metadata passed from HTTP request
    private String clientIp;
    private String userAgent;
}
