package com.healthcare.appointment.strategy;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class ConsultationPricingContext {

    private final Map<String, ConsultationPricingStrategy> strategies;

    public ConsultationPricingContext(Map<String, ConsultationPricingStrategy> strategies) {
        this.strategies = strategies;
    }

    public BigDecimal calculateCopay(String strategyType, BigDecimal baseFee, double insuranceCoverage) {
        ConsultationPricingStrategy strategy = strategies.getOrDefault(
                strategyType,
                strategies.get("StandardConsultationPricingStrategy")
        );

        if (strategy == null) {
            return baseFee;
        }

        return strategy.calculateFinalCopay(baseFee, insuranceCoverage);
    }
}
