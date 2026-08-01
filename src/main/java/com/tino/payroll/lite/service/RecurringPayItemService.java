package com.tino.payroll.lite.service;

import com.tino.payroll.lite.dto.RecurringPayItemRequest;
import com.tino.payroll.lite.dto.RecurringPayItemResponse;
import com.tino.payroll.lite.entity.Employee;
import com.tino.payroll.lite.entity.RecurringPayItem;
import com.tino.payroll.lite.enums.PayrollAdjustmentType;
import com.tino.payroll.lite.exception.EmployeeNotFoundException;
import com.tino.payroll.lite.exception.RecurringPayItemNotFoundException;
import com.tino.payroll.lite.repository.EmployeeRepo;
import com.tino.payroll.lite.repository.RecurringPayItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecurringPayItemService {

    private final RecurringPayItemRepository payItemRepository;
    private final EmployeeRepo employeeRepo;

    @Transactional(readOnly = true)
    public List<RecurringPayItemResponse> list(Long employeeId) {
        findEmployee(employeeId);
        return payItemRepository.findByEmployeeIdOrderByActiveDescEffectiveFromDescIdDesc(employeeId)
                .stream()
                .map(this::mapResponse)
                .toList();
    }

    @Transactional
    public RecurringPayItemResponse create(Long employeeId, RecurringPayItemRequest request) {
        Employee employee = findEmployee(employeeId);
        validate(request);

        RecurringPayItem item = new RecurringPayItem();
        item.setEmployee(employee);
        applyRequest(item, request);
        return mapResponse(payItemRepository.save(item));
    }

    @Transactional
    public RecurringPayItemResponse update(
            Long employeeId,
            Long payItemId,
            RecurringPayItemRequest request
    ) {
        findEmployee(employeeId);
        validate(request);
        RecurringPayItem item = payItemRepository.findByIdAndEmployeeId(payItemId, employeeId)
                .orElseThrow(() -> new RecurringPayItemNotFoundException(
                        "Recurring pay item not found with ID: " + payItemId
                ));
        applyRequest(item, request);
        return mapResponse(payItemRepository.save(item));
    }

    private void validate(RecurringPayItemRequest request) {
        if (request.getEffectiveTo() != null
                && request.getEffectiveTo().isBefore(request.getEffectiveFrom())) {
            throw new IllegalArgumentException("Effective-to date cannot be before effective-from date");
        }
        if (request.getType() == PayrollAdjustmentType.DEDUCTION && request.isTaxable()) {
            throw new IllegalArgumentException("Only earnings can be marked as taxable");
        }
    }

    private void applyRequest(RecurringPayItem item, RecurringPayItemRequest request) {
        item.setType(request.getType());
        item.setDescription(request.getDescription().trim());
        item.setAmount(request.getAmount());
        item.setTaxable(request.getType() == PayrollAdjustmentType.EARNING && request.isTaxable());
        item.setEffectiveFrom(request.getEffectiveFrom());
        item.setEffectiveTo(request.getEffectiveTo());
        item.setActive(request.getActive());
    }

    private Employee findEmployee(Long employeeId) {
        return employeeRepo.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException(
                        "Employee not found with ID: " + employeeId
                ));
    }

    private RecurringPayItemResponse mapResponse(RecurringPayItem item) {
        Employee employee = item.getEmployee();
        return RecurringPayItemResponse.builder()
                .id(item.getId())
                .employeeId(employee.getId())
                .employeeNumber(employee.getEmployeeNumber())
                .employeeName(employee.getFirstName() + " " + employee.getLastName())
                .currency(employee.getSalaryCurrency())
                .type(item.getType())
                .description(item.getDescription())
                .amount(item.getAmount())
                .taxable(item.isTaxable())
                .effectiveFrom(item.getEffectiveFrom())
                .effectiveTo(item.getEffectiveTo())
                .active(item.isActive())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
