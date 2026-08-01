package com.tino.payroll.lite.service;

import com.tino.payroll.lite.dto.PayeTaxBandRequest;
import com.tino.payroll.lite.dto.PayeTaxTableRequest;
import com.tino.payroll.lite.entity.PayeTaxTable;
import com.tino.payroll.lite.enums.CurrencyCode;
import com.tino.payroll.lite.exception.PayeTaxTableNotFoundException;
import com.tino.payroll.lite.exception.PayrollConfigurationException;
import com.tino.payroll.lite.repository.PayeTaxTableRepository;
import com.tino.payroll.lite.service.calculation.PayeCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayeTaxTableServiceTest {

    @Mock
    private PayeTaxTableRepository repository;

    private PayeTaxTableService service;

    @BeforeEach
    void setUp() {
        service = new PayeTaxTableService(repository, new PayeCalculator());
    }

    @Test
    void createsValidatedTableWithSortedOwnedBands() {
        PayeTaxTableRequest request = request();
        request.setBands(List.of(
                band("100", null, "0.20"),
                band("0", "100", "0")
        ));
        when(repository.save(any(PayeTaxTable.class))).thenAnswer(invocation -> {
            PayeTaxTable table = invocation.getArgument(0);
            table.setId(1L);
            return table;
        });

        var response = service.create(request);

        ArgumentCaptor<PayeTaxTable> captor = ArgumentCaptor.forClass(PayeTaxTable.class);
        verify(repository).save(captor.capture());
        PayeTaxTable saved = captor.getValue();
        assertEquals(1L, response.getId());
        assertEquals("PAYE-USD-2026", response.getVersion());
        assertEquals(new BigDecimal("0"), saved.getBands().getFirst().getLowerBound());
        assertEquals(new BigDecimal("100"), saved.getBands().getLast().getLowerBound());
        assertSame(saved, saved.getBands().getFirst().getTaxTable());
        verify(repository).existsOverlappingActiveTable(
                CurrencyCode.USD,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(9999, 12, 31),
                null
        );
    }

    @Test
    void rejectsOverlappingActiveTable() {
        when(repository.existsOverlappingActiveTable(any(), any(), any(), isNull()))
                .thenReturn(true);

        assertThrows(PayrollConfigurationException.class, () -> service.create(request()));

        verify(repository, never()).save(any());
    }

    @Test
    void rejectsInvalidBandConfigurationBeforeSaving() {
        PayeTaxTableRequest request = request();
        request.setBands(List.of(
                band("0", "100", "0"),
                band("101", null, "0.20")
        ));

        assertThrows(IllegalArgumentException.class, () -> service.create(request));

        verify(repository, never()).save(any());
    }

    @Test
    void updateReplacesExistingBands() {
        PayeTaxTable table = PayeTaxTable.builder()
                .id(5L)
                .version("OLD")
                .currency(CurrencyCode.USD)
                .effectiveFrom(LocalDate.of(2025, 1, 1))
                .aidsLevyRate(new BigDecimal("0.03"))
                .build();
        when(repository.findById(5L)).thenReturn(Optional.of(table));
        when(repository.save(table)).thenReturn(table);

        var response = service.update(5L, request());

        assertEquals("PAYE-USD-2026", response.getVersion());
        assertEquals(2, response.getBands().size());
        assertSame(table, table.getBands().getFirst().getTaxTable());
        verify(repository).existsOverlappingActiveTable(
                CurrencyCode.USD,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(9999, 12, 31),
                5L
        );
    }

    @Test
    void missingTableCannotBeUpdated() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(PayeTaxTableNotFoundException.class,
                () -> service.update(99L, request()));
    }

    private PayeTaxTableRequest request() {
        PayeTaxTableRequest request = new PayeTaxTableRequest();
        request.setVersion("PAYE-USD-2026");
        request.setCurrency(CurrencyCode.USD);
        request.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        request.setAidsLevyRate(new BigDecimal("0.03"));
        request.setActive(true);
        request.setBands(List.of(
                band("0", "100", "0"),
                band("100", null, "0.20")
        ));
        return request;
    }

    private PayeTaxBandRequest band(String lower, String upper, String rate) {
        PayeTaxBandRequest band = new PayeTaxBandRequest();
        band.setLowerBound(new BigDecimal(lower));
        band.setUpperBound(upper == null ? null : new BigDecimal(upper));
        band.setRate(new BigDecimal(rate));
        return band;
    }
}
