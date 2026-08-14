package com.tino.payroll.lite.util;

import com.tino.payroll.lite.entity.User;
import com.tino.payroll.lite.enums.Role;
import com.tino.payroll.lite.repository.UserRepository;
import com.tino.payroll.lite.service.JwtService;
import com.tino.payroll.lite.utlil.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FilterChain filterChain;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void grantsRoleAdminForAValidAdminToken() throws Exception {
        User admin = User.builder()
                .id(1L)
                .email("admin@example.com")
                .role(Role.ADMIN)
                .enabled(true)
                .build();
        when(jwtService.extractEmail("valid-token")).thenReturn(admin.getEmail());
        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(jwtService.isTokenValid("valid-token", admin)).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/audit-events");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userRepository);

        filter.doFilter(request, response, filterChain);

        assertEquals(
                "ROLE_ADMIN",
                SecurityContextHolder.getContext().getAuthentication()
                        .getAuthorities().iterator().next().getAuthority()
        );
        verify(filterChain).doFilter(request, response);
    }
}
