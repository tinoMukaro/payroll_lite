package com.tino.payroll.lite.service;


import com.tino.payroll.lite.dto.AuthResponse;
import com.tino.payroll.lite.dto.LoginRequest;
import com.tino.payroll.lite.dto.RegisterUserRequest;
import com.tino.payroll.lite.dto.UserResponse;
import com.tino.payroll.lite.entity.User;
import com.tino.payroll.lite.enums.Role;
import com.tino.payroll.lite.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // -----------------------------------------------------
    // REGISTER USER
    // ----------------------------------------------------
    public UserResponse register(RegisterUserRequest request){
        if (userRepository.existsByEmail(request.getEmail())){
            throw new IllegalArgumentException("Email is already registered");
        }
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(hashedPassword)
                .role(Role.EMPLOYEE)
                .enabled(true)
                .build();
        User savedUser = userRepository.save(user);

        return mapToResponse(savedUser);
    }

    //helper
    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .build();
    }
    // -----------------------------------------------------
    // USER LOGIN
    // ----------------------------------------------------
public AuthResponse login(LoginRequest request) {

    User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() ->
                    new IllegalArgumentException("Invalid email or password")
            );

    boolean passwordMatches = passwordEncoder.matches(
            request.getPassword(),
            user.getPassword()
    );

    if (!passwordMatches) {
        throw new IllegalArgumentException("Invalid email or password");
    }

    if (!user.isEnabled()) {
        throw new IllegalArgumentException("Account is disabled");
    }

    String token = jwtService.generateToken(user);

    return AuthResponse.builder()
            .token(token)
            .user(mapToResponse(user))
            .build();
}

    // -----------------------------------------------------
    // GET LOGGED IN USER
    // ----------------------------------------------------
    public UserResponse getCurrentUser(Authentication authentication){
        User user = (User) authentication.getPrincipal();
        return mapToResponse(user);
    }

}
