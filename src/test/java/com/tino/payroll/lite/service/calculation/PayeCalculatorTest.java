package com.tino.payroll.lite.service.calculation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PayeCalculatorTest {

    private PayeCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new PayeCalculator();
    }

    @Test
    void calculatesProgressiveTaxAndAidsLevyAcrossSeveralBands() {
        PayeCalculation result = calculator.calculate(
                new BigDecimal("1800.00"),
                BigDecimal.ZERO,
                parameters()
        );

        assertMoney("1800.00", result.taxableIncome());
        assertMoney("455.00", result.incomeTaxBeforeCredits());
        assertMoney("0.00", result.taxCreditsApplied());
        assertMoney("455.00", result.incomeTaxAfterCredits());
        assertMoney("13.65", result.aidsLevy());
        assertMoney("468.65", result.totalPaye());
        assertEquals("TEST-PAYE-USD-2026", result.ruleVersion());
    }

    @Test
    void appliesCreditsBeforeCalculatingAidsLevy() {
        PayeCalculation result = calculator.calculate(
                new BigDecimal("1800.00"),
                new BigDecimal("100.00"),
                parameters()
        );

        assertMoney("455.00", result.incomeTaxBeforeCredits());
        assertMoney("100.00", result.taxCreditsApplied());
        assertMoney("355.00", result.incomeTaxAfterCredits());
        assertMoney("10.65", result.aidsLevy());
        assertMoney("365.65", result.totalPaye());
    }

    @Test
    void capsCreditsAtIncomeTaxSoPayeCannotBecomeNegative() {
        PayeCalculation result = calculator.calculate(
                new BigDecimal("1800.00"),
                new BigDecimal("1000.00"),
                parameters()
        );

        assertMoney("455.00", result.taxCreditsApplied());
        assertMoney("0.00", result.incomeTaxAfterCredits());
        assertMoney("0.00", result.aidsLevy());
        assertMoney("0.00", result.totalPaye());
    }

    @Test
    void respectsBandBoundaries() {
        PayeCalculation taxFree = calculator.calculate(
                new BigDecimal("100.00"), BigDecimal.ZERO, parameters()
        );
        PayeCalculation secondBoundary = calculator.calculate(
                new BigDecimal("300.00"), BigDecimal.ZERO, parameters()
        );

        assertMoney("0.00", taxFree.totalPaye());
        assertMoney("40.00", secondBoundary.incomeTaxBeforeCredits());
        assertMoney("1.20", secondBoundary.aidsLevy());
        assertMoney("41.20", secondBoundary.totalPaye());
    }

    @Test
    void roundsTaxAndLevyToTwoDecimalPlaces() {
        PayeCalculation result = calculator.calculate(
                new BigDecimal("100.03"), BigDecimal.ZERO, parameters()
        );

        assertMoney("0.01", result.incomeTaxBeforeCredits());
        assertMoney("0.00", result.aidsLevy());
        assertMoney("0.01", result.totalPaye());
    }

    @Test
    void acceptsBandsInAnyInputOrderWithoutChangingTheSourceList() {
        List<PayeTaxBandParameters> bands = new ArrayList<>(parameters().bands());
        PayeTaxBandParameters first = bands.removeFirst();
        bands.add(first);
        PayeParameters unsorted = new PayeParameters(
                "TEST-PAYE-USD-2026",
                new BigDecimal("0.03"),
                bands
        );

        PayeCalculation result = calculator.calculate(
                new BigDecimal("1800.00"), BigDecimal.ZERO, unsorted
        );

        assertMoney("468.65", result.totalPaye());
        assertEquals(first, bands.getLast());
    }

    @Test
    void rejectsMissingGappedOrNonTerminatingBandConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(
                BigDecimal.TEN,
                BigDecimal.ZERO,
                new PayeParameters("TEST", new BigDecimal("0.03"), List.of())
        ));

        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(
                BigDecimal.TEN,
                BigDecimal.ZERO,
                new PayeParameters("TEST", new BigDecimal("0.03"), List.of(
                        band("0", "100", "0"),
                        band("101", null, "0.20")
                ))
        ));

        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(
                BigDecimal.TEN,
                BigDecimal.ZERO,
                new PayeParameters("TEST", new BigDecimal("0.03"), List.of(
                        band("0", "100", "0"),
                        band("100", "300", "0.20")
                ))
        ));
    }

    @Test
    void rejectsNegativeInputsAndRatesOutsideTheDecimalRange() {
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(
                new BigDecimal("-0.01"), BigDecimal.ZERO, parameters()
        ));
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(
                BigDecimal.TEN, new BigDecimal("-0.01"), parameters()
        ));
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(
                BigDecimal.TEN,
                BigDecimal.ZERO,
                new PayeParameters("TEST", new BigDecimal("1.01"), parameters().bands())
        ));
    }

    private PayeParameters parameters() {
        return new PayeParameters(
                "TEST-PAYE-USD-2026",
                new BigDecimal("0.03"),
                List.of(
                        band("0", "100", "0"),
                        band("100", "300", "0.20"),
                        band("300", "1000", "0.25"),
                        band("1000", "2000", "0.30"),
                        band("2000", "3000", "0.35"),
                        band("3000", null, "0.40")
                )
        );
    }

    private PayeTaxBandParameters band(String lower, String upper, String rate) {
        return new PayeTaxBandParameters(
                new BigDecimal(lower),
                upper == null ? null : new BigDecimal(upper),
                new BigDecimal(rate)
        );
    }

    private void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
