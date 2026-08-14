package com.tino.payroll.lite.service;

import com.tino.payroll.lite.dto.RegisterUserRequest;
import com.tino.payroll.lite.dto.UserResponse;
import com.tino.payroll.lite.entity.Employee;
import com.tino.payroll.lite.entity.User;
import com.tino.payroll.lite.enums.EmployeeStatus;
import com.tino.payroll.lite.repository.EmployeeRepo;
import com.tino.payroll.lite.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private EmployeeRepo employeeRepo;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuditService auditService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, employeeRepo, passwordEncoder, jwtService, auditService);
    }

    @Test
    void registrationLinksAnExistingEmployeeWithTheSameEmail() {
        Employee employee = Employee.builder().id(12L).email("person@example.com").status(EmployeeStatus.ACTIVE).build();
        when(employeeRepo.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.of(employee));
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(5L);
            return user;
        });
        when(employeeRepo.findByUserId(5L)).thenReturn(Optional.of(employee));

        UserResponse response = authService.register(registerRequest("Person@Example.com"));

        assertSame(employee.getUser(), employee.getUser());
        assertNotNull(employee.getUser());
        assertEquals(5L, employee.getUser().getId());
        assertEquals(12L, response.getEmployeeId());
        verify(employeeRepo).save(employee);
    }

    @Test
    void terminatedEmployeeCannotClaimAnAccount() {
        Employee employee = Employee.builder().email("former@example.com").status(EmployeeStatus.TERMINATED).build();
        when(employeeRepo.findByEmailIgnoreCase("former@example.com")).thenReturn(Optional.of(employee));

        assertThrows(IllegalArgumentException.class,
                () -> authService.register(registerRequest("former@example.com")));
        verify(userRepository, never()).save(any());
    }

    private RegisterUserRequest registerRequest(String email) {
        RegisterUserRequest request = new RegisterUserRequest();
        request.setFirstName("Test");
        request.setLastName("Person");
        request.setEmail(email);
        request.setPassword("password123");
        return request;
    }
}
