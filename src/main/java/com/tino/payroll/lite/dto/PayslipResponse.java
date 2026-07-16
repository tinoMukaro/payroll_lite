package com.tino.payroll.lite.dto;

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
    private BigDecimal basicSalary;
    private BigDecimal grossSalary;
    private BigDecimal nssaDeduction;
    private BigDecimal payeDeduction;
    private BigDecimal totalDeductions;
    private BigDecimal netSalary;
    private LocalDateTime createdAt;
}
