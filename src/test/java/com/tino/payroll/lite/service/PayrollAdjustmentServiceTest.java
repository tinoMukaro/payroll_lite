package com.tino.payroll.lite.service;

import com.tino.payroll.lite.dto.PayrollAdjustmentRequest;
import com.tino.payroll.lite.entity.Employee;
import com.tino.payroll.lite.entity.PayrollAdjustment;
import com.tino.payroll.lite.entity.PayrollRun;
import com.tino.payroll.lite.enums.CurrencyCode;
import com.tino.payroll.lite.enums.EmployeeStatus;
import com.tino.payroll.lite.enums.PayrollAdjustmentType;
import com.tino.payroll.lite.enums.PayrollStatus;
import com.tino.payroll.lite.exception.InvalidPayrollStateException;
import com.tino.payroll.lite.repository.EmployeeRepo;
import com.tino.payroll.lite.repository.PayrollAdjustmentRepository;
import com.tino.payroll.lite.repository.PayrollRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollAdjustmentServiceTest {

    @Mock
    private PayrollAdjustmentRepository adjustmentRepository;
    @Mock
    private PayrollRunRepository payrollRunRepository;
    @Mock
    private EmployeeRepo employeeRepo;

    private PayrollAdjustmentService service;

    @BeforeEach
    void setUp() {
        service = new PayrollAdjustmentService(
                adjustmentRepository, payrollRunRepository, employeeRepo
        );
    }

    @Test
    void createsTaxableEarningForEligibleEmployee() {
        PayrollRun payrollRun = payrollRun(PayrollStatus.DRAFT);
        Employee employee = employee(CurrencyCode.USD, EmployeeStatus.ACTIVE);
        PayrollAdjustmentRequest request = request(PayrollAdjustmentType.EARNING, true);
        when(payrollRunRepository.findById(1L)).thenReturn(Optional.of(payrollRun));
        when(employeeRepo.findById(10L)).thenReturn(Optional.of(employee));
        when(adjustmentRepository.save(any(PayrollAdjustment.class))).thenAnswer(invocation -> {
            PayrollAdjustment adjustment = invocation.getArgument(0);
            adjustment.setId(20L);
            return adjustment;
        });

        var response = service.create(1L, request);

        assertEquals(20L, response.getId());
        assertEquals("Overtime", response.getDescription());
        assertEquals(new BigDecimal("125.50"), response.getAmount());
        assertTrue(response.isTaxable());
    }

    @Test
    void rejectsChangesAfterPayrollIsProcessed() {
        when(payrollRunRepository.findById(1L))
                .thenReturn(Optional.of(payrollRun(PayrollStatus.PROCESSED)));

        assertThrows(InvalidPayrollStateException.class,
                () -> service.create(1L, request(PayrollAdjustmentType.EARNING, true)));

        verifyNoInteractions(employeeRepo, adjustmentRepository);
    }

    @Test
    void rejectsEmployeeWithDifferentCurrency() {
        when(payrollRunRepository.findById(1L))
                .thenReturn(Optional.of(payrollRun(PayrollStatus.DRAFT)));
        when(employeeRepo.findById(10L))
                .thenReturn(Optional.of(employee(CurrencyCode.ZWG, EmployeeStatus.ACTIVE)));

        assertThrows(IllegalArgumentException.class,
                () -> service.create(1L, request(PayrollAdjustmentType.EARNING, true)));

        verify(adjustmentRepository, never()).save(any());
    }

    @Test
    void rejectsTaxableDeduction() {
        when(payrollRunRepository.findById(1L))
                .thenReturn(Optional.of(payrollRun(PayrollStatus.DRAFT)));
        when(employeeRepo.findById(10L))
                .thenReturn(Optional.of(employee(CurrencyCode.USD, EmployeeStatus.ACTIVE)));

        assertThrows(IllegalArgumentException.class,
                () -> service.create(1L, request(PayrollAdjustmentType.DEDUCTION, true)));

        verify(adjustmentRepository, never()).save(any());
    }

    @Test
    void createsNonTaxableDeduction() {
        PayrollRun payrollRun = payrollRun(PayrollStatus.DRAFT);
        Employee employee = employee(CurrencyCode.USD, EmployeeStatus.ACTIVE);
        PayrollAdjustmentRequest request = request(PayrollAdjustmentType.DEDUCTION, false);
        when(payrollRunRepository.findById(1L)).thenReturn(Optional.of(payrollRun));
        when(employeeRepo.findById(10L)).thenReturn(Optional.of(employee));
        when(adjustmentRepository.save(any(PayrollAdjustment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(1L, request);

        assertEquals(PayrollAdjustmentType.DEDUCTION, response.getType());
        assertFalse(response.isTaxable());
    }

    private PayrollAdjustmentRequest request(PayrollAdjustmentType type, boolean taxable) {
        PayrollAdjustmentRequest request = new PayrollAdjustmentRequest();
        request.setEmployeeId(10L);
        request.setType(type);
        request.setDescription("  Overtime  ");
        request.setAmount(new BigDecimal("125.50"));
        request.setTaxable(taxable);
        return request;
    }

    private PayrollRun payrollRun(PayrollStatus status) {
        PayrollRun payrollRun = new PayrollRun();
        payrollRun.setId(1L);
        payrollRun.setMonth(7);
        payrollRun.setYear(2026);
        payrollRun.setCurrency(CurrencyCode.USD);
        payrollRun.setStatus(status);
        return payrollRun;
    }

    private Employee employee(CurrencyCode currency, EmployeeStatus status) {
        return Employee.builder()
                .id(10L)
                .employeeNumber("EMP-000010")
                .firstName("Ada")
                .lastName("Moyo")
                .basicSalary(new BigDecimal("1500.00"))
                .salaryCurrency(currency)
                .status(status)
                .build();
    }
}
