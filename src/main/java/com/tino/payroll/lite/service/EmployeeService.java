package com.tino.payroll.lite.service;


import com.tino.payroll.lite.dto.CreateEmployeeRequest;
import com.tino.payroll.lite.dto.EmployeeResponse;
import com.tino.payroll.lite.entity.Employee;
import com.tino.payroll.lite.repository.EmployeeRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepo employeeRepo;

//create
    public EmployeeResponse createEmployee(CreateEmployeeRequest request){
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


//get all employees
    public List<EmployeeResponse> getAllEmployees() {

        return employeeRepo.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
    //get employee by id
    public EmployeeResponse getEmployeeById(Long id) {

        Employee employee = employeeRepo.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Employee not found with ID: " + id
                        )
                );

        return mapToResponse(employee);
    }

    // update employee
    public EmployeeResponse updateEmployee(
            Long id,
            CreateEmployeeRequest request
    ) {

        Employee employee = employeeRepo.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
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
    // delete an employee
    public void deleteEmployee(Long id) {

        Employee employee = employeeRepo.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
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
