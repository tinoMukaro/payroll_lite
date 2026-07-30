package com.tino.payroll.lite.service.calculation;

import com.tino.payroll.lite.entity.NssaRule;
import com.tino.payroll.lite.enums.CurrencyCode;
import com.tino.payroll.lite.exception.PayrollConfigurationException;
import com.tino.payroll.lite.repository.NssaRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NssaRuleResolverTest {

    @Mock
    private NssaRuleRepository nssaRuleRepository;

    private NssaRuleResolver resolver;
    private LocalDate payrollDate;

    @BeforeEach
    void setUp() {
        resolver = new NssaRuleResolver(nssaRuleRepository);
        payrollDate = LocalDate.of(2026, 7, 31);
    }

    @Test
    void resolvesTheSingleApplicableRule() {
        NssaRule rule = rule("NSSA-USD-2026-07", CurrencyCode.USD);
        when(nssaRuleRepository.findApplicableRules(CurrencyCode.USD, payrollDate))
                .thenReturn(List.of(rule));

        NssaParameters parameters = resolver.resolve(CurrencyCode.USD, payrollDate);

        assertEquals(new BigDecimal("0.045"), parameters.employeeRate());
        assertEquals(new BigDecimal("0.045"), parameters.employerRate());
        assertEquals(new BigDecimal("1000.00"), parameters.pensionableEarningsCeiling());
        assertEquals("NSSA-USD-2026-07", parameters.ruleVersion());
    }

    @Test
    void rejectsMissingConfiguration() {
        when(nssaRuleRepository.findApplicableRules(CurrencyCode.ZWG, payrollDate))
                .thenReturn(List.of());

        PayrollConfigurationException exception = assertThrows(
                PayrollConfigurationException.class,
                () -> resolver.resolve(CurrencyCode.ZWG, payrollDate)
        );

        assertTrue(exception.getMessage().contains("No active NSSA rule"));
    }

    @Test
    void rejectsOverlappingConfiguration() {
        when(nssaRuleRepository.findApplicableRules(CurrencyCode.USD, payrollDate))
                .thenReturn(List.of(
                        rule("NSSA-USD-A", CurrencyCode.USD),
                        rule("NSSA-USD-B", CurrencyCode.USD)
                ));

        PayrollConfigurationException exception = assertThrows(
                PayrollConfigurationException.class,
                () -> resolver.resolve(CurrencyCode.USD, payrollDate)
        );

        assertTrue(exception.getMessage().contains("Overlapping NSSA rules"));
        assertTrue(exception.getMessage().contains("NSSA-USD-A"));
        assertTrue(exception.getMessage().contains("NSSA-USD-B"));
    }

    @Test
    void rejectsMissingResolutionInputsBeforeQuerying() {
        assertThrows(IllegalArgumentException.class,
                () -> resolver.resolve(null, payrollDate));
        assertThrows(IllegalArgumentException.class,
                () -> resolver.resolve(CurrencyCode.USD, null));
        verifyNoInteractions(nssaRuleRepository);
    }

    private NssaRule rule(String version, CurrencyCode currency) {
        return NssaRule.builder()
                .version(version)
                .currency(currency)
                .effectiveFrom(LocalDate.of(2026, 7, 1))
                .employeeRate(new BigDecimal("0.045"))
                .employerRate(new BigDecimal("0.045"))
                .pensionableEarningsCeiling(new BigDecimal("1000.00"))
                .active(true)
                .build();
    }
}