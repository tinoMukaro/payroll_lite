package com.tino.payroll.lite.service;


import com.tino.payroll.lite.dto.CreateEmployeeRequest;
import com.tino.payroll.lite.dto.EmployeeResponse;
import com.tino.payroll.lite.entity.Employee;
import com.tino.payroll.lite.exception.EmployeeNotFoundException;
import com.tino.payroll.lite.repository.EmployeeRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepo employeeRepo;

    // -----------------------------------------------------
    // CREATE EMPLOYEE
    // ----------------------------------------------------
    public EmployeeResponse createEmployee(CreateEmployeeRequest request){
        // Validate email uniqueness
        if (employeeRepo.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        // Validate employee number uniqueness
        if (employeeRepo.existsByEmployeeNumber(request.getEmployeeNumber())) {
            throw new IllegalArgumentException("Employee number already exists");
        }
        Employee employee = Employee.builder()
                .employeeNumber(request.getEmployeeNumber())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .jobTitle(request.getJobTitle())
                .basicSalary(request.getBasicSalary())
                .hireDate(request.getHireDate())
                .build();

        Employee savedEmployee = employeeRepo.save(employee);

        return mapToResponse(savedEmployee);
    }


    // -----------------------------------------------------
    // GET ALL EMPLOYEES
    // ----------------------------------------------------
    public List<EmployeeResponse> getAllEmployees() {

        return employeeRepo.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
    // -----------------------------------------------------
    // GET EMPLOYEE BY ID
    // ----------------------------------------------------
    public EmployeeResponse getEmployeeById(Long id) {

        Employee employee = employeeRepo.findById(id)
                .orElseThrow(() ->
                        new EmployeeNotFoundException(
                                "Employee not found with ID: " + id
                        )
                );

        return mapToResponse(employee);
    }

    // -----------------------------------------------------
    // UPDATE EMPLOYEE
    // ----------------------------------------------------
    public EmployeeResponse updateEmployee(
            Long id,
            CreateEmployeeRequest request
    ) {

        Employee employee = employeeRepo.findById(id)
                .orElseThrow(() ->
                        new EmployeeNotFoundException(
                                "Employee not found with ID: " + id
                        )
                );

        employee.setEmployeeNumber(request.getEmployeeNumber());
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setEmail(request.getEmail());
        employee.setJobTitle(request.getJobTitle());
        employee.setBasicSalary(request.getBasicSalary());
        employee.setHireDate(request.getHireDate());

        Employee updatedEmployee = employeeRepo.save(employee);

        return mapToResponse(updatedEmployee);
    }
    // -----------------------------------------------------
    // DELETE EMPLOYEE
    // ----------------------------------------------------
    public void deleteEmployee(Long id) {

        Employee employee = employeeRepo.findById(id)
                .orElseThrow(() ->
                        new EmployeeNotFoundException(
                                "Employee not found with ID: " + id
                        )
                );

        employeeRepo.delete(employee);
    }

    //helper function
    private EmployeeResponse mapToResponse(Employee employee) {

        return EmployeeResponse.builder()
                .id(employee.getId())
                .employeeNumber(employee.getEmployeeNumber())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .email(employee.getEmail())
                .jobTitle(employee.getJobTitle())
                .basicSalary(employee.getBasicSalary())
                .hireDate(employee.getHireDate())
                .build();
    }
}
