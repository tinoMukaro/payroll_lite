package com.tino.payroll.lite.service;


import com.tino.payroll.lite.entity.Employee;
import com.tino.payroll.lite.repository.EmployeeRepo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeeService {

    private final EmployeeRepo employeeRepo;

    public EmployeeService(EmployeeRepo employeeRepo) {
        this.employeeRepo = employeeRepo;
    }
    //create an employee
     public Employee createEmployee(Employee employee){
        return employeeRepo.save(employee);
     }
     //get all employees
    public List<Employee> getAllEmployees(){
        return employeeRepo.findAll();
    }
    //get employee by id
    public Employee getEmployeeById(Long id){
        return employeeRepo.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Employee not found with ID: " + id));
    }
    // update employee
    public Employee updateEmployee(Long id, Employee updatedEmployee){
        Employee existingEmployee = getEmployeeById(id);

        existingEmployee.setEmployeeNumber(updatedEmployee.getEmployeeNumber());
        existingEmployee.setFirstName(updatedEmployee.getFirstName());
        existingEmployee.setLastName(updatedEmployee.getLastName());
        existingEmployee.setEmail(updatedEmployee.getEmail());
        existingEmployee.setJobTitle(updatedEmployee.getJobTitle());
        existingEmployee.setBasicSalary(updatedEmployee.getBasicSalary());
        existingEmployee.setHireDate(updatedEmployee.getHireDate());

        return employeeRepo.save(existingEmployee);
    }
    // delete an employee
    public void deleteEmployee(Long id){
        Employee employee = getEmployeeById(id);
        employeeRepo.delete(employee);
    }
}
