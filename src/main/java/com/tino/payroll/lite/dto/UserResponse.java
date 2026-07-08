package com.tino.payroll.lite.dto;

import com.tino.payroll.lite.enums.Role;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
    private boolean enabled;
}
