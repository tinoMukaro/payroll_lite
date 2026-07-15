package com.tino.payroll.lite.controller;


import com.tino.payroll.lite.dto.AuthResponse;
import com.tino.payroll.lite.dto.LoginRequest;
import com.tino.payroll.lite.dto.RegisterUserRequest;
import com.tino.payroll.lite.dto.UserResponse;
import com.tino.payroll.lite.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
//register
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterUserRequest request
            ){
        UserResponse user = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(user);
    }

    //login
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    //get me
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            Authentication authentication
    ){
        UserResponse user = authService.getCurrentUser(authentication);

        return ResponseEntity.ok(user);
    }


}
