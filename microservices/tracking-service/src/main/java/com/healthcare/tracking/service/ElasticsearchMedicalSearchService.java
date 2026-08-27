package com.healthcare.tracking.service;

import com.healthcare.tracking.document.MedicalRecordIndexDocument;
import com.healthcare.tracking.document.PatientVitalsIndexDocument;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchMedicalSearchService {

    // High-performance search cache / indexed documents store
    private final List<MedicalRecordIndexDocument> medicalRecordStore = new ArrayList<>();
    private final List<PatientVitalsIndexDocument> vitalsStore = new ArrayList<>();

    @Data
    @Builder
    public static class SearchResultStats {
        private long totalHits;
        private double tookMs;
        private List<MedicalRecordIndexDocument> records;
        private List<PatientVitalsIndexDocument> anomalyVitals;
    }

    public void indexMedicalRecord(MedicalRecordIndexDocument record) {
        medicalRecordStore.add(record);
        log.info("Elasticsearch: Indexed medical record {} for patient {}", record.getId(), record.getPatientId());
    }

    public void indexVital(PatientVitalsIndexDocument vital) {
        vitalsStore.add(vital);
    }

    public SearchResultStats searchMedicalRecordsAndAnomalies(String query, String anomalyFilter) {
        long startTime = System.nanoTime();
        String lower = query != null ? query.toLowerCase() : "";

        List<MedicalRecordIndexDocument> matchedRecords = medicalRecordStore.stream()
                .filter(r -> lower.isEmpty()
                        || (r.getDiagnosis() != null && r.getDiagnosis().toLowerCase().contains(lower))
                        || (r.getClinicalNotes() != null && r.getClinicalNotes().toLowerCase().contains(lower))
                        || (r.getSymptoms() != null && r.getSymptoms().stream().anyMatch(s -> s.toLowerCase().contains(lower)))
                        || (r.getIcd10Codes() != null && r.getIcd10Codes().stream().anyMatch(c -> c.toLowerCase().contains(lower)))
                )
                .collect(Collectors.toList());

        List<PatientVitalsIndexDocument> matchedVitals = vitalsStore.stream()
                .filter(v -> anomalyFilter == null || anomalyFilter.isEmpty() || anomalyFilter.equalsIgnoreCase("ALL")
                        || (v.getAnomalyFlag() != null && v.getAnomalyFlag().equalsIgnoreCase(anomalyFilter)))
                .collect(Collectors.toList());

        double tookMs = (System.nanoTime() - startTime) / 1_000_000.0;

        return SearchResultStats.builder()
                .totalHits(matchedRecords.size() + matchedVitals.size())
                .tookMs(Math.round(tookMs * 100.0) / 100.0)
                .records(matchedRecords)
                .anomalyVitals(matchedVitals)
                .build();
    }
}
