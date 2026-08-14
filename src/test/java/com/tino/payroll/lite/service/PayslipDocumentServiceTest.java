package com.tino.payroll.lite.service;

import com.tino.payroll.lite.entity.Employee;
import com.tino.payroll.lite.entity.PayrollRun;
import com.tino.payroll.lite.entity.Payslip;
import com.tino.payroll.lite.entity.User;
import com.tino.payroll.lite.enums.Role;
import com.tino.payroll.lite.repository.PayslipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayslipDocumentServiceTest {

    @Mock
    private PayslipRepository payslipRepository;
    @Mock
    private PayslipPdfService payslipPdfService;
    @Mock
    private AuditService auditService;

    private PayslipDocumentService service;

    @BeforeEach
    void setUp() {
        service = new PayslipDocumentService(payslipRepository, payslipPdfService, auditService);
    }

    @Test
    void employeeCanDownloadOwnPayslip() {
        User employeeUser = user(7L, Role.EMPLOYEE);
        Payslip payslip = payslip(employeeUser);
        byte[] pdf = {1, 2, 3};
        when(payslipRepository.findForDownload(20L)).thenReturn(Optional.of(payslip));
        when(payslipPdfService.generate(payslip)).thenReturn(pdf);

        var document = service.generate(20L, employeeUser);

        assertEquals("Payslip-EMP-000010-2026-08.pdf", document.filename());
        assertArrayEquals(pdf, document.content());
    }

    @Test
    void employeeCannotDownloadAnotherEmployeesPayslip() {
        User owner = user(7L, Role.EMPLOYEE);
        User requester = user(8L, Role.EMPLOYEE);
        Payslip payslip = payslip(owner);
        when(payslipRepository.findForDownload(20L)).thenReturn(Optional.of(payslip));

        assertThrows(AccessDeniedException.class, () -> service.generate(20L, requester));

        verify(payslipPdfService, never()).generate(payslip);
    }

    @Test
    void hrCanDownloadAnyPayslip() {
        User owner = user(7L, Role.EMPLOYEE);
        User hr = user(2L, Role.HR);
        Payslip payslip = payslip(owner);
        when(payslipRepository.findForDownload(20L)).thenReturn(Optional.of(payslip));
        when(payslipPdfService.generate(payslip)).thenReturn(new byte[]{1});

        service.generate(20L, hr);

        verify(payslipPdfService).generate(payslip);
    }

    private Payslip payslip(User owner) {
        Employee employee = Employee.builder()
                .id(10L)
                .employeeNumber("EMP-000010")
                .firstName("Ada")
                .lastName("Moyo")
                .user(owner)
                .build();
        PayrollRun run = new PayrollRun();
        run.setMonth(8);
        run.setYear(2026);
        Payslip payslip = new Payslip();
        payslip.setId(20L);
        payslip.setEmployee(employee);
        payslip.setPayrollRun(run);
        return payslip;
    }

    private User user(Long id, Role role) {
        return User.builder().id(id).role(role).build();
    }
}
