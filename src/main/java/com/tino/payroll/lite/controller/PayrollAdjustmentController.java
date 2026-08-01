package com.tino.payroll.lite.controller;

import com.tino.payroll.lite.dto.PayrollAdjustmentRequest;
import com.tino.payroll.lite.dto.PayrollAdjustmentResponse;
import com.tino.payroll.lite.service.PayrollAdjustmentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Payroll adjustments", description = "One-off earning and deduction inputs for draft payroll runs")
@RequestMapping("/api/payroll-runs/{payrollRunId}/adjustments")
@RequiredArgsConstructor
public class PayrollAdjustmentController {

    private final PayrollAdjustmentService adjustmentService;

    @GetMapping
    public ResponseEntity<List<PayrollAdjustmentResponse>> list(
            @PathVariable Long payrollRunId
    ) {
        return ResponseEntity.ok(adjustmentService.list(payrollRunId));
    }

    @PostMapping
    public ResponseEntity<PayrollAdjustmentResponse> create(
            @PathVariable Long payrollRunId,
            @Valid @RequestBody PayrollAdjustmentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adjustmentService.create(payrollRunId, request));
    }

    @DeleteMapping("/{adjustmentId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long payrollRunId,
            @PathVariable Long adjustmentId
    ) {
        adjustmentService.delete(payrollRunId, adjustmentId);
        return ResponseEntity.noContent().build();
    }
}
