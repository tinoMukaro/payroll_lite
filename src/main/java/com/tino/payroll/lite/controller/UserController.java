package com.tino.payroll.lite.controller;


import com.tino.payroll.lite.dto.CreateInternalUserRequest;
import com.tino.payroll.lite.dto.UpdateUserRoleRequest;
import com.tino.payroll.lite.dto.UserResponse;
import com.tino.payroll.lite.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "User", description = "User management APIs")
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;


    // -----------------------------------------------------
    // GET ALL USERS
    // ----------------------------------------------------
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // -----------------------------------------------------
    // CREATE INTERNAL USER
    // ----------------------------------------------------
    @PostMapping("/internal")
    public ResponseEntity<UserResponse> createInternalUser(
            @Valid @RequestBody CreateInternalUserRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userService.createInternalUser(request));
    }

    // -----------------------------------------------------
    // UPDATE USER ROLE
    // ----------------------------------------------------
    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request
    ) {
        UserResponse user =
                userService.updateUserRole(id, request);

        return ResponseEntity.ok(user);
    }
}
