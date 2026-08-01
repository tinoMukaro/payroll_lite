package com.tino.payroll.lite.service;

import com.tino.payroll.lite.dto.RecurringPayItemRequest;
import com.tino.payroll.lite.entity.Employee;
import com.tino.payroll.lite.entity.RecurringPayItem;
import com.tino.payroll.lite.enums.CurrencyCode;
import com.tino.payroll.lite.enums.EmployeeStatus;
import com.tino.payroll.lite.enums.PayrollAdjustmentType;
import com.tino.payroll.lite.exception.RecurringPayItemNotFoundException;
import com.tino.payroll.lite.repository.EmployeeRepo;
import com.tino.payroll.lite.repository.RecurringPayItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurringPayItemServiceTest {

    @Mock
    private RecurringPayItemRepository payItemRepository;
    @Mock
    private EmployeeRepo employeeRepo;

    private RecurringPayItemService service;

    @BeforeEach
    void setUp() {
        service = new RecurringPayItemService(payItemRepository, employeeRepo);
    }

    @Test
    void createsActiveTaxableRecurringEarning() {
        Employee employee = employee();
        when(employeeRepo.findById(10L)).thenReturn(Optional.of(employee));
        when(payItemRepository.save(any(RecurringPayItem.class))).thenAnswer(invocation -> {
            RecurringPayItem item = invocation.getArgument(0);
            item.setId(20L);
            return item;
        });

        var response = service.create(10L, request(PayrollAdjustmentType.EARNING, true));

        assertEquals(20L, response.getId());
        assertEquals("Housing allowance", response.getDescription());
        assertEquals(new BigDecimal("100.00"), response.getAmount());
        assertEquals(CurrencyCode.USD, response.getCurrency());
        assertTrue(response.isTaxable());
        assertTrue(response.isActive());
    }

    @Test
    void rejectsEffectiveToBeforeEffectiveFrom() {
        RecurringPayItemRequest request = request(PayrollAdjustmentType.EARNING, true);
        request.setEffectiveTo(LocalDate.of(2026, 7, 31));
        when(employeeRepo.findById(10L)).thenReturn(Optional.of(employee()));

        assertThrows(IllegalArgumentException.class, () -> service.create(10L, request));

        verify(payItemRepository, never()).save(any());
    }

    @Test
    void rejectsTaxableRecurringDeduction() {
        when(employeeRepo.findById(10L)).thenReturn(Optional.of(employee()));

        assertThrows(IllegalArgumentException.class,
                () -> service.create(10L, request(PayrollAdjustmentType.DEDUCTION, true)));

        verify(payItemRepository, never()).save(any());
    }

    @Test
    void updatesExistingItemAndCanDeactivateIt() {
        Employee employee = employee();
        RecurringPayItem existing = new RecurringPayItem();
        existing.setId(20L);
        existing.setEmployee(employee);
        existing.setType(PayrollAdjustmentType.EARNING);
        existing.setDescription("Old allowance");
        existing.setAmount(new BigDecimal("50.00"));
        existing.setActive(true);
        RecurringPayItemRequest request = request(PayrollAdjustmentType.DEDUCTION, false);
        request.setActive(false);
        when(employeeRepo.findById(10L)).thenReturn(Optional.of(employee));
        when(payItemRepository.findByIdAndEmployeeId(20L, 10L)).thenReturn(Optional.of(existing));
        when(payItemRepository.save(existing)).thenReturn(existing);

        var response = service.update(10L, 20L, request);

        assertEquals(PayrollAdjustmentType.DEDUCTION, response.getType());
        assertFalse(response.isTaxable());
        assertFalse(response.isActive());
    }

    @Test
    void updateRequiresItemToBelongToEmployee() {
        when(employeeRepo.findById(10L)).thenReturn(Optional.of(employee()));
        when(payItemRepository.findByIdAndEmployeeId(99L, 10L)).thenReturn(Optional.empty());

        assertThrows(RecurringPayItemNotFoundException.class,
                () -> service.update(10L, 99L, request(PayrollAdjustmentType.EARNING, true)));
    }

    private RecurringPayItemRequest request(PayrollAdjustmentType type, boolean taxable) {
        RecurringPayItemRequest request = new RecurringPayItemRequest();
        request.setType(type);
        request.setDescription("  Housing allowance  ");
        request.setAmount(new BigDecimal("100.00"));
        request.setTaxable(taxable);
        request.setEffectiveFrom(LocalDate.of(2026, 8, 1));
        request.setActive(true);
        return request;
    }

    private Employee employee() {
        return Employee.builder()
                .id(10L)
                .employeeNumber("EMP-000010")
                .firstName("Ada")
                .lastName("Moyo")
                .basicSalary(new BigDecimal("1500.00"))
                .salaryCurrency(CurrencyCode.USD)
                .status(EmployeeStatus.ACTIVE)
                .build();
    }
}
