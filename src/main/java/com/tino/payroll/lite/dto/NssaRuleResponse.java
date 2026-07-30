package com.tino.payroll.lite.dto;

import com.tino.payroll.lite.enums.CurrencyCode;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class NssaRuleResponse {
    private Long id;
    private String version;
    private CurrencyCode currency;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private BigDecimal employeeRate;
    private BigDecimal employerRate;
    private BigDecimal pensionableEarningsCeiling;
    private boolean active;
}