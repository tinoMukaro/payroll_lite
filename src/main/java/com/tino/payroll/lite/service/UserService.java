package com.tino.payroll.lite.service;

import com.tino.payroll.lite.dto.CreateInternalUserRequest;
import com.tino.payroll.lite.dto.UpdateUserRoleRequest;
import com.tino.payroll.lite.dto.UserResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final EmployeeRepo employeeRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;


    // -----------------------------------------------------
    // GET ALL USERS
    // ----------------------------------------------------
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers(){
        return userRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // -----------------------------------------------------
    // CREATE INTERNAL USER
    // ----------------------------------------------------
    @Transactional
    public UserResponse createInternalUser(CreateInternalUserRequest request) {
        if (request.getRole() != Role.HR && request.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Internal users must have the HR or ADMIN role");
        }

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
                throw new IllegalArgumentException("A terminated employee cannot receive an internal account");
            }
        }

        User user = User.builder()
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .enabled(true)
                .build();
        User savedUser = userRepository.save(user);

        if (matchingEmployee != null) {
            matchingEmployee.setUser(savedUser);
            employeeRepo.save(matchingEmployee);
        }

        auditService.record(
                AuditAction.INTERNAL_USER_CREATED,
                AuditEntityType.USER,
                savedUser.getId(),
                "Created internal %s account for %s%s".formatted(
                        savedUser.getRole(),
                        savedUser.getEmail(),
                        matchingEmployee == null ? "" : " and linked employee record"
                )
        );
        return mapToResponse(savedUser);
    }

    // -----------------------------------------------------
    // UPDATE USER ROLE
    // ----------------------------------------------------
    @Transactional
    public UserResponse updateUserRole(Long id, UpdateUserRoleRequest request){
        User user = findUserById(id);

        if (user.getRole() == request.getRole()) {
            return mapToResponse(user);
        }
        if (user.getRole() == Role.ADMIN
                && request.getRole() != Role.ADMIN
                && userRepository.countByRole(Role.ADMIN) <= 1) {
            throw new LastAdministratorException(
                    "The last administrator cannot be assigned a different role"
            );
        }

        Role previousRole = user.getRole();
        user.setRole(request.getRole());

        User updatedUser = userRepository.save(user);
        auditService.record(
                AuditAction.USER_ROLE_CHANGED,
                AuditEntityType.USER,
                updatedUser.getId(),
                "Changed role for %s from %s to %s".formatted(
                        updatedUser.getEmail(), previousRole, updatedUser.getRole()
                )
        );
        return mapToResponse(updatedUser);
    }

    // -----------------------------------------------------
    // FIND USER BY ID
    // ----------------------------------------------------
    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + id));
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .employeeId(employeeRepo.findByUserId(user.getId())
                        .map(employee -> employee.getId())
                        .orElse(null))
                .build();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }




}
