package com.tino.payroll.lite.service.calculation;

import java.math.BigDecimal;
import java.util.List;

public record PayeParameters(
        String ruleVersion,
        BigDecimal aidsLevyRate,
        List<PayeTaxBandParameters> bands
) {

    public PayeParameters {
        bands = bands == null ? null : List.copyOf(bands);
    }
}
