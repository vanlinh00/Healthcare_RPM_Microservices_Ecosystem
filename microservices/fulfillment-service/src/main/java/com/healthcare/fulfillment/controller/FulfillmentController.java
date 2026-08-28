package com.healthcare.fulfillment.controller;

import com.healthcare.fulfillment.model.SampleTransitRecord;
import com.healthcare.fulfillment.service.DigitalPodService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fulfillment")
@RequiredArgsConstructor
public class FulfillmentController {

    private final DigitalPodService digitalPodService;

    @PostMapping("/digital-pod")
    public ResponseEntity<DigitalPodService.ProofOfDeliveryReceipt> issueProofOfDelivery(
            @RequestParam("prescriptionId") String prescriptionId,
            @RequestParam("patientId") String patientId,
            @RequestParam("recipientName") String recipientName) {

        DigitalPodService.ProofOfDeliveryReceipt pod = digitalPodService.generateProofOfDelivery(
                prescriptionId, patientId, recipientName
        );
        return ResponseEntity.ok(pod);
    }

    @PostMapping("/cold-chain/telemetry")
    public ResponseEntity<SampleTransitRecord> logColdChain(
            @RequestParam("recordId") String recordId,
            @RequestParam("sampleType") String sampleType,
            @RequestParam("patientId") String patientId,
            @RequestParam("temperature") double temperature,
            @RequestParam("minTemp") double minTemp,
            @RequestParam("maxTemp") double maxTemp) {

        SampleTransitRecord record = digitalPodService.logColdChainTelemetry(
                recordId, sampleType, patientId, temperature, minTemp, maxTemp
        );
        return ResponseEntity.ok(record);
    }
}
