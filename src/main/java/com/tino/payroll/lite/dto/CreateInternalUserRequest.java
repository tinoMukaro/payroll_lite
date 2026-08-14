package com.tino.payroll.lite.dto;

import com.tino.payroll.lite.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateInternalUserRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Initial password is required")
    @Size(min = 8, message = "Initial password must contain at least 8 characters")
    private String password;

    @NotNull(message = "Role is required")
    private Role role;
}
