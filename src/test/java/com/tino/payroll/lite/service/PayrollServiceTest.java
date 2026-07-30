package com.tino.payroll.lite.service;

import com.tino.payroll.lite.dto.CreatePayrollRunRequest;
import com.tino.payroll.lite.entity.Employee;
import com.tino.payroll.lite.entity.PayrollRun;
import com.tino.payroll.lite.entity.Payslip;
import com.tino.payroll.lite.enums.CurrencyCode;
import com.tino.payroll.lite.enums.EmployeeStatus;
import com.tino.payroll.lite.enums.PayrollStatus;
import com.tino.payroll.lite.exception.DuplicatePayrollRunException;
import com.tino.payroll.lite.exception.InvalidPayrollStateException;
import com.tino.payroll.lite.exception.PayrollConfigurationException;
import com.tino.payroll.lite.exception.PayrollRunNotFoundException;
import com.tino.payroll.lite.repository.EmployeeRepo;
import com.tino.payroll.lite.repository.PayrollRunRepository;
import com.tino.payroll.lite.repository.PayslipRepository;
import com.tino.payroll.lite.service.calculation.NssaCalculator;
import com.tino.payroll.lite.service.calculation.NssaParameters;
import com.tino.payroll.lite.service.calculation.NssaRuleResolver;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {

    @Mock
    private PayrollRunRepository payrollRunRepository;
    @Mock
    private PayslipRepository payslipRepository;
    @Mock
    private EmployeeRepo employeeRepo;
    @Mock
    private NssaRuleResolver nssaRuleResolver;

    private NssaCalculator nssaCalculator;

    private PayrollService payrollService;

    @BeforeEach
    void setUp() {
        nssaCalculator = new NssaCalculator();
        payrollService = new PayrollService(
                payrollRunRepository, payslipRepository, employeeRepo,
                nssaRuleResolver, nssaCalculator
        );
    }

    @Test
    void createPayrollRunRejectsDuplicatePeriod() {
        CreatePayrollRunRequest request = request(7, 2026);
        when(payrollRunRepository.existsByMonthAndYearAndCurrency(7, 2026, CurrencyCode.USD)).thenReturn(true);

        assertThrows(DuplicatePayrollRunException.class,
                () -> payrollService.createPayrollRun(request));

        verify(payrollRunRepository, never()).save(any());
    }

    @Test
    void processPayrollRunCreatesSalarySnapshotForActiveEmployees() {
        PayrollRun payrollRun = draftPayrollRun();
        Employee employee = Employee.builder()
                .id(10L)
                .employeeNumber("EMP-10")
                .firstName("Ada")
                .lastName("Moyo")
                .email("ada@example.com")
                .jobTitle("Developer")
                .basicSalary(new BigDecimal("1500.00"))
                .status(EmployeeStatus.ACTIVE)
                .build();

        when(payrollRunRepository.findById(1L)).thenReturn(Optional.of(payrollRun));
        when(employeeRepo.findAllByStatusAndSalaryCurrency(EmployeeStatus.ACTIVE, CurrencyCode.USD)).thenReturn(List.of(employee));
        when(nssaRuleResolver.resolve(CurrencyCode.USD, LocalDate.of(2026, 7, 31)))
                .thenReturn(nssaParameters());
        when(payrollRunRepository.save(any(PayrollRun.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        payrollService.processPayrollRun(1L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Payslip>> captor = ArgumentCaptor.forClass(List.class);
        verify(payslipRepository).saveAll(captor.capture());
        Payslip payslip = captor.getValue().getFirst();

        assertEquals(new BigDecimal("1500.00"), payslip.getBasicSalary());
        assertEquals(CurrencyCode.USD, payslip.getCurrency());
        assertEquals(new BigDecimal("1500.00"), payslip.getGrossSalary());
        assertEquals(new BigDecimal("1000.00"), payslip.getPensionableEarnings());
        assertEquals(new BigDecimal("45.00"), payslip.getEmployeeNssaContribution());
        assertEquals(new BigDecimal("45.00"), payslip.getEmployerNssaContribution());
        assertEquals("TEST-NSSA-USD-2026-07", payslip.getNssaRuleVersion());
        assertEquals(new BigDecimal("45.00"), payslip.getTotalDeductions());
        assertEquals(new BigDecimal("1455.00"), payslip.getNetSalary());
        assertSame(employee, payslip.getEmployee());
        assertSame(payrollRun, payslip.getPayrollRun());
        assertEquals(PayrollStatus.PROCESSED, payrollRun.getStatus());
        assertNotNull(payrollRun.getProcessedAt());

        employee.setBasicSalary(new BigDecimal("2000.00"));
        assertEquals(new BigDecimal("1500.00"), payslip.getBasicSalary());
        assertEquals(CurrencyCode.USD, payslip.getCurrency());
    }

    @Test
    void processPayrollRunOnlyQueriesActiveEmployees() {
        PayrollRun payrollRun = draftPayrollRun();
        when(payrollRunRepository.findById(1L)).thenReturn(Optional.of(payrollRun));
        when(employeeRepo.findAllByStatusAndSalaryCurrency(EmployeeStatus.ACTIVE, CurrencyCode.USD)).thenReturn(List.of());
        when(payrollRunRepository.save(any(PayrollRun.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        payrollService.processPayrollRun(1L);

        verify(employeeRepo).findAllByStatusAndSalaryCurrency(EmployeeStatus.ACTIVE, CurrencyCode.USD);
        verify(payslipRepository).saveAll(List.of());
        verifyNoInteractions(nssaRuleResolver);
    }

    @Test
    void missingNssaConfigurationStopsPayrollBeforeAnythingIsSaved() {
        PayrollRun payrollRun = draftPayrollRun();
        Employee employee = Employee.builder()
                .id(10L)
                .basicSalary(new BigDecimal("1500.00"))
                .status(EmployeeStatus.ACTIVE)
                .build();
        when(payrollRunRepository.findById(1L)).thenReturn(Optional.of(payrollRun));
        when(employeeRepo.findAllByStatusAndSalaryCurrency(EmployeeStatus.ACTIVE, CurrencyCode.USD))
                .thenReturn(List.of(employee));
        when(nssaRuleResolver.resolve(CurrencyCode.USD, LocalDate.of(2026, 7, 31)))
                .thenThrow(new PayrollConfigurationException("No NSSA rule configured"));

        assertThrows(PayrollConfigurationException.class,
                () -> payrollService.processPayrollRun(1L));

        verifyNoInteractions(payslipRepository);
        verify(payrollRunRepository, never()).save(any(PayrollRun.class));
        assertEquals(PayrollStatus.DRAFT, payrollRun.getStatus());
    }

    @Test
    void processedPayrollRunCannotBeProcessedAgain() {
        PayrollRun payrollRun = draftPayrollRun();
        payrollRun.setStatus(PayrollStatus.PROCESSED);
        when(payrollRunRepository.findById(1L)).thenReturn(Optional.of(payrollRun));

        assertThrows(InvalidPayrollStateException.class,
                () -> payrollService.processPayrollRun(1L));

        verifyNoInteractions(employeeRepo, payslipRepository, nssaRuleResolver);
    }

    @Test
    void missingPayrollRunReturnsNotFoundError() {
        when(payrollRunRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(PayrollRunNotFoundException.class,
                () -> payrollService.getPayrollRun(99L));
    }

    @Test
    void employeePayslipsAreLoadedOnlyThroughTheAuthenticatedUserId() {
        Employee employee = Employee.builder()
                .id(10L)
                .employeeNumber("EMP-000010")
                .firstName("Ada")
                .lastName("Moyo")
                .build();
        PayrollRun payrollRun = draftPayrollRun();
        Payslip payslip = new Payslip();
        payslip.setId(20L);
        payslip.setEmployee(employee);
        payslip.setPayrollRun(payrollRun);
        payslip.setGrossSalary(new BigDecimal("1500.00"));
        payslip.setTotalDeductions(BigDecimal.ZERO);
        payslip.setNetSalary(new BigDecimal("1500.00"));

        when(payslipRepository.findForUser(7L)).thenReturn(List.of(payslip));

        var responses = payrollService.getPayslipsForUser(7L);

        assertEquals(1, responses.size());
        assertEquals(7, responses.getFirst().getMonth());
        assertEquals(2026, responses.getFirst().getYear());
        assertEquals("EMP-000010", responses.getFirst().getEmployeeNumber());
        verify(payslipRepository).findForUser(7L);
    }
    private NssaParameters nssaParameters() {
        return new NssaParameters(
                new BigDecimal("0.045"),
                new BigDecimal("0.045"),
                new BigDecimal("1000.00"),
                "TEST-NSSA-USD-2026-07"
        );
    }
    private CreatePayrollRunRequest request(Integer month, Integer year) {
        CreatePayrollRunRequest request = new CreatePayrollRunRequest();
        request.setMonth(month);
        request.setYear(year);
        request.setCurrency(CurrencyCode.USD);
        return request;
    }

    private PayrollRun draftPayrollRun() {
        PayrollRun payrollRun = new PayrollRun();
        payrollRun.setId(1L);
        payrollRun.setMonth(7);
        payrollRun.setYear(2026);
        payrollRun.setCurrency(CurrencyCode.USD);
        payrollRun.setStatus(PayrollStatus.DRAFT);
        return payrollRun;
    }
}
