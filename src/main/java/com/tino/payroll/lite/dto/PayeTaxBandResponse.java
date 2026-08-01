package com.tino.payroll.lite.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PayeTaxBandResponse {
    private Long id;
    private BigDecimal lowerBound;
    private BigDecimal upperBound;
    private BigDecimal rate;
}
