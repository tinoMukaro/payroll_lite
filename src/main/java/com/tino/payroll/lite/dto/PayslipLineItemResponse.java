package com.tino.payroll.lite.dto;

import com.tino.payroll.lite.enums.PayrollAdjustmentType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PayslipLineItemResponse {
    private PayrollAdjustmentType type;
    private String description;
    private BigDecimal amount;
    private boolean taxable;
}
