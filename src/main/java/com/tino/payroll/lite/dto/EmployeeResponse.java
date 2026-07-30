package com.tino.payroll.lite.dto;

import com.tino.payroll.lite.enums.CurrencyCode;
import com.tino.payroll.lite.enums.EmployeeStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class EmployeeResponse {
    private Long id;
    private String employeeNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String jobTitle;
    private BigDecimal basicSalary;
    private CurrencyCode salaryCurrency;
    private LocalDate hireDate;
    private EmployeeStatus status;
    private Long userId;
    private boolean accountLinked;
}