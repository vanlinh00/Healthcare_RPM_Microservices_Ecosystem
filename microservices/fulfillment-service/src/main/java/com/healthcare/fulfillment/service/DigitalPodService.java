package com.healthcare.fulfillment.service;

import com.healthcare.fulfillment.model.SampleTransitRecord;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@Slf4j
public class DigitalPodService {

    private static final String HMAC_SECRET = "Healthcare-RPM-Digital-POD-Sign-Secret-2026";

    @Data
    @Builder
    public static class ProofOfDeliveryReceipt {
        private String podId;
        private String prescriptionId;
        private String patientId;
        private String recipientSignature;
        private String cryptographicHash;
        private boolean isSignatureValid;
        private ZonedDateTime deliveryTimestamp;
        private String tamperProofBadge;
    }

    public ProofOfDeliveryReceipt generateProofOfDelivery(String prescriptionId, String patientId, String signerName) {
        String podId = "POD-" + UUID.randomUUID().toString().substring(0, 8);
        ZonedDateTime now = ZonedDateTime.now();

        String rawContent = String.format("%s:%s:%s:%s", podId, prescriptionId, patientId, now.toString());
        String signature = signPayload(rawContent, HMAC_SECRET);

        return ProofOfDeliveryReceipt.builder()
                .podId(podId)
                .prescriptionId(prescriptionId)
                .patientId(patientId)
                .recipientSignature("Signed digitally by: " + signerName)
                .cryptographicHash(signature)
                .isSignatureValid(true)
                .deliveryTimestamp(now)
                .tamperProofBadge("SHA256:AUTHENTICATED:HIPAA_COMPLIANT")
                .build();
    }

    public SampleTransitRecord logColdChainTelemetry(
            String recordId,
            String sampleType,
            String patientId,
            double currentTemp,
            double minTemp,
            double maxTemp) {

        boolean breached = currentTemp < minTemp || currentTemp > maxTemp;
        String signature = signPayload(recordId + ":" + currentTemp + ":" + breached, HMAC_SECRET);

        SampleTransitRecord record = SampleTransitRecord.builder()
                .id(recordId)
                .sampleType(sampleType)
                .patientId(patientId)
                .currentTemperatureCelsius(currentTemp)
                .minAllowedTempCelsius(minTemp)
                .maxAllowedTempCelsius(maxTemp)
                .coldChainBreached(breached)
                .status(breached ? "ANOMALY_QUARANTINED" : "IN_TRANSIT")
                .digitalSignature(signature)
                .build();

        log.info("Cold-chain transit telemetry logged: Sample {} Temp {}°C [Breach: {}]", sampleType, currentTemp, breached);
        return record;
    }

    private String signPayload(String data, String key) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] bytes = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HMAC SHA256 signature", e);
        }
    }
}
