package com.tino.payroll.lite.controller;

import com.tino.payroll.lite.dto.CreatePayrollRunRequest;
import com.tino.payroll.lite.dto.PayrollRunResponse;
import com.tino.payroll.lite.dto.PayslipResponse;
import com.tino.payroll.lite.service.PayrollService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Payroll-runs", description = "Payroll management APIs")
@RequestMapping("/api/payroll-runs")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;
   // -----------------------------------------------------
    // CRATE A PAYROLL RUN (DRAFT)
    // ----------------------------------------------------
    @PostMapping
    public ResponseEntity<PayrollRunResponse> createPayrollRun(
            @Valid @RequestBody CreatePayrollRunRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(payrollService.createPayrollRun(request));
    }
   // -----------------------------------------------------
    // GET ALL RUNS
    // ----------------------------------------------------
    @GetMapping
    public ResponseEntity<List<PayrollRunResponse>> getAllPayrollRuns() {
        return ResponseEntity.ok(payrollService.getAllPayrollRuns());
    }
   // -----------------------------------------------------
    // GET RUN BY ID
    // ----------------------------------------------------
    @GetMapping("/{id}")
    public ResponseEntity<PayrollRunResponse> getPayrollRun(@PathVariable Long id) {
        return ResponseEntity.ok(payrollService.getPayrollRun(id));
    }
   // -----------------------------------------------------
    // PROCESS A RUN(FROM -DRAFT -> PROCESSED)
    // ----------------------------------------------------
    @PostMapping("/{id}/process")
    public ResponseEntity<PayrollRunResponse> processPayrollRun(@PathVariable Long id) {
        return ResponseEntity.ok(payrollService.processPayrollRun(id));
    }
   // -----------------------------------------------------
    // GET PAYSLIPS
    // ----------------------------------------------------
    @GetMapping("/{id}/payslips")
    public ResponseEntity<List<PayslipResponse>> getPayslips(@PathVariable Long id) {
        return ResponseEntity.ok(payrollService.getPayslipsForPayrollRun(id));
    }
}
