package com.tino.payroll.lite.dto;

import com.tino.payroll.lite.enums.CurrencyCode;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class PayeTaxTableResponse {
    private Long id;
    private String version;
    private CurrencyCode currency;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private BigDecimal aidsLevyRate;
    private boolean active;
    private List<PayeTaxBandResponse> bands;
}
