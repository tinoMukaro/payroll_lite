package com.tino.payroll.lite.service.calculation;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Component
public class PayeCalculator {

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;
    private static final BigDecimal ONE = BigDecimal.ONE;

    public PayeCalculation calculate(
            BigDecimal taxableIncome,
            BigDecimal taxCredits,
            PayeParameters parameters
    ) {
        validate(taxableIncome, taxCredits, parameters);

        List<PayeTaxBandParameters> bands = parameters.bands().stream()
                .sorted(Comparator.comparing(PayeTaxBandParameters::lowerBound))
                .toList();

        BigDecimal incomeTax = BigDecimal.ZERO;
        for (PayeTaxBandParameters band : bands) {
            if (taxableIncome.compareTo(band.lowerBound()) <= 0) {
                break;
            }

            BigDecimal bandEnd = band.upperBound() == null
                    ? taxableIncome
                    : taxableIncome.min(band.upperBound());
            BigDecimal amountInBand = bandEnd.subtract(band.lowerBound());
            if (amountInBand.signum() > 0) {
                incomeTax = incomeTax.add(amountInBand.multiply(band.rate()));
            }
        }

        BigDecimal roundedTaxBeforeCredits = money(incomeTax);
        BigDecimal roundedCredits = money(taxCredits);
        BigDecimal creditsApplied = roundedCredits.min(roundedTaxBeforeCredits);
        BigDecimal taxAfterCredits = roundedTaxBeforeCredits.subtract(creditsApplied);
        BigDecimal aidsLevy = money(taxAfterCredits.multiply(parameters.aidsLevyRate()));

        return new PayeCalculation(
                money(taxableIncome),
                roundedTaxBeforeCredits,
                creditsApplied,
                taxAfterCredits,
                aidsLevy,
                taxAfterCredits.add(aidsLevy),
                parameters.ruleVersion()
        );
    }

    private void validate(
            BigDecimal taxableIncome,
            BigDecimal taxCredits,
            PayeParameters parameters
    ) {
        requireNonNegative("Taxable income", taxableIncome);
        requireNonNegative("Tax credits", taxCredits);
        if (parameters == null) {
            throw new IllegalArgumentException("PAYE parameters are required");
        }
        if (parameters.ruleVersion() == null || parameters.ruleVersion().isBlank()) {
            throw new IllegalArgumentException("PAYE rule version is required");
        }
        validateRate("AIDS levy rate", parameters.aidsLevyRate());
        validateBands(parameters.bands());
    }

    private void validateBands(List<PayeTaxBandParameters> bands) {
        if (bands == null || bands.isEmpty()) {
            throw new IllegalArgumentException("At least one PAYE tax band is required");
        }

        List<PayeTaxBandParameters> sortedBands = bands.stream()
                .sorted(Comparator.comparing(PayeTaxBandParameters::lowerBound,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .toList();

        BigDecimal expectedLowerBound = BigDecimal.ZERO;
        for (int index = 0; index < sortedBands.size(); index++) {
            PayeTaxBandParameters band = sortedBands.get(index);
            if (band.lowerBound() == null || band.lowerBound().signum() < 0) {
                throw new IllegalArgumentException("PAYE band lower bounds cannot be negative");
            }
            if (band.lowerBound().compareTo(expectedLowerBound) != 0) {
                throw new IllegalArgumentException("PAYE tax bands must be contiguous and start at zero");
            }
            validateRate("PAYE band rate", band.rate());

            boolean finalBand = index == sortedBands.size() - 1;
            if (band.upperBound() == null) {
                if (!finalBand) {
                    throw new IllegalArgumentException("Only the final PAYE tax band can be open-ended");
                }
                continue;
            }
            if (band.upperBound().compareTo(band.lowerBound()) <= 0) {
                throw new IllegalArgumentException("PAYE band upper bounds must exceed lower bounds");
            }
            if (finalBand) {
                throw new IllegalArgumentException("The final PAYE tax band must be open-ended");
            }
            expectedLowerBound = band.upperBound();
        }
    }

    private void requireNonNegative(String name, BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        if (value.signum() < 0) {
            throw new IllegalArgumentException(name + " cannot be negative");
        }
    }

    private void validateRate(String name, BigDecimal rate) {
        if (rate == null || rate.signum() < 0 || rate.compareTo(ONE) > 0) {
            throw new IllegalArgumentException(name + " must be between 0 and 1");
        }
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, MONEY_ROUNDING);
    }
}
