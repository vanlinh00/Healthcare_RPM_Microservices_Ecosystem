package com.healthcare.user.dto;

import com.healthcare.user.model.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Payload for onboarding new patient, physician, nurse, or clinical staff into Keycloak and PostgreSQL")
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "Primary clinical email address", example = "emily.vance@healthcare.org", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    /**
     * Optional custom username; defaults to email if omitted.
     */
    @Schema(description = "Optional custom username (defaults to email if omitted)", example = "doctor_emily")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Schema(description = "Secure password (minimum 8 characters)", example = "Password123!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @NotBlank(message = "First name is required")
    @Schema(description = "First legal name", example = "Emily", requiredMode = Schema.RequiredMode.REQUIRED)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Schema(description = "Last legal name", example = "Vance", requiredMode = Schema.RequiredMode.REQUIRED)
    private String lastName;

    /**
     * Target user role (PATIENT, DOCTOR, NURSE, PHARMACIST, LAB_TECH, ADMIN).
     * Defaults to PATIENT if not specified.
     */
    @Builder.Default
    @Schema(description = "Ecosystem security role", example = "DOCTOR", defaultValue = "PATIENT")
    private UserRole role = UserRole.PATIENT;

    @Schema(description = "Primary contact phone number", example = "+1-555-019-2834")
    private String phoneNumber;

    /**
     * If true, enables TOTP 2FA for HIPAA compliance and generates secret + QR code URI.
     */
    @Schema(description = "Enforce RFC 6238 TOTP 2FA for HIPAA compliance", example = "true")
    private boolean enableTotp;

    // Optional physician-specific registration metadata (when role == DOCTOR)
    @Schema(description = "State medical license ID (required for DOCTOR)", example = "MD-CA-99201")
    private String medicalLicenseNumber;

    @Schema(description = "Clinical specialty (for DOCTOR)", example = "Cardiology")
    private String specialty;

    @Schema(description = "Years of clinical medical experience", example = "12")
    private Integer yearsOfExperience;

    @Schema(description = "Standard consultation fee in USD", example = "150.00")
    private BigDecimal consultationFee;

    // Metadata passed from HTTP request
    @Schema(hidden = true)
    private String clientIp;

    @Schema(hidden = true)
    private String userAgent;
}

