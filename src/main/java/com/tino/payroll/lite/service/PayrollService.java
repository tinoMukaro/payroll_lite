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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PayrollService {

    private final PayrollRunRepository payrollRunRepository;
    private final PayslipRepository payslipRepository;
    private final EmployeeRepo employeeRepo;

    @Transactional
    public PayrollRunResponse createPayrollRun(CreatePayrollRunRequest request) {
        if (payrollRunRepository.existsByMonthAndYear(request.getMonth(), request.getYear())) {
            throw new DuplicatePayrollRunException(
                    "A payroll run already exists for month " + request.getMonth() + " and year " + request.getYear()
            );
        }

        PayrollRun payrollRun = new PayrollRun();
        payrollRun.setMonth(request.getMonth());
        payrollRun.setYear(request.getYear());
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

        List<Payslip> payslips = employeeRepo.findAllByStatus(EmployeeStatus.ACTIVE).stream()
                .map(employee -> createPayslip(employee, payrollRun))
                .toList();

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

    private Payslip createPayslip(Employee employee, PayrollRun payrollRun) {
        BigDecimal basicSalarySnapshot = employee.getBasicSalary();
        BigDecimal nssaDeduction = BigDecimal.ZERO;
        BigDecimal payeDeduction = BigDecimal.ZERO;
        BigDecimal totalDeductions = nssaDeduction.add(payeDeduction);

        Payslip payslip = new Payslip();
        payslip.setEmployee(employee);
        payslip.setPayrollRun(payrollRun);
        payslip.setBasicSalary(basicSalarySnapshot);
        payslip.setGrossSalary(basicSalarySnapshot);
        payslip.setNssaDeduction(nssaDeduction);
        payslip.setPayeDeduction(payeDeduction);
        payslip.setTotalDeductions(totalDeductions);
        payslip.setNetSalary(basicSalarySnapshot.subtract(totalDeductions));
        return payslip;
    }

    private PayrollRunResponse mapPayrollRun(PayrollRun payrollRun) {
        return PayrollRunResponse.builder()
                .id(payrollRun.getId())
                .month(payrollRun.getMonth())
                .year(payrollRun.getYear())
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
                .basicSalary(payslip.getBasicSalary())
                .grossSalary(payslip.getGrossSalary())
                .nssaDeduction(payslip.getNssaDeduction())
                .payeDeduction(payslip.getPayeDeduction())
                .totalDeductions(payslip.getTotalDeductions())
                .netSalary(payslip.getNetSalary())
                .createdAt(payslip.getCreatedAt())
                .build();
    }
}
