package com.tino.payroll.lite.service.calculation;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class NssaCalculator {

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;
    private static final BigDecimal ONE = BigDecimal.ONE;

    public NssaCalculation calculate(
            BigDecimal pensionableEarnings,
            NssaParameters parameters
    ) {
        validate(pensionableEarnings, parameters);

        BigDecimal cappedEarnings = pensionableEarnings
                .min(parameters.pensionableEarningsCeiling());

        BigDecimal employeeContribution = contribution(
                cappedEarnings,
                parameters.employeeRate()
        );
        BigDecimal employerContribution = contribution(
                cappedEarnings,
                parameters.employerRate()
        );

        return new NssaCalculation(
                money(cappedEarnings),
                employeeContribution,
                employerContribution,
                parameters.ruleVersion()
        );
    }

    private BigDecimal contribution(BigDecimal earnings, BigDecimal rate) {
        return money(earnings.multiply(rate));
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, MONEY_ROUNDING);
    }

    private void validate(BigDecimal earnings, NssaParameters parameters) {
        if (earnings == null) {
            throw new IllegalArgumentException("Pensionable earnings are required");
        }
        if (parameters == null) {
            throw new IllegalArgumentException("NSSA parameters are required");
        }
        if (earnings.signum() < 0) {
            throw new IllegalArgumentException("Pensionable earnings cannot be negative");
        }
        validateRate("Employee rate", parameters.employeeRate());
        validateRate("Employer rate", parameters.employerRate());
        if (parameters.pensionableEarningsCeiling() == null
                || parameters.pensionableEarningsCeiling().signum() < 0) {
            throw new IllegalArgumentException("Pensionable earnings ceiling cannot be negative");
        }
        if (parameters.ruleVersion() == null || parameters.ruleVersion().isBlank()) {
            throw new IllegalArgumentException("NSSA rule version is required");
        }
    }

    private void validateRate(String name, BigDecimal rate) {
        if (rate == null || rate.signum() < 0 || rate.compareTo(ONE) > 0) {
            throw new IllegalArgumentException(name + " must be between 0 and 1");
        }
    }
}