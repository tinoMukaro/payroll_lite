package com.tino.payroll.lite.dto;

import com.tino.payroll.lite.enums.PayrollAdjustmentType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PayrollAdjustmentResponse {
    private Long id;
    private Long payrollRunId;
    private Long employeeId;
    private String employeeNumber;
    private String employeeName;
    private PayrollAdjustmentType type;
    private String description;
    private BigDecimal amount;
    private boolean taxable;
    private LocalDateTime createdAt;
}
