package com.healthcare.appointment.strategy;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component("SpecialistPricingStrategy")
public class SpecialistPricingStrategy implements ConsultationPricingStrategy {

    @Override
    public String getStrategyName() {
        return "SPECIALIST_CONSULTATION";
    }

    @Override
    public BigDecimal calculateFinalCopay(BigDecimal baseFee, double insuranceCoveragePercentage) {
        // Specialist copay has $50 base floor + 15% coinsurance
        BigDecimal baseCoinsurance = baseFee.multiply(BigDecimal.valueOf(1.0 - insuranceCoveragePercentage));
        BigDecimal specialistFloor = new BigDecimal("50.00");
        return specialistFloor.max(baseCoinsurance).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getExplanation() {
        return "Specialist / Surgeon consultation requiring referrals with $50.00 specialist floor.";
    }
}
