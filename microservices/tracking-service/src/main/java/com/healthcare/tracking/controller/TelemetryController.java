package com.healthcare.tracking.controller;

import com.healthcare.tracking.document.MedicalRecordIndexDocument;
import com.healthcare.tracking.document.PatientVitalsIndexDocument;
import com.healthcare.tracking.service.ElasticsearchMedicalSearchService;
import com.healthcare.tracking.websocket.PatientMonitorWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/telemetry")
@RequiredArgsConstructor
public class TelemetryController {

    private final ElasticsearchMedicalSearchService searchService;
    private final PatientMonitorWebSocketHandler webSocketHandler;

    @PostMapping("/vitals/ingest")
    public ResponseEntity<PatientVitalsIndexDocument> ingestVitals(
            @RequestBody PatientVitalsIndexDocument vitals) {

        if (vitals.getId() == null) {
            vitals.setId(UUID.randomUUID().toString());
        }
        if (vitals.getRecordedAt() == null) {
            vitals.setRecordedAt(Instant.now());
        }

        // Detect anomalies
        String anomaly = "NORMAL";
        if (vitals.getHeartRateBpm() > 120) {
            anomaly = "TACHYCARDIA";
        } else if (vitals.getOxygenSaturationSpO2() < 90.0) {
            anomaly = "HYPOXIA";
        } else if (vitals.getSystolicBp() >= 180) {
            anomaly = "HYPERTENSIVE_CRISIS";
        } else if (vitals.getBloodGlucoseMgDl() < 70.0) {
            anomaly = "HYPOGLYCEMIA";
        }
        vitals.setAnomalyFlag(anomaly);

        searchService.indexVital(vitals);

        // Broadcast to live ICU WebSocket monitors
        webSocketHandler.broadcastVitals(String.format(
                "{\"patientId\":\"%s\",\"bpm\":%d,\"spo2\":%.1f,\"bp\":\"%d/%d\",\"anomaly\":\"%s\"}",
                vitals.getPatientId(), vitals.getHeartRateBpm(), vitals.getOxygenSaturationSpO2(),
                vitals.getSystolicBp(), vitals.getDiastolicBp(), vitals.getAnomalyFlag()
        ));

        return ResponseEntity.ok(vitals);
    }

    @GetMapping("/search/medical-records")
    public ResponseEntity<ElasticsearchMedicalSearchService.SearchResultStats> searchMedicalRecords(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String anomalyFilter) {

        return ResponseEntity.ok(searchService.searchMedicalRecordsAndAnomalies(query, anomalyFilter));
    }
}
