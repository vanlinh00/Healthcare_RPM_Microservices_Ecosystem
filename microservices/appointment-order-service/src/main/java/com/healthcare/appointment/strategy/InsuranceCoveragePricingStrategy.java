package com.healthcare.appointment.strategy;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component("InsuranceCoveragePricingStrategy")
public class InsuranceCoveragePricingStrategy implements ConsultationPricingStrategy {

    @Override
    public String getStrategyName() {
        return "COMPREHENSIVE_INSURANCE_COVERAGE";
    }

    @Override
    public BigDecimal calculateFinalCopay(BigDecimal baseFee, double insuranceCoveragePercentage) {
        BigDecimal patientShare = baseFee.multiply(BigDecimal.valueOf(1.0 - Math.min(1.0, insuranceCoveragePercentage)));
        return patientShare.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getExplanation() {
        return "Comprehensive in-network HMO/PPO plan dynamic coverage calculation.";
    }
}
