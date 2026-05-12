package pl.cafteo.jdgflow.invoice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.cafteo.jdgflow.integration.fakturownia.FakturowniaProperties;
import pl.cafteo.jdgflow.invoice.domain.ExternalInvoice;
import pl.cafteo.jdgflow.invoice.domain.FakturowniaClient;
import pl.cafteo.jdgflow.invoice.service.InvoiceUpserter.UpsertOutcome;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceSyncService {

    private final FakturowniaClient fakturowniaClient;
    private final InvoiceUpserter invoiceUpserter;
    private final FakturowniaProperties properties;

    public SyncResult syncAll(UUID userId) {
        int pageSize = properties.defaultPageSize();
        int created = 0;
        int updated = 0;
        int page = 1;

        while (true) {
            List<ExternalInvoice> batch = fakturowniaClient.fetchInvoices(page, pageSize);
            if (batch.isEmpty()) break;

            for (ExternalInvoice ext : batch) {
                UpsertOutcome outcome = invoiceUpserter.upsert(userId, ext);
                if (outcome == UpsertOutcome.CREATED) created++;
                else updated++;
            }

            log.info("Synced page {} ({} invoices) for user {}", page, batch.size(), userId);
            if (batch.size() < pageSize) break;
            page++;
        }

        log.info("Fakturownia sync finished: created={}, updated={}", created, updated);
        return new SyncResult(created, updated);
    }

    public record SyncResult(int created, int updated) {
        public int total() { return created + updated; }
    }
}
