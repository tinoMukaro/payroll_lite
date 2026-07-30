package com.tino.payroll.lite.dto;

import com.tino.payroll.lite.enums.CurrencyCode;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class NssaRuleRequest {

    @NotBlank
    private String version;

    @NotNull
    private CurrencyCode currency;

    @NotNull
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    @Digits(integer = 1, fraction = 6)
    private BigDecimal employeeRate;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    @Digits(integer = 1, fraction = 6)
    private BigDecimal employerRate;

    @NotNull
    @Positive
    @Digits(integer = 16, fraction = 2)
    private BigDecimal pensionableEarningsCeiling;

    @NotNull
    private Boolean active;
}