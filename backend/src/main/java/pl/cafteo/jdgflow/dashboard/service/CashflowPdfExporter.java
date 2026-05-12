package pl.cafteo.jdgflow.dashboard.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pl.cafteo.jdgflow.auth.domain.User;
import pl.cafteo.jdgflow.dashboard.api.dto.CashflowMonthEntry;
import pl.cafteo.jdgflow.dashboard.api.dto.CashflowResponse;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Component
public class CashflowPdfExporter {

    private static final String[] MONTH_NAMES_PL = {
            "Styczeń", "Luty", "Marzec", "Kwiecień", "Maj", "Czerwiec",
            "Lipiec", "Sierpień", "Wrzesień", "Październik", "Listopad", "Grudzień"
    };

    private static final Color BRAND_VIOLET = new Color(124, 58, 237);   // brand-600
    private static final Color SLATE_700    = new Color(51, 65, 85);
    private static final Color SLATE_500    = new Color(100, 116, 139);
    private static final Color SLATE_100    = new Color(241, 245, 249);
    private static final Color EMERALD_700  = new Color(4, 120, 87);
    private static final Color ROSE_700     = new Color(190, 18, 60);

    private static final NumberFormat MONEY = NumberFormat.getNumberInstance(Locale.of("pl", "PL"));
    static {
        MONEY.setMinimumFractionDigits(2);
        MONEY.setMaximumFractionDigits(2);
    }

