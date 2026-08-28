package com.healthcare.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.healthcare.user.model.DoctorProfile;
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
public class RegisterResponse {

    private String id;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private String primaryRole;
    private List<String> roles;
    private String phoneNumber;
    private boolean active;

    @JsonProperty("totp_enabled")
    private boolean totpEnabled;

    @JsonProperty("totp_secret")
    private String totpSecret;

    @JsonProperty("totp_qr_code_uri")
    private String totpQrCodeUri;

    private String message;

    private DoctorProfile doctorProfile;

    private ZonedDateTime createdAt;
}
