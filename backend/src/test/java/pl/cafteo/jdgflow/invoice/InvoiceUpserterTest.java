package pl.cafteo.jdgflow.invoice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.cafteo.jdgflow.invoice.domain.ExternalInvoice;
import pl.cafteo.jdgflow.invoice.domain.Invoice;
import pl.cafteo.jdgflow.invoice.domain.InvoiceKind;
import pl.cafteo.jdgflow.invoice.domain.InvoiceRepository;
import pl.cafteo.jdgflow.invoice.domain.InvoiceStatus;
import pl.cafteo.jdgflow.invoice.service.InvoiceUpserter;
import pl.cafteo.jdgflow.invoice.service.InvoiceUpserter.UpsertOutcome;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link InvoiceUpserter}. Idempotency contract: same {@code fakturownia_id}
 * never inserts a duplicate, only mutates the existing entity in place.
 */
@ExtendWith(MockitoExtension.class)
class InvoiceUpserterTest {

    @Mock InvoiceRepository invoiceRepository;

    @InjectMocks InvoiceUpserter upserter;

    private final UUID userId = UUID.randomUUID();

    @Test
    @DisplayName("brak istniejącego rekordu → CREATED, save wywołane raz")
    void missing_invoice_is_created() {
        ExternalInvoice ext = sampleExternal(99L, InvoiceStatus.SENT);
        when(invoiceRepository.findByUserIdAndFakturowniaId(userId, 99L)).thenReturn(Optional.empty());

        UpsertOutcome outcome = upserter.upsert(userId, ext);

        assertThat(outcome).isEqualTo(UpsertOutcome.CREATED);
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("istniejący rekord → UPDATED, save NIE jest wywołane (mutacja in-place pod tx)")
    void existing_invoice_is_updated_in_place() {
        Invoice existing = Invoice.fromExternal(userId, sampleExternal(99L, InvoiceStatus.SENT));
        when(invoiceRepository.findByUserIdAndFakturowniaId(userId, 99L)).thenReturn(Optional.of(existing));

        UpsertOutcome outcome = upserter.upsert(userId, sampleExternal(99L, InvoiceStatus.PAID));

        assertThat(outcome).isEqualTo(UpsertOutcome.UPDATED);
        assertThat(existing.getStatus()).isEqualTo(InvoiceStatus.PAID);
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    private static ExternalInvoice sampleExternal(long id, InvoiceStatus status) {
        return new ExternalInvoice(
                id, "FV/2026/" + id, InvoiceKind.VAT, status,
                new BigDecimal("1000.00"), new BigDecimal("230.00"), new BigDecimal("1230.00"),
                "PLN", null,
                LocalDate.of(2026, 4, 15), LocalDate.of(2026, 4, 15),
                LocalDate.of(2026, 4, 29),
                status == InvoiceStatus.PAID ? LocalDate.of(2026, 4, 22) : null,
                "Acme", "5252111111", "billing@acme.test");
    }
}
