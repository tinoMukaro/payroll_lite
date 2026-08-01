package com.tino.payroll.lite.service.calculation;

import java.math.BigDecimal;

public record PayeCalculation(
        BigDecimal taxableIncome,
        BigDecimal incomeTaxBeforeCredits,
        BigDecimal taxCreditsApplied,
        BigDecimal incomeTaxAfterCredits,
        BigDecimal aidsLevy,
        BigDecimal totalPaye,
        String ruleVersion
) {
}
