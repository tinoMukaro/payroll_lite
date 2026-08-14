package com.tino.payroll.lite.config;

import com.tino.payroll.lite.entity.User;
import com.tino.payroll.lite.enums.Role;
import com.tino.payroll.lite.repository.UserRepository;
import com.tino.payroll.lite.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BootstrapAdminInitializerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuditService auditService;

    private BootstrapAdminInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = initializer(true, "admin@example.com", "strong-password");
    }

    @Test
    void createsAdministratorWhenNoneExists() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("strong-password")).thenReturn("encoded");

        initializer.run(null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(Role.ADMIN, captor.getValue().getRole());
        assertEquals("encoded", captor.getValue().getPassword());
        assertTrue(captor.getValue().isEnabled());
    }

    @Test
    void leavesTheDatabaseUntouchedWhenAnAdministratorExists() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(true);

        initializer.run(null);

        verify(userRepository, never()).save(any());
    }

    @Test
    void promotesMatchingAccountWhenNoAdministratorExists() {
        User existing = User.builder().role(Role.EMPLOYEE).enabled(true).build();
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase("admin@example.com"))
                .thenReturn(Optional.of(existing));

        initializer.run(null);

        assertEquals(Role.ADMIN, existing.getRole());
        verify(userRepository).save(existing);
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void rejectsAnUnsafeBootstrapPassword() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        BootstrapAdminInitializer unsafe = initializer(true, "admin@example.com", "short");

        assertThrows(IllegalStateException.class, () -> unsafe.run(null));
    }

    private BootstrapAdminInitializer initializer(boolean enabled, String email, String password) {
        return new BootstrapAdminInitializer(
                userRepository,
                passwordEncoder,
                auditService,
                enabled,
                email,
                password,
                "System",
                "Administrator"
        );
    }
}