    public byte[] toPdf(User user, CashflowResponse data) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 42, 42, 56, 48);
            PdfWriter.getInstance(document, out);

            BaseFont base = BaseFont.createFont(BaseFont.HELVETICA, "Cp1250", BaseFont.NOT_EMBEDDED);
            BaseFont baseBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, "Cp1250", BaseFont.NOT_EMBEDDED);

            Font fontTitle    = new Font(baseBold, 20, Font.NORMAL, BRAND_VIOLET);
            Font fontEyebrow  = new Font(baseBold, 9,  Font.NORMAL, BRAND_VIOLET);
            Font fontSubtitle = new Font(base,     11, Font.NORMAL, SLATE_500);
            Font fontH2       = new Font(baseBold, 13, Font.NORMAL, SLATE_700);
            Font fontBody     = new Font(base,     10, Font.NORMAL, SLATE_700);
            Font fontBodyBold = new Font(baseBold, 10, Font.NORMAL, SLATE_700);
            Font fontMuted    = new Font(base,     9,  Font.NORMAL, SLATE_500);
            Font fontFooter   = new Font(base,     8,  Font.NORMAL, SLATE_500);

            document.open();

            Paragraph eyebrow = new Paragraph("JDGFLOW • RAPORT CASHFLOW", fontEyebrow);
            eyebrow.setSpacingAfter(2);
            document.add(eyebrow);

            Paragraph title = new Paragraph("Cashflow " + data.year(), fontTitle);
            title.setSpacingAfter(4);
            document.add(title);

            String subtitleText = buildSubtitle(user);
            if (!subtitleText.isEmpty()) {
                Paragraph sub = new Paragraph(subtitleText, fontSubtitle);
                sub.setSpacingAfter(18);
                document.add(sub);
            }

            document.add(sectionHeader("Przepływ miesięczny", fontH2));
            document.add(buildMonthlyTable(data, fontBody, fontBodyBold));

            if (data.previousYear() != null) {
                Paragraph spacer = new Paragraph(" ");
                spacer.setSpacingAfter(14);
                document.add(spacer);
                document.add(sectionHeader("Porównanie rok do roku", fontH2));
                document.add(buildYoyTable(data, fontBody, fontBodyBold, fontMuted));
            }

            Paragraph footer = new Paragraph(
                    "Wygenerowano " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                            + " z JDGFlow",
                    fontFooter);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(28);
            document.add(footer);

            document.close();
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Nie udało się wygenerować PDF: " + e.getMessage(), e);
        }
    }

    private static Paragraph sectionHeader(String text, Font font) {
        Paragraph p = new Paragraph(text, font);
        p.setSpacingAfter(8);
        return p;
    }

    private static String buildSubtitle(User user) {
        StringBuilder sb = new StringBuilder();
        if (user.getBusinessName() != null && !user.getBusinessName().isBlank()) {
            sb.append(user.getBusinessName());
        } else if (user.getFullName() != null && !user.getFullName().isBlank()) {
            sb.append(user.getFullName());
        }
        if (user.getNip() != null && !user.getNip().isBlank()) {
            if (sb.length() > 0) sb.append("  •  ");
            sb.append("NIP ").append(user.getNip());
        }
        if (sb.length() > 0) sb.append("  •  ");
        sb.append(user.getEmail());
        return sb.toString();
    }

    private static PdfPTable buildMonthlyTable(CashflowResponse data, Font body, Font bold) throws IOException {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3.5f, 2.5f, 2.5f, 2.5f});

        table.addCell(headerCell("Miesiąc", bold));
        table.addCell(headerCell("Przychód", bold));
        table.addCell(headerCell("Koszty", bold));
        table.addCell(headerCell("Dochód", bold));

        for (CashflowMonthEntry m : data.months()) {
            table.addCell(textCell(MONTH_NAMES_PL[m.month() - 1], body, Element.ALIGN_LEFT));
            table.addCell(moneyCell(m.revenue(), body, EMERALD_700));
            table.addCell(moneyCell(m.costs(), body, ROSE_700));
            table.addCell(moneyCell(m.income(), body, BRAND_VIOLET));
        }

        table.addCell(highlightCell("Suma " + data.year(), bold, Element.ALIGN_LEFT));
        table.addCell(highlightMoneyCell(data.total().revenue(), bold, EMERALD_700));
        table.addCell(highlightMoneyCell(data.total().costs(), bold, ROSE_700));
        table.addCell(highlightMoneyCell(data.total().income(), bold, BRAND_VIOLET));

        return table;
    }

    private static PdfPTable buildYoyTable(CashflowResponse current, Font body, Font bold, Font muted) throws IOException {
        CashflowResponse prev = current.previousYear();
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2.5f, 2.5f, 2.5f, 2.5f});

        table.addCell(headerCell("", bold));
        table.addCell(headerCell(String.valueOf(prev.year()), bold));
        table.addCell(headerCell(String.valueOf(current.year()), bold));
        table.addCell(headerCell("Δ YoY", bold));

        addYoyRow(table, "Przychód",
                prev.total().revenue(), current.total().revenue(),
                body, muted, EMERALD_700);
        addYoyRow(table, "Koszty",
                prev.total().costs(), current.total().costs(),
                body, muted, ROSE_700);
        addYoyRow(table, "Dochód",
                prev.total().income(), current.total().income(),
                body, muted, BRAND_VIOLET);

        return table;
    }

    private static void addYoyRow(PdfPTable table, String label,
                                  BigDecimal prev, BigDecimal cur,
                                  Font body, Font muted, Color colorAccent) {
        table.addCell(textCell(label, body, Element.ALIGN_LEFT));
        table.addCell(moneyCell(prev, muted, SLATE_500));
        table.addCell(moneyCell(cur, body, colorAccent));

        BigDecimal diff = cur.subtract(prev);
        String diffText;
        Color diffColor;
        if (prev.signum() == 0) {
            diffText = "—";
            diffColor = SLATE_500;
        } else {
            BigDecimal pct = diff.multiply(BigDecimal.valueOf(100))
                    .divide(prev.abs(), 1, RoundingMode.HALF_UP);
            diffText = (pct.signum() >= 0 ? "+" : "") + pct.toPlainString() + "%";
            diffColor = pct.signum() >= 0 ? EMERALD_700 : ROSE_700;
        }

        Font diffFont = new Font(body.getBaseFont(), 10, Font.BOLD, diffColor);
        PdfPCell cell = new PdfPCell(new Phrase(diffText, diffFont));
        styleDataCell(cell, Element.ALIGN_RIGHT);
        table.addCell(cell);
    }

    private static PdfPCell headerCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(SLATE_100);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(SLATE_500);
        cell.setBorderWidth(0.6f);
        cell.setPadding(7);
        cell.setHorizontalAlignment("Miesiąc".equals(text) || text.isEmpty() ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
        return cell;
    }

    private static PdfPCell textCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        styleDataCell(cell, alignment);
        return cell;
    }

    private static PdfPCell moneyCell(BigDecimal value, Font font, Color color) {
        Font colored = new Font(font.getBaseFont(), font.getSize(), font.getStyle(), color);
        PdfPCell cell = new PdfPCell(new Phrase(formatMoney(value), colored));
        styleDataCell(cell, Element.ALIGN_RIGHT);
        return cell;
    }

    private static PdfPCell highlightCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(SLATE_100);
        cell.setBorder(Rectangle.TOP);
        cell.setBorderColor(SLATE_500);
        cell.setBorderWidth(0.6f);
        cell.setPadding(8);
        cell.setHorizontalAlignment(alignment);
        return cell;
    }

    private static PdfPCell highlightMoneyCell(BigDecimal value, Font font, Color color) {
        Font colored = new Font(font.getBaseFont(), font.getSize(), font.getStyle(), color);
        PdfPCell cell = new PdfPCell(new Phrase(formatMoney(value), colored));
        cell.setBackgroundColor(SLATE_100);
        cell.setBorder(Rectangle.TOP);
        cell.setBorderColor(SLATE_500);
        cell.setBorderWidth(0.6f);
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }

    private static void styleDataCell(PdfPCell cell, int alignment) {
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(SLATE_100);
        cell.setBorderWidth(0.5f);
        cell.setPadding(6);
        cell.setHorizontalAlignment(alignment);
    }

    private static String formatMoney(BigDecimal value) {
        if (value == null) return "—";
        return MONEY.format(value) + " zł";
    }
}
