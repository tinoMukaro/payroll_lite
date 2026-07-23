package com.tino.payroll.lite.service;

import com.tino.payroll.lite.dto.CreateEmployeeRequest;
import com.tino.payroll.lite.dto.EmployeeResponse;
import com.tino.payroll.lite.entity.Employee;
import com.tino.payroll.lite.entity.User;
import com.tino.payroll.lite.enums.Role;
import com.tino.payroll.lite.repository.EmployeeRepo;
import com.tino.payroll.lite.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock private EmployeeRepo employeeRepo;
    @Mock private UserRepository userRepository;
    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        employeeService = new EmployeeService(employeeRepo, userRepository);
    }

    @Test
    void createEmployeeGeneratesNumberFromDatabaseId() {
        CreateEmployeeRequest request = employeeRequest("New.Employee@Example.com");
        when(userRepository.findByEmailIgnoreCase("new.employee@example.com")).thenReturn(Optional.empty());
        when(employeeRepo.saveAndFlush(any(Employee.class))).thenAnswer(invocation -> {
            Employee employee = invocation.getArgument(0);
            employee.setId(42L);
            return employee;
        });

        EmployeeResponse response = employeeService.createEmployee(request);

        assertEquals("EMP-000042", response.getEmployeeNumber());
        assertEquals("new.employee@example.com", response.getEmail());
        assertFalse(response.isAccountLinked());
    }

    @Test
    void createEmployeeLinksAnExistingUserWithTheSameEmail() {
        User user = User.builder().id(7L).email("person@example.com").role(Role.EMPLOYEE).build();
        when(userRepository.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.of(user));
        when(employeeRepo.existsByUserId(7L)).thenReturn(false);
        when(employeeRepo.saveAndFlush(any(Employee.class))).thenAnswer(invocation -> {
            Employee employee = invocation.getArgument(0);
            employee.setId(3L);
            return employee;
        });

        EmployeeResponse response = employeeService.createEmployee(employeeRequest("Person@Example.com"));

        assertTrue(response.isAccountLinked());
        assertEquals(7L, response.getUserId());
    }

    private CreateEmployeeRequest employeeRequest(String email) {
        CreateEmployeeRequest request = new CreateEmployeeRequest();
        request.setFirstName("Test");
        request.setLastName("Person");
        request.setEmail(email);
        request.setJobTitle("Developer");
        request.setBasicSalary(new BigDecimal("1000.00"));
        request.setHireDate(LocalDate.of(2026, 1, 1));
        return request;
    }
}