package com.tino.payroll.lite.service;

import com.tino.payroll.lite.dto.CreateEmployeeRequest;
import com.tino.payroll.lite.dto.EmployeeResponse;
import com.tino.payroll.lite.dto.UpdateEmployeeRequest;
import com.tino.payroll.lite.entity.Employee;
import com.tino.payroll.lite.entity.User;
import com.tino.payroll.lite.enums.AuditAction;
import com.tino.payroll.lite.enums.AuditEntityType;
import com.tino.payroll.lite.enums.EmployeeStatus;
import com.tino.payroll.lite.exception.EmployeeNotFoundException;
import com.tino.payroll.lite.repository.EmployeeRepo;
import com.tino.payroll.lite.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepo employeeRepo;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional
    public EmployeeResponse createEmployee(CreateEmployeeRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (employeeRepo.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("An employee with this email already exists");
        }

        User matchingUser = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (matchingUser != null && employeeRepo.existsByUserId(matchingUser.getId())) {
            throw new IllegalArgumentException("This user account is already linked to another employee");
        }

        Employee employee = Employee.builder()
                .employeeNumber("PENDING-" + UUID.randomUUID())
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(email)
                .jobTitle(request.getJobTitle().trim())
                .basicSalary(request.getBasicSalary())
                .salaryCurrency(request.getSalaryCurrency())
                .hireDate(request.getHireDate())
                .status(request.getStatus() == null ? EmployeeStatus.ACTIVE : request.getStatus())
                .user(matchingUser)
                .build();

        Employee savedEmployee = employeeRepo.saveAndFlush(employee);
        savedEmployee.setEmployeeNumber("EMP-%06d".formatted(savedEmployee.getId()));
        auditService.record(
                AuditAction.EMPLOYEE_CREATED,
                AuditEntityType.EMPLOYEE,
                savedEmployee.getId(),
                "Created employee " + savedEmployee.getEmployeeNumber()
        );
        return mapToResponse(savedEmployee);
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> getAllEmployees() {
        return employeeRepo.findAll().stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long id) {
        return mapToResponse(findEmployee(id));
    }

    @Transactional
    public EmployeeResponse updateEmployee(Long id, UpdateEmployeeRequest request) {
        Employee employee = findEmployee(id);
        EmployeeStatus previousStatus = employee.getStatus();
        String email = normalizeEmail(request.getEmail());

        if (employeeRepo.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new IllegalArgumentException("An employee with this email already exists");
        }

        User linkedUser = employee.getUser();
        if (linkedUser != null && userRepository.existsByEmailIgnoreCaseAndIdNot(email, linkedUser.getId())) {
            throw new IllegalArgumentException("This email belongs to another user account");
        }

        employee.setFirstName(request.getFirstName().trim());
        employee.setLastName(request.getLastName().trim());
        employee.setEmail(email);
        employee.setJobTitle(request.getJobTitle().trim());
        employee.setBasicSalary(request.getBasicSalary());
        employee.setSalaryCurrency(request.getSalaryCurrency());
        employee.setHireDate(request.getHireDate());
        if (request.getStatus() != null) employee.setStatus(request.getStatus());

        if (linkedUser != null) {
            linkedUser.setFirstName(employee.getFirstName());
            linkedUser.setLastName(employee.getLastName());
            linkedUser.setEmail(email);
            if (employee.getStatus() == EmployeeStatus.TERMINATED) linkedUser.setEnabled(false);
            userRepository.save(linkedUser);
        }

        Employee savedEmployee = employeeRepo.save(employee);
        auditService.record(
                AuditAction.EMPLOYEE_UPDATED,
                AuditEntityType.EMPLOYEE,
                savedEmployee.getId(),
                "Updated employee %s (status %s -> %s)".formatted(
                        savedEmployee.getEmployeeNumber(), previousStatus, savedEmployee.getStatus()
                )
        );
        return mapToResponse(savedEmployee);
    }

    @Transactional
    public void deleteEmployee(Long id) {
        Employee employee = findEmployee(id);
        employeeRepo.delete(employee);
        auditService.record(
                AuditAction.EMPLOYEE_DELETED,
                AuditEntityType.EMPLOYEE,
                employee.getId(),
                "Deleted employee " + employee.getEmployeeNumber()
        );
    }

    private Employee findEmployee(Long id) {
        return employeeRepo.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found with ID: " + id));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private EmployeeResponse mapToResponse(Employee employee) {
        User user = employee.getUser();
        return EmployeeResponse.builder()
                .id(employee.getId())
                .employeeNumber(employee.getEmployeeNumber())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .email(employee.getEmail())
                .jobTitle(employee.getJobTitle())
                .basicSalary(employee.getBasicSalary())
                .salaryCurrency(employee.getSalaryCurrency())
                .hireDate(employee.getHireDate())
                .status(employee.getStatus())
                .userId(user == null ? null : user.getId())
                .accountLinked(user != null)
                .build();
    }
}