package com.tino.payroll.lite.dto;

import com.tino.payroll.lite.enums.CurrencyCode;
import com.tino.payroll.lite.enums.PayrollAdjustmentType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class RecurringPayItemResponse {
    private Long id;
    private Long employeeId;
    private String employeeNumber;
    private String employeeName;
    private CurrencyCode currency;
    private PayrollAdjustmentType type;
    private String description;
    private BigDecimal amount;
    private boolean taxable;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
