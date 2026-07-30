package com.tino.payroll.lite.service.calculation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class NssaCalculatorTest {

    private NssaCalculator calculator;
    private NssaParameters parameters;

    @BeforeEach
    void setUp() {
        calculator = new NssaCalculator();
        parameters = new NssaParameters(
                new BigDecimal("0.045"),
                new BigDecimal("0.045"),
                new BigDecimal("1000.00"),
                "TEST-USD-2026-01"
        );
    }

    @Test
    void calculatesContributionsBelowTheCeiling() {
        NssaCalculation result = calculator.calculate(new BigDecimal("800.00"), parameters);

        assertEquals(new BigDecimal("800.00"), result.pensionableEarnings());
        assertEquals(new BigDecimal("36.00"), result.employeeContribution());
        assertEquals(new BigDecimal("36.00"), result.employerContribution());
        assertEquals("TEST-USD-2026-01", result.ruleVersion());
    }

    @Test
    void calculatesContributionsAtTheCeiling() {
        NssaCalculation result = calculator.calculate(new BigDecimal("1000.00"), parameters);

        assertEquals(new BigDecimal("1000.00"), result.pensionableEarnings());
        assertEquals(new BigDecimal("45.00"), result.employeeContribution());
        assertEquals(new BigDecimal("45.00"), result.employerContribution());
    }

    @Test
    void capsContributionsAboveTheCeiling() {
        NssaCalculation result = calculator.calculate(new BigDecimal("2500.00"), parameters);

        assertEquals(new BigDecimal("1000.00"), result.pensionableEarnings());
        assertEquals(new BigDecimal("45.00"), result.employeeContribution());
        assertEquals(new BigDecimal("45.00"), result.employerContribution());
    }

    @Test
    void roundsContributionsToTwoDecimalPlaces() {
        NssaCalculation result = calculator.calculate(new BigDecimal("123.45"), parameters);

        assertEquals(new BigDecimal("5.56"), result.employeeContribution());
        assertEquals(new BigDecimal("5.56"), result.employerContribution());
    }

    @Test
    void rejectsNegativeEarnings() {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(new BigDecimal("-0.01"), parameters));
    }

    @Test
    void rejectsRatesOutsideTheDecimalRange() {
        NssaParameters invalid = new NssaParameters(
                new BigDecimal("4.5"),
                new BigDecimal("0.045"),
                new BigDecimal("1000.00"),
                "INVALID"
        );

        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(new BigDecimal("500.00"), invalid));
    }
}