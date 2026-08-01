package com.tino.payroll.lite.service;

import com.tino.payroll.lite.entity.Employee;
import com.tino.payroll.lite.entity.PayrollRun;
import com.tino.payroll.lite.entity.Payslip;
import com.tino.payroll.lite.entity.PayslipLineItem;
import com.tino.payroll.lite.enums.CurrencyCode;
import com.tino.payroll.lite.enums.PayItemSource;
import com.tino.payroll.lite.enums.PayrollAdjustmentType;
import com.tino.payroll.lite.enums.PayrollStatus;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayslipPdfServiceTest {

    private final PayslipPdfService service = new PayslipPdfService();

    @Test
    void generatesReadableItemisedPayslipPdf() throws Exception {
        byte[] pdf = service.generate(samplePayslip());

        assertTrue(pdf.length > 1_000);
        assertEquals("%PDF", new String(pdf, 0, 4));
        try (var document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("PAYROLL LITE"));
            assertTrue(text.contains("Ada Moyo"));
            assertTrue(text.contains("Housing allowance"));
            assertTrue(text.contains("Loan repayment"));
            assertTrue(text.contains("NET PAY"));
            assertTrue(text.contains("1,192.25"));
        }

        if (Boolean.getBoolean("payslip.fixture")) {
            Path output = Path.of("target", "test-output", "payslip-sample.pdf");
            Files.createDirectories(output.getParent());
            Files.write(output, pdf);
            try (var document = Loader.loadPDF(pdf)) {
                PDFRenderer renderer = new PDFRenderer(document);
                ImageIO.write(renderer.renderImageWithDPI(0, 144), "PNG",
                        output.resolveSibling("payslip-sample-page-1.png").toFile());
            }
        }
    }

    private Payslip samplePayslip() {
        Employee employee = Employee.builder()
                .id(10L)
                .employeeNumber("EMP-000010")
                .firstName("Ada")
                .lastName("Moyo")
                .jobTitle("Developer")
                .basicSalary(new BigDecimal("1500.00"))
                .salaryCurrency(CurrencyCode.USD)
                .build();
        PayrollRun run = new PayrollRun();
        run.setId(1L);
        run.setMonth(8);
        run.setYear(2026);
        run.setCurrency(CurrencyCode.USD);
        run.setStatus(PayrollStatus.PROCESSED);

        Payslip payslip = new Payslip();
        payslip.setId(20L);
        payslip.setEmployee(employee);
        payslip.setPayrollRun(run);
        payslip.setCurrency(CurrencyCode.USD);
        payslip.setBasicSalary(new BigDecimal("1500.00"));
        payslip.setGrossSalary(new BigDecimal("1750.00"));
        payslip.setTaxableIncome(new BigDecimal("1700.00"));
        payslip.setPensionableEarnings(new BigDecimal("1000.00"));
        payslip.setEmployeeNssaContribution(new BigDecimal("45.00"));
        payslip.setEmployerNssaContribution(new BigDecimal("45.00"));
        payslip.setNssaRuleVersion("NSSA-USD-2026");
        payslip.setIncomeTaxBeforeCredits(new BigDecimal("425.00"));
        payslip.setTaxCreditsApplied(BigDecimal.ZERO);
        payslip.setAidsLevy(new BigDecimal("12.75"));
        payslip.setPayeDeduction(new BigDecimal("437.75"));
        payslip.setPayeRuleVersion("PAYE-USD-2026");
        payslip.setTotalDeductions(new BigDecimal("557.75"));
        payslip.setNetSalary(new BigDecimal("1192.25"));
        payslip.addLineItem(line(
                "Housing allowance", "200.00", PayrollAdjustmentType.EARNING,
                true, PayItemSource.RECURRING
        ));
        payslip.addLineItem(line(
                "Travel reimbursement", "50.00", PayrollAdjustmentType.EARNING,
                false, PayItemSource.ONE_OFF
        ));
        payslip.addLineItem(line(
                "Loan repayment", "75.00", PayrollAdjustmentType.DEDUCTION,
                false, PayItemSource.RECURRING
        ));
        return payslip;
    }

    private PayslipLineItem line(
            String description,
            String amount,
            PayrollAdjustmentType type,
            boolean taxable,
            PayItemSource source
    ) {
        PayslipLineItem item = new PayslipLineItem();
        item.setDescription(description);
        item.setAmount(new BigDecimal(amount));
        item.setType(type);
        item.setTaxable(taxable);
        item.setSource(source);
        return item;
    }
}
