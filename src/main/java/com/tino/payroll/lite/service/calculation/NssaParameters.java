package com.tino.payroll.lite.service.calculation;

import java.math.BigDecimal;

public record NssaParameters(
        BigDecimal employeeRate,
        BigDecimal employerRate,
        BigDecimal pensionableEarningsCeiling,
        String ruleVersion
) {
}