package com.tino.payroll.lite.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PayeTaxBandRequest {

    @NotNull
    @DecimalMin("0.0")
    @Digits(integer = 16, fraction = 2)
    private BigDecimal lowerBound;

    @DecimalMin("0.0")
    @Digits(integer = 16, fraction = 2)
    private BigDecimal upperBound;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    @Digits(integer = 1, fraction = 6)
    private BigDecimal rate;
}
