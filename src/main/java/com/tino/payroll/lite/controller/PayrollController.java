package com.tino.payroll.lite.controller;

import com.tino.payroll.lite.dto.CreatePayrollRunRequest;
import com.tino.payroll.lite.dto.PayrollRunResponse;
import com.tino.payroll.lite.dto.PayslipResponse;
import com.tino.payroll.lite.service.PayrollService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payroll-runs")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;

    @PostMapping
    public ResponseEntity<PayrollRunResponse> createPayrollRun(
            @Valid @RequestBody CreatePayrollRunRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(payrollService.createPayrollRun(request));
    }

    @GetMapping
    public ResponseEntity<List<PayrollRunResponse>> getAllPayrollRuns() {
        return ResponseEntity.ok(payrollService.getAllPayrollRuns());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PayrollRunResponse> getPayrollRun(@PathVariable Long id) {
        return ResponseEntity.ok(payrollService.getPayrollRun(id));
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<PayrollRunResponse> processPayrollRun(@PathVariable Long id) {
        return ResponseEntity.ok(payrollService.processPayrollRun(id));
    }

    @GetMapping("/{id}/payslips")
    public ResponseEntity<List<PayslipResponse>> getPayslips(@PathVariable Long id) {
        return ResponseEntity.ok(payrollService.getPayslipsForPayrollRun(id));
    }
}
