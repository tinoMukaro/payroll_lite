package com.tino.payroll.lite.service;

import com.tino.payroll.lite.entity.Employee;
import com.tino.payroll.lite.entity.PayrollRun;
import com.tino.payroll.lite.entity.Payslip;
import com.tino.payroll.lite.entity.PayslipLineItem;
import com.tino.payroll.lite.enums.PayItemSource;
import com.tino.payroll.lite.enums.PayrollAdjustmentType;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Service
public class PayslipPdfService {

    private static final Color NAVY = new Color(23, 36, 61);
    private static final Color BLUE = new Color(46, 91, 145);
    private static final Color TEXT = new Color(37, 50, 74);
    private static final Color MUTED = new Color(102, 112, 133);
    private static final Color LINE = new Color(221, 227, 234);
    private static final Color LIGHT = new Color(247, 249, 251);

    public byte[] generate(Payslip payslip) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream();
             DocumentWriter writer = new DocumentWriter(document)) {
            document.getDocumentInformation().setTitle(filenameTitle(payslip));
            document.getDocumentInformation().setAuthor("Payroll Lite");

            writePayslip(writer, payslip);
            writer.finish();
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not generate payslip PDF", exception);
        }
    }

    private void writePayslip(DocumentWriter writer, Payslip payslip) throws IOException {
        Employee employee = payslip.getEmployee();
        PayrollRun run = payslip.getPayrollRun();
        List<PayslipLineItem> lineItems = payslip.getLineItems() == null
                ? Collections.emptyList()
                : payslip.getLineItems();

        writer.sectionTitle("EMPLOYEE AND PAY PERIOD");
        writer.keyValue("Employee", employee.getFirstName() + " " + employee.getLastName());
        writer.keyValue("Employee number", employee.getEmployeeNumber());
        writer.keyValue("Pay period", payPeriod(run));
        writer.keyValue("Currency", payslip.getCurrency().name());

        writer.sectionTitle("EARNINGS");
        writer.tableHeader("DESCRIPTION", "SOURCE / TAX", "AMOUNT");
        writer.moneyRow("Basic salary", "Base pay - Taxable", payslip.getBasicSalary(), false);
        for (PayslipLineItem item : lineItems) {
            if (item.getType() == PayrollAdjustmentType.EARNING) {
                writer.moneyRow(item.getDescription(), lineMeta(item), item.getAmount(), false);
            }
        }
        writer.moneyRow("Gross salary", "Total earnings", payslip.getGrossSalary(), true);

        writer.sectionTitle("DEDUCTIONS");
        writer.tableHeader("DESCRIPTION", "DETAIL", "AMOUNT");
        writer.moneyRow("NSSA", payslip.getNssaRuleVersion(), payslip.getEmployeeNssaContribution(), false);
        writer.moneyRow("PAYE", payslip.getPayeRuleVersion(), payslip.getPayeDeduction(), false);
        for (PayslipLineItem item : lineItems) {
            if (item.getType() == PayrollAdjustmentType.DEDUCTION) {
                writer.moneyRow(item.getDescription(), lineMeta(item), item.getAmount(), false);
            }
        }
        writer.moneyRow("Total deductions", "NSSA + PAYE + other", payslip.getTotalDeductions(), true);

        writer.netPay(payslip.getCurrency().name(), payslip.getNetSalary());

        writer.sectionTitle("STATUTORY DETAIL");
        writer.keyMoney("Taxable income", payslip.getTaxableIncome());
        writer.keyMoney("Income tax before credits", payslip.getIncomeTaxBeforeCredits());
        writer.keyMoney("Tax credits applied", payslip.getTaxCreditsApplied());
        writer.keyMoney("AIDS levy", payslip.getAidsLevy());
        writer.keyMoney("Employer NSSA contribution", payslip.getEmployerNssaContribution());

        writer.note("This payslip is a system-generated record of the processed payroll snapshot.");
    }

    private String filenameTitle(Payslip payslip) {
        return "Payslip " + payslip.getEmployee().getEmployeeNumber() + " " + payPeriod(payslip.getPayrollRun());
    }

    private String payPeriod(PayrollRun run) {
        String month = Month.of(run.getMonth()).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        return month + " " + run.getYear();
    }

    private String lineMeta(PayslipLineItem item) {
        PayItemSource source = item.getSource() == null ? PayItemSource.ONE_OFF : item.getSource();
        String label = source == PayItemSource.RECURRING ? "Recurring" : "One-off";
        return item.isTaxable() ? label + " - Taxable" : label;
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private final class DocumentWriter implements AutoCloseable {
        private static final float MARGIN = 48f;
        private static final float BOTTOM = 45f;
        private final PDDocument document;
        private final PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        private final PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        private PDPageContentStream content;
        private float y;
        private int pageNumber;
        private boolean finished;

        private DocumentWriter(PDDocument document) throws IOException {
            this.document = document;
            openPage();
        }

        private void openPage() throws IOException {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            content = new PDPageContentStream(document, page);
            pageNumber++;

            content.setNonStrokingColor(NAVY);
            content.addRect(0, PDRectangle.A4.getHeight() - 92, PDRectangle.A4.getWidth(), 92);
            content.fill();
            drawText("PAYROLL LITE", MARGIN, PDRectangle.A4.getHeight() - 48, bold, 18, Color.WHITE);
            drawText("EMPLOYEE PAYSLIP", MARGIN, PDRectangle.A4.getHeight() - 68, regular, 9, new Color(195, 205, 220));
            drawRightText("CONFIDENTIAL", PDRectangle.A4.getWidth() - MARGIN,
                    PDRectangle.A4.getHeight() - 56, bold, 8, new Color(195, 205, 220));
            y = PDRectangle.A4.getHeight() - 120;
        }

        private void ensureSpace(float required) throws IOException {
            if (y - required < BOTTOM) {
                closePage();
                openPage();
            }
        }

        private void sectionTitle(String title) throws IOException {
            ensureSpace(34);
            content.setNonStrokingColor(LIGHT);
            content.addRect(MARGIN, y - 21, PDRectangle.A4.getWidth() - (MARGIN * 2), 21);
            content.fill();
            drawText(title, MARGIN + 8, y - 14, bold, 8, BLUE);
            y -= 31;
        }

        private void keyValue(String label, String value) throws IOException {
            ensureSpace(20);
            drawText(label, MARGIN + 5, y - 12, regular, 9, MUTED);
            drawText(value, 190, y - 12, bold, 9, TEXT);
            drawRule(y - 18);
            y -= 20;
        }

        private void keyMoney(String label, BigDecimal value) throws IOException {
            ensureSpace(20);
            drawText(label, MARGIN + 5, y - 12, regular, 9, MUTED);
            drawRightText(money(value), PDRectangle.A4.getWidth() - MARGIN - 5, y - 12, bold, 9, TEXT);
            drawRule(y - 18);
            y -= 20;
        }

        private void tableHeader(String first, String second, String third) throws IOException {
            ensureSpace(22);
            drawText(first, MARGIN + 5, y - 11, bold, 7, MUTED);
            drawText(second, 300, y - 11, bold, 7, MUTED);
            drawRightText(third, PDRectangle.A4.getWidth() - MARGIN - 5, y - 11, bold, 7, MUTED);
            drawRule(y - 17);
            y -= 20;
        }

        private void moneyRow(
                String description,
                String meta,
                BigDecimal amount,
                boolean emphasized
        ) throws IOException {
            ensureSpace(23);
            PDType1Font font = emphasized ? bold : regular;
            Color color = emphasized ? NAVY : TEXT;
            drawText(fit(description, font, 9, 230), MARGIN + 5, y - 13, font, 9, color);
            drawText(fit(meta, regular, 8, 145), 300, y - 13, regular, 8, MUTED);
            drawRightText(money(amount), PDRectangle.A4.getWidth() - MARGIN - 5, y - 13, font, 9, color);
            drawRule(y - 20);
            y -= 23;
        }

        private void netPay(String currency, BigDecimal value) throws IOException {
            ensureSpace(62);
            y -= 8;
            content.setNonStrokingColor(NAVY);
            content.addRect(MARGIN, y - 45, PDRectangle.A4.getWidth() - (MARGIN * 2), 45);
            content.fill();
            drawText("NET PAY", MARGIN + 14, y - 27, bold, 10, Color.WHITE);
            drawRightText(currency + " " + money(value), PDRectangle.A4.getWidth() - MARGIN - 14,
                    y - 29, bold, 17, Color.WHITE);
            y -= 60;
        }

        private void note(String text) throws IOException {
            ensureSpace(32);
            y -= 6;
            drawText(fit(text, regular, 8, PDRectangle.A4.getWidth() - (MARGIN * 2)),
                    MARGIN, y - 10, regular, 8, MUTED);
            y -= 24;
        }

        private void drawRule(float ruleY) throws IOException {
            content.setStrokingColor(LINE);
            content.setLineWidth(0.5f);
            content.moveTo(MARGIN, ruleY);
            content.lineTo(PDRectangle.A4.getWidth() - MARGIN, ruleY);
            content.stroke();
        }

        private void drawText(
                String text,
                float x,
                float textY,
                PDType1Font font,
                float size,
                Color color
        ) throws IOException {
            content.beginText();
            content.setFont(font, size);
            content.setNonStrokingColor(color);
            content.newLineAtOffset(x, textY);
            content.showText(safe(text));
            content.endText();
        }

        private void drawRightText(
                String text,
                float right,
                float textY,
                PDType1Font font,
                float size,
                Color color
        ) throws IOException {
            String safe = safe(text);
            float width = font.getStringWidth(safe) / 1000f * size;
            drawText(safe, right - width, textY, font, size, color);
        }

        private String fit(String text, PDType1Font font, float size, float maxWidth) throws IOException {
            String value = safe(text);
            if (font.getStringWidth(value) / 1000f * size <= maxWidth) return value;
            String suffix = "...";
            while (!value.isEmpty()
                    && font.getStringWidth(value + suffix) / 1000f * size > maxWidth) {
                value = value.substring(0, value.length() - 1);
            }
            return value + suffix;
        }

        private String safe(String value) {
            if (value == null) return "-";
            return value.replaceAll("[^\\x20-\\x7E]", "?");
        }

        private String money(BigDecimal amount) {
            return String.format(Locale.ROOT, "%,.2f", zeroIfNull(amount));
        }

        private void closePage() throws IOException {
            if (content == null) return;
            content.setStrokingColor(LINE);
            content.moveTo(MARGIN, 31);
            content.lineTo(PDRectangle.A4.getWidth() - MARGIN, 31);
            content.stroke();
            drawText("Generated by Payroll Lite", MARGIN, 18, regular, 7, MUTED);
            drawRightText("Page " + pageNumber, PDRectangle.A4.getWidth() - MARGIN, 18, regular, 7, MUTED);
            content.close();
            content = null;
        }

        private void finish() throws IOException {
            if (!finished) {
                closePage();
                finished = true;
            }
        }

        @Override
        public void close() throws IOException {
            finish();
        }
    }

}
