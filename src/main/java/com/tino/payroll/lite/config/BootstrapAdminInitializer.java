package com.tino.payroll.lite.config;

import com.tino.payroll.lite.entity.User;
import com.tino.payroll.lite.enums.Role;
import com.tino.payroll.lite.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class BootstrapAdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;
    private final String email;
    private final String password;
    private final String firstName;
    private final String lastName;

    public BootstrapAdminInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap-admin.enabled:false}") boolean enabled,
            @Value("${app.bootstrap-admin.email:}") String email,
            @Value("${app.bootstrap-admin.password:}") String password,
            @Value("${app.bootstrap-admin.first-name:System}") String firstName,
            @Value("${app.bootstrap-admin.last-name:Administrator}") String lastName
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled || userRepository.existsByRole(Role.ADMIN)) {
            return;
        }

        validateConfiguration();
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        User admin = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .map(this::promoteExistingUser)
                .orElseGet(() -> createAdministrator(normalizedEmail));

        userRepository.save(admin);
        log.info("Initial administrator is ready: {}", normalizedEmail);
    }

    private User promoteExistingUser(User user) {
        user.setRole(Role.ADMIN);
        user.setEnabled(true);
        return user;
    }

    private User createAdministrator(String normalizedEmail) {
        return User.builder()
                .firstName(firstName.trim())
                .lastName(lastName.trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(password))
                .role(Role.ADMIN)
                .enabled(true)
                .build();
    }

    private void validateConfiguration() {
        if (email == null || email.isBlank()) {
            throw new IllegalStateException(
                    "BOOTSTRAP_ADMIN_EMAIL is required when administrator bootstrap is enabled"
            );
        }
        if (password == null || password.length() < 12) {
            throw new IllegalStateException(
                    "BOOTSTRAP_ADMIN_PASSWORD must contain at least 12 characters"
            );
        }
        if (firstName == null || firstName.isBlank() || lastName == null || lastName.isBlank()) {
            throw new IllegalStateException("Bootstrap administrator names cannot be blank");
        }
    }
}
