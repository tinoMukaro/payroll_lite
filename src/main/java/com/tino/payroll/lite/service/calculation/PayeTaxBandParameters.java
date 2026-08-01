package com.tino.payroll.lite.service.calculation;

import java.math.BigDecimal;

public record PayeTaxBandParameters(
        BigDecimal lowerBound,
        BigDecimal upperBound,
        BigDecimal rate
) {
}
