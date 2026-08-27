package com.healthcare.user.service;

import com.healthcare.user.model.DoctorProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorVerificationService {

    // In-memory mock/store repository for verification states
    private final ConcurrentHashMap<String, DoctorProfile> doctorStore = new ConcurrentHashMap<>();

    public DoctorProfile submitVerification(String doctorId, String licenseNumber, String specialty, int experience) {
        DoctorProfile profile = DoctorProfile.builder()
                .doctorId(doctorId)
                .medicalLicenseNumber(licenseNumber)
                .specialty(specialty)
                .yearsOfExperience(experience)
                .verified(false)
                .build();
        doctorStore.put(doctorId, profile);
        log.info("Medical license submitted for doctor: {}, license: {}", doctorId, licenseNumber);
        return profile;
    }

    public Optional<DoctorProfile> verifyDoctor(String doctorId, String adminId) {
        DoctorProfile profile = doctorStore.get(doctorId);
        if (profile != null) {
            profile.setVerified(true);
            profile.setVerifiedByAdmin(adminId);
            profile.setVerificationTimestamp(ZonedDateTime.now());
            log.info("Doctor {} verified by Admin {}", doctorId, adminId);
            return Optional.of(profile);
        }
        return Optional.empty();
    }

    public Optional<DoctorProfile> getDoctorProfile(String doctorId) {
        return Optional.ofNullable(doctorStore.get(doctorId));
    }
}
