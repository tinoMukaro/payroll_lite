package com.tino.payroll.lite.controller;

import com.tino.payroll.lite.dto.PayslipResponse;
import com.tino.payroll.lite.entity.User;
import com.tino.payroll.lite.service.PayrollService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Payslips", description = "Employee self-service payslip APIs")
@RequestMapping("/api/payslips")
@RequiredArgsConstructor
public class PayslipController {

    private final PayrollService payrollService;

    @GetMapping("/me")
    public ResponseEntity<List<PayslipResponse>> getMyPayslips(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(payrollService.getPayslipsForUser(user.getId()));
    }
}