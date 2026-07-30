package com.tino.payroll.lite.entity;

import com.tino.payroll.lite.enums.CurrencyCode;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "nssa_rules",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_nssa_rule_currency_effective_from",
                columnNames = {"currency", "effective_from"}
        )
)
public class NssaRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_version", nullable = false, unique = true)
    private String version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CurrencyCode currency;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "employee_rate", nullable = false, precision = 8, scale = 6)
    private BigDecimal employeeRate;

    @Column(name = "employer_rate", nullable = false, precision = 8, scale = 6)
    private BigDecimal employerRate;

    @Column(name = "pensionable_earnings_ceiling", nullable = false, precision = 18, scale = 2)
    private BigDecimal pensionableEarningsCeiling;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}