package com.tino.payroll.lite.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "paye_tax_bands",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_paye_band_table_lower_bound",
                columnNames = {"tax_table_id", "lower_bound"}
        )
)
public class PayeTaxBand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tax_table_id", nullable = false)
    private PayeTaxTable taxTable;

    @Column(name = "lower_bound", nullable = false, precision = 18, scale = 2)
    private BigDecimal lowerBound;

    @Column(name = "upper_bound", precision = 18, scale = 2)
    private BigDecimal upperBound;

    @Column(name = "tax_rate", nullable = false, precision = 8, scale = 6)
    private BigDecimal rate;
}
