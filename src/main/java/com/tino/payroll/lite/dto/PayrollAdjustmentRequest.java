package com.tino.payroll.lite.dto;

import com.tino.payroll.lite.enums.PayrollAdjustmentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PayrollAdjustmentRequest {

    @NotNull(message = "Employee is required")
    private Long employeeId;

    @NotNull(message = "Adjustment type is required")
    private PayrollAdjustmentType type;

    @NotBlank(message = "Description is required")
    @Size(max = 100, message = "Description must not exceed 100 characters")
    private String description;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    @Digits(integer = 10, fraction = 2, message = "Amount must have at most 2 decimal places")
    private BigDecimal amount;

    private boolean taxable;
}
