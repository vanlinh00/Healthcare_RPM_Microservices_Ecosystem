package com.healthcare.user.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standardized REST Error payload conforming to RFC 7807 Problem Details")
public class ApiErrorResponse {

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "HTTP error title", example = "Bad Request")
    private String error;

    @Schema(description = "Descriptive error message", example = "Invalid TOTP 2FA code provided")
    private String message;

    @Schema(description = "Requested resource path", example = "/api/v1/auth/login")
    private String path;

    @Builder.Default
    @Schema(description = "ISO-8601 error timestamp", example = "2026-09-03T03:15:00.000Z")
    private String timestamp = ZonedDateTime.now().toString();

    @Schema(description = "Detailed field validation errors", example = "[\"usernameOrEmail: Username or email is required\"]")
    private List<String> validationErrors;

    @Schema(description = "Optional diagnostic details")
    private Map<String, Object> details;
}

