package com.tino.payroll.lite.service;

import com.tino.payroll.lite.dto.AuthResponse;
import com.tino.payroll.lite.dto.LoginRequest;
import com.tino.payroll.lite.dto.RegisterUserRequest;
import com.tino.payroll.lite.dto.UserResponse;
import com.tino.payroll.lite.entity.Employee;
import com.tino.payroll.lite.entity.User;
import com.tino.payroll.lite.enums.AuditAction;
import com.tino.payroll.lite.enums.AuditEntityType;
import com.tino.payroll.lite.enums.EmployeeStatus;
import com.tino.payroll.lite.enums.Role;
import com.tino.payroll.lite.repository.EmployeeRepo;
import com.tino.payroll.lite.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final EmployeeRepo employeeRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;

    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }

        Employee matchingEmployee = employeeRepo.findByEmailIgnoreCase(email).orElse(null);
        if (matchingEmployee != null) {
            if (matchingEmployee.getUser() != null) {
                throw new IllegalArgumentException("This employee already has a user account");
            }
            if (matchingEmployee.getStatus() == EmployeeStatus.TERMINATED) {
                throw new IllegalArgumentException("A terminated employee cannot register an account");
            }
        }

        User user = User.builder()
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.EMPLOYEE)
                .enabled(true)
                .build();
        User savedUser = userRepository.save(user);

        if (matchingEmployee != null) {
            matchingEmployee.setUser(savedUser);
            employeeRepo.save(matchingEmployee);
        }

        auditService.recordFor(
                savedUser,
                AuditAction.USER_REGISTERED,
                AuditEntityType.USER,
                savedUser.getId(),
                "Registered user account" + (matchingEmployee == null ? "" : " and linked employee record")
        );
        return mapToResponse(savedUser);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        if (!user.isEnabled()) {
            throw new IllegalArgumentException("Account is disabled");
        }

        return AuthResponse.builder()
                .token(jwtService.generateToken(user))
                .user(mapToResponse(user))
                .build();
    }

    public UserResponse getCurrentUser(Authentication authentication) {
        return mapToResponse((User) authentication.getPrincipal());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private UserResponse mapToResponse(User user) {
        Long employeeId = employeeRepo.findByUserId(user.getId()).map(Employee::getId).orElse(null);
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .employeeId(employeeId)
                .build();
    }
}