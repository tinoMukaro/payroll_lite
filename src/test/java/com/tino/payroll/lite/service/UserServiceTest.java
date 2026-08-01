package com.tino.payroll.lite.service;

import com.tino.payroll.lite.dto.UpdateUserRoleRequest;
import com.tino.payroll.lite.entity.Employee;
import com.tino.payroll.lite.entity.User;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private EmployeeRepo employeeRepo;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, employeeRepo);
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
}
