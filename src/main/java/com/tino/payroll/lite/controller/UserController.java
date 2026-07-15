package com.tino.payroll.lite.controller;


import com.tino.payroll.lite.dto.UpdateUserRoleRequest;
import com.tino.payroll.lite.dto.UserResponse;
import com.tino.payroll.lite.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

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
