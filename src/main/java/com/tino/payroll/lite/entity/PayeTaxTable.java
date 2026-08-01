package com.tino.payroll.lite.entity;

import com.tino.payroll.lite.enums.CurrencyCode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "paye_tax_tables",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_paye_table_currency_effective_from",
                columnNames = {"currency", "effective_from"}
        )
)
public class PayeTaxTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_version", nullable = false, unique = true, length = 100)
    private String version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CurrencyCode currency;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "aids_levy_rate", nullable = false, precision = 8, scale = 6)
    private BigDecimal aidsLevyRate;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @OneToMany(
            mappedBy = "taxTable",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @OrderBy("lowerBound ASC")
    @Builder.Default
    private List<PayeTaxBand> bands = new ArrayList<>();
}
