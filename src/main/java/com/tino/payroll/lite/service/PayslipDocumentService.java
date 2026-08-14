package com.tino.payroll.lite.service;

import com.tino.payroll.lite.dto.PayslipDocument;
import com.tino.payroll.lite.entity.Payslip;
import com.tino.payroll.lite.entity.User;
import com.tino.payroll.lite.enums.AuditAction;
import com.tino.payroll.lite.enums.AuditEntityType;
import com.tino.payroll.lite.enums.Role;
import com.tino.payroll.lite.exception.PayslipNotFoundException;
import com.tino.payroll.lite.repository.PayslipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PayslipDocumentService {

    private final PayslipRepository payslipRepository;
    private final PayslipPdfService payslipPdfService;
    private final AuditService auditService;

    @Transactional
    public PayslipDocument generate(Long payslipId, User requester) {
        Payslip payslip = payslipRepository.findForDownload(payslipId)
                .orElseThrow(() -> new PayslipNotFoundException(
                        "Payslip not found with ID: " + payslipId
                ));
        authorize(payslip, requester);
        String filename = filename(payslip);
        PayslipDocument document = new PayslipDocument(filename, payslipPdfService.generate(payslip));
        auditService.recordFor(
                requester,
                AuditAction.PAYSLIP_DOWNLOADED,
                AuditEntityType.PAYSLIP,
                payslip.getId(),
                "Downloaded " + filename
        );
        return document;
    }

    private void authorize(Payslip payslip, User requester) {
        if (requester.getRole() == Role.ADMIN || requester.getRole() == Role.HR) {
            return;
        }
        User linkedUser = payslip.getEmployee().getUser();
        if (requester.getRole() != Role.EMPLOYEE
                || linkedUser == null
                || !linkedUser.getId().equals(requester.getId())) {
            throw new AccessDeniedException("You can only download your own payslips");
        }
    }

    private String filename(Payslip payslip) {
        return "Payslip-%s-%04d-%02d.pdf".formatted(
                payslip.getEmployee().getEmployeeNumber(),
                payslip.getPayrollRun().getYear(),
                payslip.getPayrollRun().getMonth()
        );
    }
}
