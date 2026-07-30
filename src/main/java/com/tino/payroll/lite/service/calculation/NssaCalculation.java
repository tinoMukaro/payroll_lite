package com.tino.payroll.lite.service.calculation;

import java.math.BigDecimal;

public record NssaCalculation(
        BigDecimal pensionableEarnings,
        BigDecimal employeeContribution,
        BigDecimal employerContribution,
        String ruleVersion
) {
}