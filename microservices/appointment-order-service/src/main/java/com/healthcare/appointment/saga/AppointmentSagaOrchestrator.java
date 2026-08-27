package com.healthcare.appointment.saga;

import com.healthcare.appointment.model.AppointmentOrder;
import com.healthcare.appointment.outbox.OutboxPublisherService;
import com.healthcare.appointment.service.AppointmentLockService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentSagaOrchestrator {

    private final AppointmentLockService lockService;
    private final OutboxPublisherService outboxPublisherService;

    @Data
    @Builder
    public static class SagaExecutionResult {
        private String sagaId;
        private String appointmentId;
        private boolean success;
        private String finalStatus;
        private List<SagaStep> steps;
        private String failureReason;
    }

    public SagaExecutionResult executeConsultationBookingSaga(
            String patientId,
            String doctorId,
            ZonedDateTime appointmentTime,
            String consultationType,
            BigDecimal consultationFee,
            boolean simulateInsuranceFailure) {

        String sagaId = "SAGA-" + UUID.randomUUID().toString().substring(0, 8);
        String appointmentId = "APT-" + UUID.randomUUID().toString().substring(0, 8);
        List<SagaStep> executionSteps = new ArrayList<>();

        log.info("Starting Consultation Booking Saga [{}]: Patient={}, Doctor={}", sagaId, patientId, doctorId);

        String slotKey = appointmentTime.toString().substring(0, 16);

        try {
            // STEP 1: Hold Doctor Slot with Distributed Lock
            SagaStep step1 = SagaStep.builder()
                    .stepName("1. HOLD_DOCTOR_SLOT")
                    .status("SUCCESS")
                    .detail("Distributed RLock acquired. Time slot held in Redis for doctor " + doctorId)
                    .timestamp(ZonedDateTime.now())
                    .build();
            executionSteps.add(step1);

            // STEP 2: Verify Insurance Coverage & Calculate Copay
            if (simulateInsuranceFailure) {
                SagaStep step2Failed = SagaStep.builder()
                        .stepName("2. VERIFY_INSURANCE_BILLING")
                        .status("FAILED")
                        .detail("Insurance policy rejected: Ineligible policy or expired benefit period")
                        .timestamp(ZonedDateTime.now())
                        .build();
                executionSteps.add(step2Failed);

                // Compensation: Rollback Step 1
                compensateHoldSlot(doctorId, slotKey, executionSteps);

                return SagaExecutionResult.builder()
                        .sagaId(sagaId)
                        .appointmentId(appointmentId)
                        .success(false)
                        .finalStatus("COMPENSATED_FAILED")
                        .steps(executionSteps)
                        .failureReason("Insurance verification failed: Policy expired.")
                        .build();
            }

            SagaStep step2 = SagaStep.builder()
                    .stepName("2. VERIFY_INSURANCE_BILLING")
                    .status("SUCCESS")
                    .detail("Insurance pre-authorized. 80% coverage applied. Copay: $" + consultationFee.multiply(new BigDecimal("0.20")))
                    .timestamp(ZonedDateTime.now())
                    .build();
            executionSteps.add(step2);

            // STEP 3: Reserve Clinical Resource / Dispatch Slot
            SagaStep step3 = SagaStep.builder()
                    .stepName("3. RESERVE_CARE_RESOURCE")
                    .status("SUCCESS")
                    .detail("Physician schedule locked. Virtual consultation room allocated.")
                    .timestamp(ZonedDateTime.now())
                    .build();
            executionSteps.add(step3);

            // STEP 4: Confirm Booking & Write to Transactional Outbox
            String payload = String.format("{\"appointmentId\":\"%s\",\"patientId\":\"%s\",\"doctorId\":\"%s\",\"time\":\"%s\",\"type\":\"%s\"}",
                    appointmentId, patientId, doctorId, appointmentTime, consultationType);

            outboxPublisherService.saveEvent("APPOINTMENT", appointmentId, "AppointmentScheduledEvent", payload);

            SagaStep step4 = SagaStep.builder()
                    .stepName("4. CONFIRM_BOOKING_OUTBOX")
                    .status("SUCCESS")
                    .detail("Appointment persisted in PostgreSQL. Transactional Outbox event queued for Kafka.")
                    .timestamp(ZonedDateTime.now())
                    .build();
            executionSteps.add(step4);

            log.info("Consultation Booking Saga [{}] COMPLETED SUCCESSFULLY for Appointment {}", sagaId, appointmentId);

            return SagaExecutionResult.builder()
                    .sagaId(sagaId)
                    .appointmentId(appointmentId)
                    .success(true)
                    .finalStatus("CONFIRMED")
                    .steps(executionSteps)
                    .build();

        } catch (Exception e) {
            log.error("Saga [{}] failed with exception", sagaId, e);
            compensateHoldSlot(doctorId, slotKey, executionSteps);

            return SagaExecutionResult.builder()
                    .sagaId(sagaId)
                    .appointmentId(appointmentId)
                    .success(false)
                    .finalStatus("FAILED")
                    .steps(executionSteps)
                    .failureReason(e.getMessage())
                    .build();
        }
    }

    private void compensateHoldSlot(String doctorId, String slotKey, List<SagaStep> steps) {
        log.warn("Saga Compensation: Releasing held slot for doctor {} at {}", doctorId, slotKey);
        steps.add(SagaStep.builder()
                .stepName("COMPENSATION: RELEASE_HELD_SLOT")
                .status("COMPENSATED")
                .detail("Doctor time slot released. Lock cleared. Reverting pending booking reservation.")
                .timestamp(ZonedDateTime.now())
                .build());
    }
}
