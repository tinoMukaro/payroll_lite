package com.tino.payroll.lite.dto;

import com.tino.payroll.lite.enums.CurrencyCode;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PayslipResponse {
    private Long id;
    private Long employeeId;
    private String employeeNumber;
    private String employeeName;
    private Long payrollRunId;
    private Integer month;
    private Integer year;
    private CurrencyCode currency;
    private BigDecimal basicSalary;
    private BigDecimal grossSalary;
    private BigDecimal pensionableEarnings;
    private BigDecimal employeeNssaContribution;
    private BigDecimal employerNssaContribution;
    private String nssaRuleVersion;
    private BigDecimal payeDeduction;
    private BigDecimal taxableIncome;
    private BigDecimal incomeTaxBeforeCredits;
    private BigDecimal taxCreditsApplied;
    private BigDecimal aidsLevy;
    private String payeRuleVersion;
    private BigDecimal totalDeductions;
    private BigDecimal netSalary;
    private LocalDateTime createdAt;
}
