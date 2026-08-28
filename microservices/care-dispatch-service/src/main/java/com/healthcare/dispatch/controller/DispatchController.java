package com.healthcare.dispatch.controller;

import com.healthcare.dispatch.model.EmergencyDispatchTask;
import com.healthcare.dispatch.service.NurseDispatchScoringService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dispatch")
@RequiredArgsConstructor
public class DispatchController {

    private final NurseDispatchScoringService scoringService;

    @Data
    public static class DispatchMatchRequest {
        private double patientLat;
        private double patientLon;
        private String specialty;
        private List<NurseDispatchScoringService.CaregiverCandidate> candidates;
    }

    @PostMapping("/match-optimal-responder")
    public ResponseEntity<NurseDispatchScoringService.DispatchRecommendation> matchResponder(
            @RequestBody DispatchMatchRequest request) {

        NurseDispatchScoringService.DispatchRecommendation recommendation = scoringService.findOptimalResponder(
                request.getPatientLat(),
                request.getPatientLon(),
                request.getSpecialty() != null ? request.getSpecialty() : "ICU",
                request.getCandidates()
        );

        return ResponseEntity.ok(recommendation);
    }

    @PostMapping("/emergency-code-blue")
    public ResponseEntity<EmergencyDispatchTask> triggerEmergencyCodeBlue(
            @RequestParam("patientId") String patientId,
            @RequestParam("lat") double lat,
            @RequestParam("lon") double lon) {

        EmergencyDispatchTask task = EmergencyDispatchTask.builder()
                .id("EMG-" + UUID.randomUUID().toString().substring(0, 8))
                .patientId(patientId)
                .severityCode("CODE_BLUE")
                .patientLocationLat(lat)
                .patientLocationLon(lon)
                .status("DISPATCHED")
                .responderType("AMBULANCE_PARAMEDIC")
                .etaMinutes(8)
                .build();

        return ResponseEntity.ok(task);
    }
}
