package com.healthcare.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.healthcare.user.model.DoctorProfile;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Response DTO returned upon successful user registration in Keycloak IAM and PostgreSQL database.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response payload after successful user provisioning in Keycloak and PostgreSQL")
public class RegisterResponse {

    @Schema(description = "Keycloak user UUID", example = "usr-doc-204")
    private String id;

    @Schema(description = "User primary email address", example = "emily.vance@healthcare.org")
    private String email;

    @Schema(description = "Unique user login handle", example = "doctor_emily")
    private String username;

    @Schema(description = "First name", example = "Emily")
    private String firstName;

    @Schema(description = "Last name", example = "Vance")
    private String lastName;

    @Schema(description = "Primary assigned role", example = "DOCTOR")
    private String primaryRole;

    @Schema(description = "Assigned Keycloak realm & client roles", example = "[\"DOCTOR\", \"default-roles-healthcare\"]")
    private List<String> roles;

    @Schema(description = "Primary phone number", example = "+1-555-019-2834")
    private String phoneNumber;

    @Schema(description = "Account enabled status", example = "true")
    private boolean active;

    @JsonProperty("totp_enabled")
    @Schema(description = "Indicates whether 2FA was provisioned", example = "true")
    private boolean totpEnabled;

    @JsonProperty("totp_secret")
    @Schema(description = "Base32 TOTP secret for authenticator app enrollment", example = "JBSWY3DPEHPK3PXP")
    private String totpSecret;

    @JsonProperty("totp_qr_code_uri")
    @Schema(description = "Data URI of QR code image for Google Authenticator / 1Password", example = "data:image/png;base64,iVBORw0KGgo...")
    private String totpQrCodeUri;

    @Schema(description = "Registration confirmation message", example = "User registered successfully in Keycloak IAM.")
    private String message;

    @Schema(description = "Physician clinical profile (if role == DOCTOR)")
    private DoctorProfile doctorProfile;

    @Schema(description = "Creation timestamp")
    private ZonedDateTime createdAt;
}

