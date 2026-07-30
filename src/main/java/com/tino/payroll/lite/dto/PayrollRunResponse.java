package com.tino.payroll.lite.dto;

import com.tino.payroll.lite.enums.CurrencyCode;
import com.tino.payroll.lite.enums.PayrollStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PayrollRunResponse {
    private Long id;
    private Integer month;
    private Integer year;
    private CurrencyCode currency;
    private PayrollStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
