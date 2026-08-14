package com.tino.payroll.lite.service;

import com.tino.payroll.lite.dto.CreateInternalUserRequest;
import com.tino.payroll.lite.dto.UpdateUserRoleRequest;
import com.tino.payroll.lite.entity.Employee;
import com.tino.payroll.lite.entity.User;
import com.tino.payroll.lite.enums.AuditAction;
import com.tino.payroll.lite.enums.AuditEntityType;
import com.tino.payroll.lite.enums.EmployeeStatus;
import com.tino.payroll.lite.enums.Role;
import com.tino.payroll.lite.exception.LastAdministratorException;
import com.tino.payroll.lite.exception.UserNotFoundException;
import com.tino.payroll.lite.repository.EmployeeRepo;
import com.tino.payroll.lite.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private EmployeeRepo employeeRepo;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuditService auditService;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, employeeRepo, passwordEncoder, auditService);
    }

    @Test
    void adminCanCreateAnInternalUserAndLinkTheirEmployeeRecord() {
        CreateInternalUserRequest request = internalUserRequest(Role.HR);
        Employee employee = Employee.builder()
                .id(10L)
                .email("jane@example.com")
                .status(EmployeeStatus.ACTIVE)
                .build();
        when(employeeRepo.findByEmailIgnoreCase("jane@example.com"))
                .thenReturn(Optional.of(employee));
        when(passwordEncoder.encode("Initial123"))
                .thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(8L);
            return saved;
        });
        when(employeeRepo.findByUserId(8L)).thenReturn(Optional.of(employee));

        var response = service.createInternalUser(request);

        assertEquals("jane@example.com", response.getEmail());
        assertEquals(Role.HR, response.getRole());
        assertEquals(10L, response.getEmployeeId());
        assertEquals("encoded-password", employee.getUser().getPassword());
        verify(employeeRepo).save(employee);
        verify(auditService).record(
                eq(AuditAction.INTERNAL_USER_CREATED),
                eq(AuditEntityType.USER),
                eq(8L),
                contains("jane@example.com")
        );
    }

    @Test
    void employeeRoleCannotBeCreatedThroughTheInternalUserFlow() {
        CreateInternalUserRequest request = internalUserRequest(Role.EMPLOYEE);

        var error = assertThrows(IllegalArgumentException.class,
                () -> service.createInternalUser(request));

        assertEquals("Internal users must have the HR or ADMIN role", error.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void duplicateInternalUserEmailIsRejectedCaseInsensitively() {
        CreateInternalUserRequest request = internalUserRequest(Role.ADMIN);
        when(userRepository.existsByEmailIgnoreCase("jane@example.com")).thenReturn(true);

        var error = assertThrows(IllegalArgumentException.class,
                () -> service.createInternalUser(request));

        assertEquals("Email is already registered", error.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void adminRoleUpdateReturnsTheNewRole() {
        User user = user();
        UpdateUserRoleRequest request = new UpdateUserRoleRequest();
        request.setRole(Role.HR);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(employeeRepo.findByUserId(7L)).thenReturn(Optional.empty());

        var response = service.updateUserRole(7L, request);

        assertEquals(Role.HR, response.getRole());
        verify(userRepository).save(user);
    }

    @Test
    void userResponseIncludesLinkedEmployeeId() {
        User user = user();
        Employee employee = Employee.builder().id(10L).build();
        when(userRepository.findAll()).thenReturn(List.of(user));
        when(employeeRepo.findByUserId(7L)).thenReturn(Optional.of(employee));

        var response = service.getAllUsers().getFirst();

        assertEquals(10L, response.getEmployeeId());
    }

    @Test
    void missingUserReturnsDomainNotFoundError() {
        UpdateUserRoleRequest request = new UpdateUserRoleRequest();
        request.setRole(Role.HR);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> service.updateUserRole(99L, request));
    }

    @Test
    void lastAdministratorCannotBeDemoted() {
        User user = user();
        user.setRole(Role.ADMIN);
        UpdateUserRoleRequest request = new UpdateUserRoleRequest();
        request.setRole(Role.HR);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);

        assertThrows(LastAdministratorException.class,
                () -> service.updateUserRole(7L, request));
    }

    private User user() {
        return User.builder()
                .id(7L)
                .firstName("Ada")
                .lastName("Moyo")
                .email("ada@example.com")
                .role(Role.EMPLOYEE)
                .enabled(true)
                .build();
    }

    private CreateInternalUserRequest internalUserRequest(Role role) {
        CreateInternalUserRequest request = new CreateInternalUserRequest();
        request.setFirstName(" Jane ");
        request.setLastName(" Moyo ");
        request.setEmail(" JANE@EXAMPLE.COM ");
        request.setPassword("Initial123");
        request.setRole(role);
        return request;
    }
}
