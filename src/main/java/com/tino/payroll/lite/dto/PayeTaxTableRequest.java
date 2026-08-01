package com.tino.payroll.lite.dto;

import com.tino.payroll.lite.enums.CurrencyCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class PayeTaxTableRequest {

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
    private BigDecimal aidsLevyRate;

    @NotNull
    private Boolean active;

    @NotEmpty
    @Valid
    private List<PayeTaxBandRequest> bands;
}
