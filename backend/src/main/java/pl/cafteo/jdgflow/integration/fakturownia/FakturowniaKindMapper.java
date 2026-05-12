package pl.cafteo.jdgflow.integration.fakturownia;

import org.springframework.stereotype.Component;
import pl.cafteo.jdgflow.invoice.domain.InvoiceKind;

import java.util.Map;

@Component
public class FakturowniaKindMapper {

    private static final Map<String, InvoiceKind> MAPPING = Map.of(
            "vat",        InvoiceKind.VAT,
            "proforma",   InvoiceKind.PROFORMA,
            "correction", InvoiceKind.CORRECTION,
            "receipt",    InvoiceKind.RECEIPT,
            "advance",    InvoiceKind.ADVANCE,
            "final",      InvoiceKind.FINAL
    );

    public InvoiceKind toDomain(String raw) {
        if (raw == null || raw.isBlank()) return InvoiceKind.OTHER;
        return MAPPING.getOrDefault(raw.toLowerCase(), InvoiceKind.OTHER);
    }
}
