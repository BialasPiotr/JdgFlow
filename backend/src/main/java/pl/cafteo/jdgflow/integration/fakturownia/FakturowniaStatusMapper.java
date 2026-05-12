package pl.cafteo.jdgflow.integration.fakturownia;

import org.springframework.stereotype.Component;
import pl.cafteo.jdgflow.invoice.domain.InvoiceStatus;

import java.util.Map;

@Component
public class FakturowniaStatusMapper {

    private static final Map<String, InvoiceStatus> MAPPING = Map.of(
            "paid",     InvoiceStatus.PAID,
            "partial",  InvoiceStatus.PARTIAL,
            "sent",     InvoiceStatus.SENT,
            "rejected", InvoiceStatus.CANCELLED,
            "declined", InvoiceStatus.CANCELLED,
            "overdue",  InvoiceStatus.OVERDUE
    );

    public InvoiceStatus toDomain(String raw) {
        if (raw == null || raw.isBlank()) return InvoiceStatus.DRAFT;
        return MAPPING.getOrDefault(raw.toLowerCase(), InvoiceStatus.DRAFT);
    }
}
