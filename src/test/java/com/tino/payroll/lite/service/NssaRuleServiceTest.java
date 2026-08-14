package com.tino.payroll.lite.service;

import com.tino.payroll.lite.dto.NssaRuleRequest;
import com.tino.payroll.lite.entity.NssaRule;
import com.tino.payroll.lite.enums.CurrencyCode;
import com.tino.payroll.lite.exception.NssaRuleNotFoundException;
import com.tino.payroll.lite.exception.PayrollConfigurationException;
import com.tino.payroll.lite.repository.NssaRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NssaRuleServiceTest {

    @Mock
    private NssaRuleRepository repository;
    @Mock
    private AuditService auditService;

    private NssaRuleService service;

    @BeforeEach
    void setUp() {
        service = new NssaRuleService(repository, auditService);
    }

    @Test
    void createsValidatedRule() {
        NssaRuleRequest request = request();
        when(repository.save(any(NssaRule.class))).thenAnswer(invocation -> {
            NssaRule rule = invocation.getArgument(0);
            rule.setId(1L);
            return rule;
        });

        var response = service.create(request);

        assertEquals(1L, response.getId());
        assertEquals("NSSA-USD-2026", response.getVersion());
        assertEquals(new BigDecimal("0.045"), response.getEmployeeRate());
        verify(repository).existsOverlappingActiveRule(
                CurrencyCode.USD,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(9999, 12, 31),
                null
        );
    }

    @Test
    void overlappingActiveRuleIsRejected() {
        NssaRuleRequest request = request();
        when(repository.existsOverlappingActiveRule(any(), any(), any(), isNull()))
                .thenReturn(true);

        assertThrows(PayrollConfigurationException.class, () -> service.create(request));

        verify(repository, never()).save(any());
    }

    @Test
    void endDateCannotPrecedeStartDate() {
        NssaRuleRequest request = request();
        request.setEffectiveTo(LocalDate.of(2025, 12, 31));

        assertThrows(PayrollConfigurationException.class, () -> service.create(request));

        verify(repository, never()).save(any());
    }

    @Test
    void missingRuleCannotBeUpdated() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NssaRuleNotFoundException.class, () -> service.update(99L, request()));
    }

    private NssaRuleRequest request() {
        NssaRuleRequest request = new NssaRuleRequest();
        request.setVersion("NSSA-USD-2026");
        request.setCurrency(CurrencyCode.USD);
        request.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        request.setEmployeeRate(new BigDecimal("0.045"));
        request.setEmployerRate(new BigDecimal("0.045"));
        request.setPensionableEarningsCeiling(new BigDecimal("1000.00"));
        request.setActive(true);
        return request;
    }
}
