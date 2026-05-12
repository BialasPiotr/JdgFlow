package pl.cafteo.jdgflow.notification.service;

import org.springframework.stereotype.Component;
import pl.cafteo.jdgflow.tax.domain.ObligationType;
import pl.cafteo.jdgflow.tax.domain.TaxObligation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class ObligationEmailTemplate {

    private static final DateTimeFormatter PL_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public Rendered renderReminder(TaxObligation obligation, int daysUntilDeadline) {
        String label = obligationLabel(obligation.getType());
        String deadlineStr = obligation.getDeadline().format(PL_DATE);
        String amountStr = formatMoney(obligation.getAmount());

        String subject = "JDGFlow • " + label + " — termin " + deadlineStr
                + " (za " + daysUntilDeadline + (daysUntilDeadline == 1 ? " dzień" : " dni") + ")";

        String body = """
                <!DOCTYPE html>
                <html lang="pl">
                <body style="margin:0;padding:0;background:#f8fafc;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Helvetica,Arial,sans-serif;color:#334155;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f8fafc;padding:32px 16px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="560" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:16px;box-shadow:0 1px 3px rgba(15,23,42,0.06);overflow:hidden;">
                          <tr>
                            <td style="padding:28px 32px 8px 32px;">
                              <p style="margin:0;font-size:11px;font-weight:700;color:#7c3aed;letter-spacing:0.08em;text-transform:uppercase;">JDGFlow • Przypomnienie</p>
                              <h1 style="margin:6px 0 4px 0;font-size:22px;color:#0f172a;font-weight:700;">%s</h1>
                              <p style="margin:0;font-size:14px;color:#64748b;">Zbliża się termin płatności — zaplanuj przelew żeby uniknąć odsetek.</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:20px 32px 0 32px;">
                              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="border-collapse:collapse;">
                                <tr>
                                  <td style="padding:14px 0;border-top:1px solid #e2e8f0;font-size:13px;color:#64748b;">Termin</td>
                                  <td style="padding:14px 0;border-top:1px solid #e2e8f0;font-size:14px;color:#0f172a;text-align:right;font-weight:600;">%s</td>
                                </tr>
                                <tr>
                                  <td style="padding:14px 0;border-top:1px solid #e2e8f0;font-size:13px;color:#64748b;">Pozostało</td>
                                  <td style="padding:14px 0;border-top:1px solid #e2e8f0;font-size:14px;color:#0f172a;text-align:right;font-weight:600;">%s</td>
                                </tr>
                                <tr>
                                  <td style="padding:14px 0;border-top:1px solid #e2e8f0;font-size:13px;color:#64748b;">Kwota</td>
                                  <td style="padding:14px 0;border-top:1px solid #e2e8f0;font-size:18px;color:#7c3aed;text-align:right;font-weight:700;">%s</td>
                                </tr>
                              </table>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:24px 32px 28px 32px;">
                              <p style="margin:0;font-size:12px;color:#94a3b8;">
                                Po opłaceniu zaznacz w aplikacji jako zapłacone (zakładka „Podatki").
                              </p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:16px 32px;background:#f8fafc;border-top:1px solid #e2e8f0;">
                              <p style="margin:0;font-size:11px;color:#94a3b8;text-align:center;">
                                Wiadomość wygenerowana automatycznie przez JDGFlow • %s
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                escape(label),
                escape(deadlineStr),
                escape(daysUntilDeadline + (daysUntilDeadline == 1 ? " dzień" : " dni")),
                escape(amountStr),
                escape(LocalDate.now().format(PL_DATE))
        );

        return new Rendered(subject, body);
    }

    public Rendered renderTest(String recipient) {
        String subject = "JDGFlow • Test wiadomości";
        String body = """
                <!DOCTYPE html>
                <html lang="pl">
                <body style="margin:0;padding:0;background:#f8fafc;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Helvetica,Arial,sans-serif;color:#334155;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f8fafc;padding:32px 16px;">
                    <tr><td align="center">
                      <table role="presentation" width="480" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:16px;padding:28px 32px;">
                        <tr><td>
                          <p style="margin:0;font-size:11px;font-weight:700;color:#7c3aed;letter-spacing:0.08em;text-transform:uppercase;">JDGFlow</p>
                          <h1 style="margin:6px 0 8px 0;font-size:20px;color:#0f172a;font-weight:700;">Konfiguracja SendGrid działa ✓</h1>
                          <p style="margin:0;font-size:14px;color:#64748b;line-height:1.5;">
                            Jeśli to czytasz, integracja jest poprawna. Aplikacja jest gotowa wysyłać przypomnienia o terminach ZUS, PIT i VAT na ten adres: <strong>%s</strong>.
                          </p>
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(escape(recipient));
        return new Rendered(subject, body);
    }

    private static String obligationLabel(ObligationType type) {
        return switch (type) {
            case ZUS_SOCIAL  -> "Składka ZUS społeczny";
            case ZUS_HEALTH  -> "Składka zdrowotna";
            case PIT_ADVANCE -> "Zaliczka PIT";
            case VAT         -> "VAT (JPK_V7M)";
        };
    }

    private static String formatMoney(BigDecimal amount) {
        if (amount == null) return "—";
        java.text.NumberFormat fmt = java.text.NumberFormat.getNumberInstance(Locale.of("pl", "PL"));
        fmt.setMinimumFractionDigits(2);
        fmt.setMaximumFractionDigits(2);
        return fmt.format(amount) + " zł";
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    public record Rendered(String subject, String htmlBody) {}
}
