package com.healthcare.appointment.strategy;

import java.math.BigDecimal;

public interface ConsultationPricingStrategy {
    String getStrategyName();
    BigDecimal calculateFinalCopay(BigDecimal baseFee, double insuranceCoveragePercentage);
    String getExplanation();
}
