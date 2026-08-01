package com.tino.payroll.lite.service;

import com.tino.payroll.lite.dto.PayrollAdjustmentRequest;
import com.tino.payroll.lite.dto.PayrollAdjustmentResponse;
import com.tino.payroll.lite.entity.Employee;
import com.tino.payroll.lite.entity.PayrollAdjustment;
import com.tino.payroll.lite.entity.PayrollRun;
import com.tino.payroll.lite.enums.EmployeeStatus;
import com.tino.payroll.lite.enums.PayrollAdjustmentType;
import com.tino.payroll.lite.enums.PayrollStatus;
import com.tino.payroll.lite.exception.EmployeeNotFoundException;
import com.tino.payroll.lite.exception.InvalidPayrollStateException;
import com.tino.payroll.lite.exception.PayrollAdjustmentNotFoundException;
import com.tino.payroll.lite.exception.PayrollRunNotFoundException;
import com.tino.payroll.lite.repository.EmployeeRepo;
import com.tino.payroll.lite.repository.PayrollAdjustmentRepository;
import com.tino.payroll.lite.repository.PayrollRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PayrollAdjustmentService {

    private final PayrollAdjustmentRepository adjustmentRepository;
    private final PayrollRunRepository payrollRunRepository;
    private final EmployeeRepo employeeRepo;

    @Transactional(readOnly = true)
    public List<PayrollAdjustmentResponse> list(Long payrollRunId) {
        findPayrollRun(payrollRunId);
        return adjustmentRepository.findByPayrollRunIdOrderByIdAsc(payrollRunId).stream()
                .map(this::mapResponse)
                .toList();
    }

    @Transactional
    public PayrollAdjustmentResponse create(Long payrollRunId, PayrollAdjustmentRequest request) {
        PayrollRun payrollRun = findPayrollRun(payrollRunId);
        requireDraft(payrollRun);

        Employee employee = employeeRepo.findById(request.getEmployeeId())
                .orElseThrow(() -> new EmployeeNotFoundException(
                        "Employee not found with ID: " + request.getEmployeeId()
                ));
        validateEmployee(employee, payrollRun);
        if (request.getType() == PayrollAdjustmentType.DEDUCTION && request.isTaxable()) {
            throw new IllegalArgumentException("Only earnings can be marked as taxable");
        }

        PayrollAdjustment adjustment = new PayrollAdjustment();
        adjustment.setPayrollRun(payrollRun);
        adjustment.setEmployee(employee);
        adjustment.setType(request.getType());
        adjustment.setDescription(request.getDescription().trim());
        adjustment.setAmount(request.getAmount());
        adjustment.setTaxable(request.getType() == PayrollAdjustmentType.EARNING && request.isTaxable());
        return mapResponse(adjustmentRepository.save(adjustment));
    }

    @Transactional
    public void delete(Long payrollRunId, Long adjustmentId) {
        PayrollRun payrollRun = findPayrollRun(payrollRunId);
        requireDraft(payrollRun);
        PayrollAdjustment adjustment = adjustmentRepository
                .findByIdAndPayrollRunId(adjustmentId, payrollRunId)
                .orElseThrow(() -> new PayrollAdjustmentNotFoundException(
                        "Payroll adjustment not found with ID: " + adjustmentId
                ));
        adjustmentRepository.delete(adjustment);
    }

    private PayrollRun findPayrollRun(Long id) {
        return payrollRunRepository.findById(id)
                .orElseThrow(() -> new PayrollRunNotFoundException("Payroll run not found with ID: " + id));
    }

    private void requireDraft(PayrollRun payrollRun) {
        if (payrollRun.getStatus() != PayrollStatus.DRAFT) {
            throw new InvalidPayrollStateException(
                    "Adjustments can only be changed while the payroll run is DRAFT"
            );
        }
    }

    private void validateEmployee(Employee employee, PayrollRun payrollRun) {
        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new IllegalArgumentException("Adjustments can only be added for active employees");
        }
        if (employee.getSalaryCurrency() != payrollRun.getCurrency()) {
            throw new IllegalArgumentException(
                    "Employee salary currency must match the payroll run currency"
            );
        }
    }

    private PayrollAdjustmentResponse mapResponse(PayrollAdjustment adjustment) {
        Employee employee = adjustment.getEmployee();
        return PayrollAdjustmentResponse.builder()
                .id(adjustment.getId())
                .payrollRunId(adjustment.getPayrollRun().getId())
                .employeeId(employee.getId())
                .employeeNumber(employee.getEmployeeNumber())
                .employeeName(employee.getFirstName() + " " + employee.getLastName())
                .type(adjustment.getType())
                .description(adjustment.getDescription())
                .amount(adjustment.getAmount())
                .taxable(adjustment.isTaxable())
                .createdAt(adjustment.getCreatedAt())
                .build();
    }
}
