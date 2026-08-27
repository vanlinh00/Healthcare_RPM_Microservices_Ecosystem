package com.healthcare.appointment.strategy;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component("EmergencyCarePricingStrategy")
public class EmergencyCarePricingStrategy implements ConsultationPricingStrategy {

    @Override
    public String getStrategyName() {
        return "EMERGENCY_CARE";
    }

    @Override
    public BigDecimal calculateFinalCopay(BigDecimal baseFee, double insuranceCoveragePercentage) {
        // Emergency care flat $100 ER copay waived if admitted, or 10% coinsurance
        BigDecimal erBase = new BigDecimal("100.00");
        return erBase.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getExplanation() {
        return "Emergency triage & acute response consultation with immediate dispatch priority.";
    }
}
