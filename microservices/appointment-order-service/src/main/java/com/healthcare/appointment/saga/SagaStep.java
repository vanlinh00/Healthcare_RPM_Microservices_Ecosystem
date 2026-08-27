package com.healthcare.appointment.saga;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@Builder
public class SagaStep {
    private String stepName;
    private String status; // PENDING, SUCCESS, COMPENSATING, COMPENSATED, FAILED
    private String detail;
    private ZonedDateTime timestamp;
}
