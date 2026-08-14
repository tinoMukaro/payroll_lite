package com.tino.payroll.lite.service;

import com.tino.payroll.lite.dto.UpdateUserRoleRequest;
import com.tino.payroll.lite.dto.UserResponse;
import com.tino.payroll.lite.entity.User;
import com.tino.payroll.lite.enums.AuditAction;
import com.tino.payroll.lite.enums.AuditEntityType;
import com.tino.payroll.lite.enums.Role;
import com.tino.payroll.lite.exception.LastAdministratorException;
import com.tino.payroll.lite.exception.UserNotFoundException;
import com.tino.payroll.lite.repository.EmployeeRepo;
import com.tino.payroll.lite.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final EmployeeRepo employeeRepo;
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




}
