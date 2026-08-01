package com.tino.payroll.lite.controller;

import com.tino.payroll.lite.dto.PayslipResponse;
import com.tino.payroll.lite.dto.PayslipDocument;
import com.tino.payroll.lite.entity.User;
import com.tino.payroll.lite.service.PayrollService;
import com.tino.payroll.lite.service.PayslipDocumentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Payslips", description = "Employee self-service payslip APIs")
@RequestMapping("/api/payslips")
@RequiredArgsConstructor
public class PayslipController {

    private final PayrollService payrollService;
    private final PayslipDocumentService payslipDocumentService;
   // -----------------------------------------------------
    // GET PAYSLIPS (USER ENDPOINT)
    // ----------------------------------------------------
    @GetMapping("/me")
    public ResponseEntity<List<PayslipResponse>> getMyPayslips(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(payrollService.getPayslipsForUser(user.getId()));
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadPayslip(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        PayslipDocument document = payslipDocumentService.generate(id, user);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + document.filename() + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.APPLICATION_PDF)
                .body(document.content());
    }
}
