package com.tino.payroll.lite.entity;

import com.tino.payroll.lite.enums.CurrencyCode;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "payslips",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_payslip_employee_payroll_run",
                columnNames = {"employee_id", "payroll_run_id"}
        )
)
public class Payslip {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payroll_run_id", nullable = false)
    private PayrollRun payrollRun;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal basicSalary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CurrencyCode currency;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal grossSalary;

    @Column(name = "pensionable_earnings", nullable = false, precision = 12, scale = 2)
    private BigDecimal pensionableEarnings;

    @Column(name = "nssa_deduction", nullable = false, precision = 12, scale = 2)
    private BigDecimal employeeNssaContribution;

    @Column(name = "employer_nssa_contribution", nullable = false, precision = 12, scale = 2)
    private BigDecimal employerNssaContribution;

    @Column(name = "nssa_rule_version", nullable = false, length = 100)
    private String nssaRuleVersion;
    
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal payeDeduction;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalDeductions;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal netSalary;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (pensionableEarnings == null) pensionableEarnings = BigDecimal.ZERO;
        if (employeeNssaContribution == null) employeeNssaContribution = BigDecimal.ZERO;
        if (employerNssaContribution == null) employerNssaContribution = BigDecimal.ZERO;
        if (nssaRuleVersion == null) nssaRuleVersion = "NOT_APPLIED";
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
    
}
