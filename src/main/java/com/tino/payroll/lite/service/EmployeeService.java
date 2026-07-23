package com.tino.payroll.lite.service;

import com.tino.payroll.lite.dto.CreateEmployeeRequest;
import com.tino.payroll.lite.dto.EmployeeResponse;
import com.tino.payroll.lite.dto.UpdateEmployeeRequest;
import com.tino.payroll.lite.entity.Employee;
import com.tino.payroll.lite.entity.User;
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
                .hireDate(request.getHireDate())
                .status(request.getStatus() == null ? EmployeeStatus.ACTIVE : request.getStatus())
                .user(matchingUser)
                .build();

        Employee savedEmployee = employeeRepo.saveAndFlush(employee);
        savedEmployee.setEmployeeNumber("EMP-%06d".formatted(savedEmployee.getId()));
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
        employee.setHireDate(request.getHireDate());
        if (request.getStatus() != null) employee.setStatus(request.getStatus());

        if (linkedUser != null) {
            linkedUser.setFirstName(employee.getFirstName());
            linkedUser.setLastName(employee.getLastName());
            linkedUser.setEmail(email);
            if (employee.getStatus() == EmployeeStatus.TERMINATED) linkedUser.setEnabled(false);
            userRepository.save(linkedUser);
        }

        return mapToResponse(employeeRepo.save(employee));
    }

    @Transactional
    public void deleteEmployee(Long id) {
        employeeRepo.delete(findEmployee(id));
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
                .hireDate(employee.getHireDate())
                .status(employee.getStatus())
                .userId(user == null ? null : user.getId())
                .accountLinked(user != null)
                .build();
    }
}