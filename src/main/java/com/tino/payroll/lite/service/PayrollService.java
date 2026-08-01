package com.tino.payroll.lite.service;

import com.tino.payroll.lite.dto.CreatePayrollRunRequest;
import com.tino.payroll.lite.dto.PayrollRunResponse;
import com.tino.payroll.lite.dto.PayslipResponse;
import com.tino.payroll.lite.entity.Employee;
import com.tino.payroll.lite.entity.PayrollRun;
import com.tino.payroll.lite.entity.Payslip;
import com.tino.payroll.lite.enums.EmployeeStatus;
import com.tino.payroll.lite.enums.PayrollStatus;
import com.tino.payroll.lite.exception.DuplicatePayrollRunException;
import com.tino.payroll.lite.exception.InvalidPayrollStateException;
import com.tino.payroll.lite.exception.PayrollRunNotFoundException;
import com.tino.payroll.lite.repository.EmployeeRepo;
import com.tino.payroll.lite.repository.PayrollRunRepository;
import com.tino.payroll.lite.repository.PayslipRepository;
import com.tino.payroll.lite.service.calculation.NssaCalculation;
import com.tino.payroll.lite.service.calculation.NssaCalculator;
import com.tino.payroll.lite.service.calculation.NssaParameters;
import com.tino.payroll.lite.service.calculation.NssaRuleResolver;
import com.tino.payroll.lite.service.calculation.PayeCalculation;
import com.tino.payroll.lite.service.calculation.PayeCalculator;
import com.tino.payroll.lite.service.calculation.PayeParameters;
import com.tino.payroll.lite.service.calculation.PayeTaxTableResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PayrollService {

    private final PayrollRunRepository payrollRunRepository;
    private final PayslipRepository payslipRepository;
    private final EmployeeRepo employeeRepo;
    private final NssaRuleResolver nssaRuleResolver;
    private final NssaCalculator nssaCalculator;
    private final PayeTaxTableResolver payeTaxTableResolver;
    private final PayeCalculator payeCalculator;

    @Transactional
    public PayrollRunResponse createPayrollRun(CreatePayrollRunRequest request) {
        if (payrollRunRepository.existsByMonthAndYearAndCurrency(request.getMonth(), request.getYear(), request.getCurrency())) {
            throw new DuplicatePayrollRunException(
                    "A payroll run already exists for month " + request.getMonth() + " and year " + request.getYear() + " in " + request.getCurrency()
            );
        }

        PayrollRun payrollRun = new PayrollRun();
        payrollRun.setMonth(request.getMonth());
        payrollRun.setYear(request.getYear());
        payrollRun.setCurrency(request.getCurrency());
        payrollRun.setStatus(PayrollStatus.DRAFT);

        return mapPayrollRun(payrollRunRepository.save(payrollRun));
    }

    @Transactional(readOnly = true)
    public List<PayrollRunResponse> getAllPayrollRuns() {
        return payrollRunRepository.findAll().stream()
                .map(this::mapPayrollRun)
                .toList();
    }

    @Transactional(readOnly = true)
    public PayrollRunResponse getPayrollRun(Long id) {
        return mapPayrollRun(findPayrollRun(id));
    }

    @Transactional
    public PayrollRunResponse processPayrollRun(Long id) {
        PayrollRun payrollRun = findPayrollRun(id);

        if (payrollRun.getStatus() != PayrollStatus.DRAFT) {
            throw new InvalidPayrollStateException(
                    "Only DRAFT payroll runs can be processed. Current status: " + payrollRun.getStatus()
            );
        }

        List<Employee> eligibleEmployees = employeeRepo.findAllByStatusAndSalaryCurrency(
                EmployeeStatus.ACTIVE,
                payrollRun.getCurrency()
        );

        List<Payslip> payslips;
        if (eligibleEmployees.isEmpty()) {
            payslips = List.of();
        } else {
            NssaParameters nssaParameters = nssaRuleResolver.resolve(
                    payrollRun.getCurrency(),
                    YearMonth.of(payrollRun.getYear(), payrollRun.getMonth()).atEndOfMonth()
            );
            PayeParameters payeParameters = payeTaxTableResolver.resolve(
                    payrollRun.getCurrency(),
                    YearMonth.of(payrollRun.getYear(), payrollRun.getMonth()).atEndOfMonth()
            );
            payslips = eligibleEmployees.stream()
                    .map(employee -> createPayslip(
                            employee, payrollRun, nssaParameters, payeParameters
                    ))
                    .toList();
        }

        payslipRepository.saveAll(payslips);
        payrollRun.setStatus(PayrollStatus.PROCESSED);
        payrollRun.setProcessedAt(LocalDateTime.now());

        return mapPayrollRun(payrollRunRepository.save(payrollRun));
    }

    @Transactional(readOnly = true)
    public List<PayslipResponse> getPayslipsForPayrollRun(Long payrollRunId) {
        findPayrollRun(payrollRunId);
        return payslipRepository.findByPayrollRunIdOrderByIdAsc(payrollRunId).stream()
                .map(this::mapPayslip)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PayslipResponse> getPayslipsForUser(Long userId) {
        return payslipRepository.findForUser(userId).stream()
                .map(this::mapPayslip)
                .toList();
    }
    private PayrollRun findPayrollRun(Long id) {
        return payrollRunRepository.findById(id)
                .orElseThrow(() -> new PayrollRunNotFoundException("Payroll run not found with ID: " + id));
    }

    private Payslip createPayslip(
            Employee employee,
            PayrollRun payrollRun,
            NssaParameters nssaParameters,
            PayeParameters payeParameters
    ) {
        BigDecimal basicSalarySnapshot = employee.getBasicSalary();
        NssaCalculation nssaCalculation = nssaCalculator.calculate(
                basicSalarySnapshot,
                nssaParameters
        );
        PayeCalculation payeCalculation = payeCalculator.calculate(
                basicSalarySnapshot,
                BigDecimal.ZERO,
                payeParameters
        );
        BigDecimal totalDeductions = nssaCalculation.employeeContribution()
                .add(payeCalculation.totalPaye());

        Payslip payslip = new Payslip();
        payslip.setEmployee(employee);
        payslip.setPayrollRun(payrollRun);
        payslip.setBasicSalary(basicSalarySnapshot);
        payslip.setCurrency(payrollRun.getCurrency());
        payslip.setGrossSalary(basicSalarySnapshot);
        payslip.setPensionableEarnings(nssaCalculation.pensionableEarnings());
        payslip.setEmployeeNssaContribution(nssaCalculation.employeeContribution());
        payslip.setEmployerNssaContribution(nssaCalculation.employerContribution());
        payslip.setNssaRuleVersion(nssaCalculation.ruleVersion());
        payslip.setPayeDeduction(payeCalculation.totalPaye());
        payslip.setTaxableIncome(payeCalculation.taxableIncome());
        payslip.setIncomeTaxBeforeCredits(payeCalculation.incomeTaxBeforeCredits());
        payslip.setTaxCreditsApplied(payeCalculation.taxCreditsApplied());
        payslip.setAidsLevy(payeCalculation.aidsLevy());
        payslip.setPayeRuleVersion(payeCalculation.ruleVersion());
        payslip.setTotalDeductions(totalDeductions);
        payslip.setNetSalary(basicSalarySnapshot.subtract(totalDeductions));
        return payslip;
    }
    private PayrollRunResponse mapPayrollRun(PayrollRun payrollRun) {
        return PayrollRunResponse.builder()
                .id(payrollRun.getId())
                .month(payrollRun.getMonth())
                .year(payrollRun.getYear())
                .currency(payrollRun.getCurrency())
                .status(payrollRun.getStatus())
                .createdAt(payrollRun.getCreatedAt())
                .processedAt(payrollRun.getProcessedAt())
                .build();
    }

    private PayslipResponse mapPayslip(Payslip payslip) {
        Employee employee = payslip.getEmployee();
        return PayslipResponse.builder()
                .id(payslip.getId())
                .employeeId(employee.getId())
                .employeeNumber(employee.getEmployeeNumber())
                .employeeName(employee.getFirstName() + " " + employee.getLastName())
                .payrollRunId(payslip.getPayrollRun().getId())
                .month(payslip.getPayrollRun().getMonth())
                .year(payslip.getPayrollRun().getYear())
                .currency(payslip.getCurrency())
                .basicSalary(payslip.getBasicSalary())
                .grossSalary(payslip.getGrossSalary())
                .pensionableEarnings(payslip.getPensionableEarnings())
                .employeeNssaContribution(payslip.getEmployeeNssaContribution())
                .employerNssaContribution(payslip.getEmployerNssaContribution())
                .nssaRuleVersion(payslip.getNssaRuleVersion())
                .payeDeduction(payslip.getPayeDeduction())
                .taxableIncome(orDefault(payslip.getTaxableIncome(), payslip.getGrossSalary()))
                .incomeTaxBeforeCredits(orZero(payslip.getIncomeTaxBeforeCredits()))
                .taxCreditsApplied(orZero(payslip.getTaxCreditsApplied()))
                .aidsLevy(orZero(payslip.getAidsLevy()))
                .payeRuleVersion(payslip.getPayeRuleVersion() == null
                        ? "LEGACY_ZERO"
                        : payslip.getPayeRuleVersion())
                .totalDeductions(payslip.getTotalDeductions())
                .netSalary(payslip.getNetSalary())
                .createdAt(payslip.getCreatedAt())
                .build();
    }

    private BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal orDefault(BigDecimal value, BigDecimal fallback) {
        return value == null ? fallback : value;
    }
}
