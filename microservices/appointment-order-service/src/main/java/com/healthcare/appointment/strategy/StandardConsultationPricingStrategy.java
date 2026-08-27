package com.healthcare.appointment.strategy;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component("StandardConsultationPricingStrategy")
public class StandardConsultationPricingStrategy implements ConsultationPricingStrategy {

    @Override
    public String getStrategyName() {
        return "STANDARD_CONSULTATION";
    }

    @Override
    public BigDecimal calculateFinalCopay(BigDecimal baseFee, double insuranceCoveragePercentage) {
        // Standard consultation flat $25 copay or remaining percentage, whichever is lower
        BigDecimal standardCopay = new BigDecimal("25.00");
        BigDecimal calculated = baseFee.multiply(BigDecimal.valueOf(1.0 - insuranceCoveragePercentage));
        return standardCopay.min(calculated).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getExplanation() {
        return "General practitioner consultation with flat tiered copay capped at $25.00.";
    }
}
