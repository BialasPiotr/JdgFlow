package pl.cafteo.jdgflow.invoice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.cafteo.jdgflow.invoice.domain.ExternalInvoice;
import pl.cafteo.jdgflow.invoice.domain.Invoice;
import pl.cafteo.jdgflow.invoice.domain.InvoiceRepository;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InvoiceUpserter {

    private final InvoiceRepository invoiceRepository;

    @Transactional
    public UpsertOutcome upsert(UUID userId, ExternalInvoice external) {
        return invoiceRepository.findByUserIdAndFakturowniaId(userId, external.id())
                .map(existing -> {
                    existing.applyExternal(external);
                    return UpsertOutcome.UPDATED;
                })
                .orElseGet(() -> {
                    invoiceRepository.save(Invoice.fromExternal(userId, external));
                    return UpsertOutcome.CREATED;
                });
    }

    public enum UpsertOutcome { CREATED, UPDATED }
}
