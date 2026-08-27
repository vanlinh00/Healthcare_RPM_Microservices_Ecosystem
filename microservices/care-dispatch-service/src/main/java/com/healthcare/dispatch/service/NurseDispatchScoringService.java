package com.healthcare.dispatch.service;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
public class NurseDispatchScoringService {

    @Data
    @Builder
    public static class CaregiverCandidate {
        private String id;
        private String name;
        private String role; // NURSE, DOCTOR
        private String specialty; // ICU, CARDIOLOGY, PEDIATRICS, GERIATRICS
        private double latitude;
        private double longitude;
        private double rating; // 1.0 - 5.0
        private int activeCases;
        private boolean isAvailable;
    }

    @Data
    @Builder
    public static class DispatchRecommendation {
        private CaregiverCandidate candidate;
        private double totalScore;
        private double distanceKm;
        private int estimatedEtaMinutes;
        private String matchRationale;
    }

    /**
     * Calculates optimal dispatch candidate using weighted scoring:
     * - Proximity score (40% weight)
     * - Specialty exact match (30% weight)
     * - Workload balance (active cases inverse) (20% weight)
     * - Clinical rating (10% weight)
     */
    public DispatchRecommendation findOptimalResponder(
            double patientLat,
            double patientLon,
            String requiredSpecialty,
            List<CaregiverCandidate> candidates) {

        return candidates.stream()
                .filter(CaregiverCandidate::isAvailable)
                .map(c -> {
                    double dist = calculateHaversineDistance(patientLat, patientLon, c.getLatitude(), c.getLongitude());
                    double proximityScore = Math.max(0, 100 - (dist * 5)); // 0-100 pts
                    double specialtyScore = requiredSpecialty.equalsIgnoreCase(c.getSpecialty()) ? 100.0 : 40.0;
                    double workloadScore = Math.max(0, 100 - (c.getActiveCases() * 20));
                    double ratingScore = (c.getRating() / 5.0) * 100;

                    double total = (proximityScore * 0.40) + (specialtyScore * 0.30) + (workloadScore * 0.20) + (ratingScore * 0.10);
                    int eta = (int) Math.max(5, Math.round(dist * 3.5)); // rough estimate

                    String rationale = String.format("Score: %.1f/100 (Distance: %.1f km, ETA: %d min, Match: %s)",
                            total, dist, eta, c.getSpecialty());

                    return DispatchRecommendation.builder()
                            .candidate(c)
                            .totalScore(total)
                            .distanceKm(dist)
                            .estimatedEtaMinutes(eta)
                            .matchRationale(rationale)
                            .build();
                })
                .max(Comparator.comparingDouble(DispatchRecommendation::getTotalScore))
                .orElse(null);
    }

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in KM
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round((R * c) * 10.0) / 10.0;
    }
}
