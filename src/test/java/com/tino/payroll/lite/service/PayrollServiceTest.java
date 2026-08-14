package com.tino.payroll.lite.service;

import com.tino.payroll.lite.dto.CreatePayrollRunRequest;
import com.tino.payroll.lite.entity.Employee;
import com.tino.payroll.lite.entity.PayrollAdjustment;
import com.tino.payroll.lite.entity.PayrollRun;
import com.tino.payroll.lite.entity.Payslip;
import com.tino.payroll.lite.entity.RecurringPayItem;
import com.tino.payroll.lite.enums.CurrencyCode;
import com.tino.payroll.lite.enums.EmployeeStatus;
import com.tino.payroll.lite.enums.PayItemSource;
import com.tino.payroll.lite.enums.PayrollAdjustmentType;
import com.tino.payroll.lite.enums.PayrollStatus;
import com.tino.payroll.lite.exception.DuplicatePayrollRunException;
import com.tino.payroll.lite.exception.InvalidPayrollStateException;
import com.tino.payroll.lite.exception.PayrollConfigurationException;
import com.tino.payroll.lite.exception.PayrollRunNotFoundException;
import com.tino.payroll.lite.repository.EmployeeRepo;
import com.tino.payroll.lite.repository.PayrollAdjustmentRepository;
import com.tino.payroll.lite.repository.PayrollRunRepository;
import com.tino.payroll.lite.repository.PayslipRepository;
import com.tino.payroll.lite.repository.RecurringPayItemRepository;
import com.tino.payroll.lite.service.calculation.NssaCalculator;
import com.tino.payroll.lite.service.calculation.NssaParameters;
import com.tino.payroll.lite.service.calculation.NssaRuleResolver;
import com.tino.payroll.lite.service.calculation.PayeCalculator;
import com.tino.payroll.lite.service.calculation.PayeParameters;
import com.tino.payroll.lite.service.calculation.PayeTaxBandParameters;
import com.tino.payroll.lite.service.calculation.PayeTaxTableResolver;
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
    private PayrollAdjustmentRepository adjustmentRepository;
    @Mock
    private RecurringPayItemRepository recurringPayItemRepository;
    @Mock
    private NssaRuleResolver nssaRuleResolver;
    @Mock
    private PayeTaxTableResolver payeTaxTableResolver;
    @Mock
    private AuditService auditService;

    private NssaCalculator nssaCalculator;
    private PayeCalculator payeCalculator;

    private PayrollService payrollService;

    @BeforeEach
    void setUp() {
        nssaCalculator = new NssaCalculator();
        payeCalculator = new PayeCalculator();
        payrollService = new PayrollService(
                payrollRunRepository, payslipRepository, employeeRepo, adjustmentRepository,
                recurringPayItemRepository,
                nssaRuleResolver, nssaCalculator,
                payeTaxTableResolver, payeCalculator, auditService
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
        when(payeTaxTableResolver.resolve(CurrencyCode.USD, LocalDate.of(2026, 7, 31)))
                .thenReturn(payeParameters());
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
        assertEquals(new BigDecimal("1500.00"), payslip.getTaxableIncome());
        assertEquals(new BigDecimal("365.00"), payslip.getIncomeTaxBeforeCredits());
        assertEquals(new BigDecimal("0.00"), payslip.getTaxCreditsApplied());
        assertEquals(new BigDecimal("10.95"), payslip.getAidsLevy());
        assertEquals(new BigDecimal("375.95"), payslip.getPayeDeduction());
        assertEquals("TEST-PAYE-USD-2026", payslip.getPayeRuleVersion());
        assertEquals(new BigDecimal("420.95"), payslip.getTotalDeductions());
        assertEquals(new BigDecimal("1079.05"), payslip.getNetSalary());
        assertSame(employee, payslip.getEmployee());
        assertSame(payrollRun, payslip.getPayrollRun());
        assertEquals(PayrollStatus.PROCESSED, payrollRun.getStatus());
        assertNotNull(payrollRun.getProcessedAt());

        employee.setBasicSalary(new BigDecimal("2000.00"));
        assertEquals(new BigDecimal("1500.00"), payslip.getBasicSalary());
        assertEquals(CurrencyCode.USD, payslip.getCurrency());
    }

    @Test
    void previewReturnsTheCalculationWithoutSavingOrChangingTheRun() {
        PayrollRun payrollRun = draftPayrollRun();
        Employee employee = Employee.builder()
                .id(10L)
                .employeeNumber("EMP-10")
                .firstName("Ada")
                .lastName("Moyo")
                .basicSalary(new BigDecimal("1500.00"))
                .salaryCurrency(CurrencyCode.USD)
                .status(EmployeeStatus.ACTIVE)
                .build();
        when(payrollRunRepository.findById(1L)).thenReturn(Optional.of(payrollRun));
        when(employeeRepo.findAllByStatusAndSalaryCurrency(EmployeeStatus.ACTIVE, CurrencyCode.USD))
                .thenReturn(List.of(employee));
        when(nssaRuleResolver.resolve(CurrencyCode.USD, LocalDate.of(2026, 7, 31)))
                .thenReturn(nssaParameters());
        when(payeTaxTableResolver.resolve(CurrencyCode.USD, LocalDate.of(2026, 7, 31)))
                .thenReturn(payeParameters());

        var preview = payrollService.previewPayrollRun(1L);

        assertEquals(1, preview.size());
        assertNull(preview.getFirst().getId());
        assertEquals(new BigDecimal("1500.00"), preview.getFirst().getGrossSalary());
        assertEquals(new BigDecimal("420.95"), preview.getFirst().getTotalDeductions());
        assertEquals(new BigDecimal("1079.05"), preview.getFirst().getNetSalary());
        assertEquals(PayrollStatus.DRAFT, payrollRun.getStatus());
        assertNull(payrollRun.getProcessedAt());
        verify(payslipRepository, never()).saveAll(any());
        verify(payrollRunRepository, never()).save(any());
    }

    @Test
    void processPayrollRunAutomaticallyAppliesApplicableRecurringItems() {
        PayrollRun payrollRun = draftPayrollRun();
        Employee employee = Employee.builder()
                .id(10L)
                .employeeNumber("EMP-10")
                .firstName("Ada")
                .lastName("Moyo")
                .basicSalary(new BigDecimal("1500.00"))
                .salaryCurrency(CurrencyCode.USD)
                .status(EmployeeStatus.ACTIVE)
                .build();
        RecurringPayItem housingAllowance = recurringItem(
                4L, employee, PayrollAdjustmentType.EARNING,
                "Housing allowance", "100.00", true
        );
        RecurringPayItem medicalAid = recurringItem(
                5L, employee, PayrollAdjustmentType.DEDUCTION,
                "Medical aid", "30.00", false
        );

        when(payrollRunRepository.findById(1L)).thenReturn(Optional.of(payrollRun));
        when(employeeRepo.findAllByStatusAndSalaryCurrency(EmployeeStatus.ACTIVE, CurrencyCode.USD))
                .thenReturn(List.of(employee));
        when(recurringPayItemRepository.findApplicable(
                List.of(10L), LocalDate.of(2026, 7, 31)
        )).thenReturn(List.of(housingAllowance, medicalAid));
        when(nssaRuleResolver.resolve(CurrencyCode.USD, LocalDate.of(2026, 7, 31)))
                .thenReturn(nssaParameters());
        when(payeTaxTableResolver.resolve(CurrencyCode.USD, LocalDate.of(2026, 7, 31)))
                .thenReturn(payeParameters());
        when(payrollRunRepository.save(any(PayrollRun.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        payrollService.processPayrollRun(1L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Payslip>> captor = ArgumentCaptor.forClass(List.class);
        verify(payslipRepository).saveAll(captor.capture());
        Payslip payslip = captor.getValue().getFirst();

        assertEquals(new BigDecimal("1600.00"), payslip.getGrossSalary());
        assertEquals(new BigDecimal("1600.00"), payslip.getTaxableIncome());
        assertEquals(new BigDecimal("395.00"), payslip.getIncomeTaxBeforeCredits());
        assertEquals(new BigDecimal("11.85"), payslip.getAidsLevy());
        assertEquals(new BigDecimal("406.85"), payslip.getPayeDeduction());
        assertEquals(new BigDecimal("481.85"), payslip.getTotalDeductions());
        assertEquals(new BigDecimal("1118.15"), payslip.getNetSalary());
        assertEquals(2, payslip.getLineItems().size());
        assertEquals(PayItemSource.RECURRING, payslip.getLineItems().getFirst().getSource());
    }

    @Test
    void processPayrollRunAppliesAndSnapshotsOneOffAdjustments() {
        PayrollRun payrollRun = draftPayrollRun();
        Employee employee = Employee.builder()
                .id(10L)
                .employeeNumber("EMP-10")
                .firstName("Ada")
                .lastName("Moyo")
                .basicSalary(new BigDecimal("1500.00"))
                .salaryCurrency(CurrencyCode.USD)
                .status(EmployeeStatus.ACTIVE)
                .build();
        PayrollAdjustment taxableBonus = adjustment(
                1L, employee, payrollRun, PayrollAdjustmentType.EARNING,
                "Performance bonus", "200.00", true
        );
        PayrollAdjustment nonTaxableAllowance = adjustment(
                2L, employee, payrollRun, PayrollAdjustmentType.EARNING,
                "Reimbursement", "50.00", false
        );
        PayrollAdjustment loanRepayment = adjustment(
                3L, employee, payrollRun, PayrollAdjustmentType.DEDUCTION,
                "Loan repayment", "75.00", false
        );

        when(payrollRunRepository.findById(1L)).thenReturn(Optional.of(payrollRun));
        when(employeeRepo.findAllByStatusAndSalaryCurrency(EmployeeStatus.ACTIVE, CurrencyCode.USD))
                .thenReturn(List.of(employee));
        when(adjustmentRepository.findByPayrollRunIdOrderByIdAsc(1L))
                .thenReturn(List.of(taxableBonus, nonTaxableAllowance, loanRepayment));
        when(nssaRuleResolver.resolve(CurrencyCode.USD, LocalDate.of(2026, 7, 31)))
                .thenReturn(nssaParameters());
        when(payeTaxTableResolver.resolve(CurrencyCode.USD, LocalDate.of(2026, 7, 31)))
                .thenReturn(payeParameters());
        when(payrollRunRepository.save(any(PayrollRun.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        payrollService.processPayrollRun(1L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Payslip>> captor = ArgumentCaptor.forClass(List.class);
        verify(payslipRepository).saveAll(captor.capture());
        Payslip payslip = captor.getValue().getFirst();

        assertEquals(new BigDecimal("1750.00"), payslip.getGrossSalary());
        assertEquals(new BigDecimal("1700.00"), payslip.getTaxableIncome());
        assertEquals(new BigDecimal("425.00"), payslip.getIncomeTaxBeforeCredits());
        assertEquals(new BigDecimal("12.75"), payslip.getAidsLevy());
        assertEquals(new BigDecimal("437.75"), payslip.getPayeDeduction());
        assertEquals(new BigDecimal("557.75"), payslip.getTotalDeductions());
        assertEquals(new BigDecimal("1192.25"), payslip.getNetSalary());
        assertEquals(3, payslip.getLineItems().size());
        assertEquals("Performance bonus", payslip.getLineItems().getFirst().getDescription());
        assertTrue(payslip.getLineItems().getFirst().isTaxable());
        assertSame(payslip, payslip.getLineItems().getFirst().getPayslip());

        taxableBonus.setAmount(new BigDecimal("999.00"));
        assertEquals(new BigDecimal("200.00"), payslip.getLineItems().getFirst().getAmount());
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
        verifyNoInteractions(nssaRuleResolver, payeTaxTableResolver);
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
        verifyNoInteractions(payeTaxTableResolver);
        verify(payrollRunRepository, never()).save(any(PayrollRun.class));
        assertEquals(PayrollStatus.DRAFT, payrollRun.getStatus());
    }

    @Test
    void missingPayeConfigurationStopsPayrollBeforeAnythingIsSaved() {
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
                .thenReturn(nssaParameters());
        when(payeTaxTableResolver.resolve(CurrencyCode.USD, LocalDate.of(2026, 7, 31)))
                .thenThrow(new PayrollConfigurationException("No PAYE table configured"));

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

        verifyNoInteractions(
                employeeRepo, payslipRepository,
                adjustmentRepository, recurringPayItemRepository,
                nssaRuleResolver, payeTaxTableResolver
        );
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
    private PayeParameters payeParameters() {
        return new PayeParameters(
                "TEST-PAYE-USD-2026",
                new BigDecimal("0.03"),
                List.of(
                        payeBand("0", "100", "0"),
                        payeBand("100", "300", "0.20"),
                        payeBand("300", "1000", "0.25"),
                        payeBand("1000", "2000", "0.30"),
                        payeBand("2000", "3000", "0.35"),
                        payeBand("3000", null, "0.40")
                )
        );
    }
    private PayeTaxBandParameters payeBand(String lower, String upper, String rate) {
        return new PayeTaxBandParameters(
                new BigDecimal(lower),
                upper == null ? null : new BigDecimal(upper),
                new BigDecimal(rate)
        );
    }
    private PayrollAdjustment adjustment(
            Long id,
            Employee employee,
            PayrollRun payrollRun,
            PayrollAdjustmentType type,
            String description,
            String amount,
            boolean taxable
    ) {
        PayrollAdjustment adjustment = new PayrollAdjustment();
        adjustment.setId(id);
        adjustment.setEmployee(employee);
        adjustment.setPayrollRun(payrollRun);
        adjustment.setType(type);
        adjustment.setDescription(description);
        adjustment.setAmount(new BigDecimal(amount));
        adjustment.setTaxable(taxable);
        return adjustment;
    }
    private RecurringPayItem recurringItem(
            Long id,
            Employee employee,
            PayrollAdjustmentType type,
            String description,
            String amount,
            boolean taxable
    ) {
        RecurringPayItem item = new RecurringPayItem();
        item.setId(id);
        item.setEmployee(employee);
        item.setType(type);
        item.setDescription(description);
        item.setAmount(new BigDecimal(amount));
        item.setTaxable(taxable);
        item.setActive(true);
        return item;
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
