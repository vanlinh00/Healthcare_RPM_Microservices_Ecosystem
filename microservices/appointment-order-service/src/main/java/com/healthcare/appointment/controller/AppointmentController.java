package com.healthcare.appointment.controller;

import com.healthcare.appointment.saga.AppointmentSagaOrchestrator;
import com.healthcare.appointment.strategy.ConsultationPricingContext;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentSagaOrchestrator sagaOrchestrator;
    private final ConsultationPricingContext pricingContext;

    @Data
    public static class BookAppointmentRequest {
        private String patientId;
        private String doctorId;
        private String consultationType; // STANDARD, SPECIALIST, EMERGENCY
        private BigDecimal fee;
        private boolean simulateFailure;
    }

    @PostMapping("/book")
    public ResponseEntity<AppointmentSagaOrchestrator.SagaExecutionResult> bookAppointment(
            @RequestBody BookAppointmentRequest request) {

        AppointmentSagaOrchestrator.SagaExecutionResult result = sagaOrchestrator.executeConsultationBookingSaga(
                request.getPatientId(),
                request.getDoctorId(),
                ZonedDateTime.now().plusDays(1),
                request.getConsultationType(),
                request.getFee() != null ? request.getFee() : new BigDecimal("150.00"),
                request.isSimulateFailure()
        );

        return ResponseEntity.ok(result);
    }

    @GetMapping("/pricing/calculate")
    public ResponseEntity<Map<String, Object>> calculatePricing(
            @RequestParam("strategy") String strategy,
            @RequestParam("baseFee") BigDecimal baseFee,
            @RequestParam("coverage") double coverage) {

        BigDecimal copay = pricingContext.calculateCopay(strategy, baseFee, coverage);
        return ResponseEntity.ok(Map.of(
                "strategy", strategy,
                "baseFee", baseFee,
                "coveragePercent", coverage * 100,
                "calculatedCopay", copay
        ));
    }
}
