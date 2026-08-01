package com.tino.payroll.lite.service.calculation;

import com.tino.payroll.lite.entity.PayeTaxBand;
import com.tino.payroll.lite.entity.PayeTaxTable;
import com.tino.payroll.lite.enums.CurrencyCode;
import com.tino.payroll.lite.exception.PayrollConfigurationException;
import com.tino.payroll.lite.repository.PayeTaxTableRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayeTaxTableResolverTest {

    @Mock
    private PayeTaxTableRepository repository;

    private PayeTaxTableResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new PayeTaxTableResolver(repository);
    }

    @Test
    void resolvesAndSortsTheSingleApplicableTable() {
        LocalDate payrollDate = LocalDate.of(2026, 7, 31);
        PayeTaxTable table = table("PAYE-USD-2026");
        table.setBands(List.of(
                band(table, "100", null, "0.20"),
                band(table, "0", "100", "0")
        ));
        when(repository.findApplicableTables(CurrencyCode.USD, payrollDate))
                .thenReturn(List.of(table));

        PayeParameters result = resolver.resolve(CurrencyCode.USD, payrollDate);

        assertEquals("PAYE-USD-2026", result.ruleVersion());
        assertEquals(new BigDecimal("0.03"), result.aidsLevyRate());
        assertEquals(new BigDecimal("0"), result.bands().getFirst().lowerBound());
        assertEquals(new BigDecimal("100"), result.bands().getLast().lowerBound());
    }

    @Test
    void rejectsMissingConfiguration() {
        LocalDate payrollDate = LocalDate.of(2026, 7, 31);
        when(repository.findApplicableTables(CurrencyCode.USD, payrollDate))
                .thenReturn(List.of());

        PayrollConfigurationException exception = assertThrows(
                PayrollConfigurationException.class,
                () -> resolver.resolve(CurrencyCode.USD, payrollDate)
        );

        assertEquals(
                "No active PAYE tax table exists for USD on 2026-07-31",
                exception.getMessage()
        );
    }

    @Test
    void rejectsOverlappingConfigurationAndNamesTheVersions() {
        LocalDate payrollDate = LocalDate.of(2026, 7, 31);
        when(repository.findApplicableTables(CurrencyCode.USD, payrollDate))
                .thenReturn(List.of(table("PAYE-B"), table("PAYE-A")));

        PayrollConfigurationException exception = assertThrows(
                PayrollConfigurationException.class,
                () -> resolver.resolve(CurrencyCode.USD, payrollDate)
        );

        assertEquals(
                "Overlapping PAYE tax tables exist for USD on 2026-07-31: PAYE-A, PAYE-B",
                exception.getMessage()
        );
    }

    @Test
    void rejectsMissingInputsBeforeQuerying() {
        assertThrows(IllegalArgumentException.class,
                () -> resolver.resolve(null, LocalDate.of(2026, 7, 31)));
        assertThrows(IllegalArgumentException.class,
                () -> resolver.resolve(CurrencyCode.USD, null));

        verifyNoInteractions(repository);
    }

    private PayeTaxTable table(String version) {
        return PayeTaxTable.builder()
                .version(version)
                .currency(CurrencyCode.USD)
                .effectiveFrom(LocalDate.of(2026, 1, 1))
                .aidsLevyRate(new BigDecimal("0.03"))
                .active(true)
                .build();
    }

    private PayeTaxBand band(
            PayeTaxTable table,
            String lower,
            String upper,
            String rate
    ) {
        return PayeTaxBand.builder()
                .taxTable(table)
                .lowerBound(new BigDecimal(lower))
                .upperBound(upper == null ? null : new BigDecimal(upper))
                .rate(new BigDecimal(rate))
                .build();
    }
}
