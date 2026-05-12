package pl.cafteo.jdgflow.invoice.domain;

import java.util.List;
import java.util.Optional;

public interface FakturowniaClient {

    List<ExternalInvoice> fetchInvoices(int page, int perPage);

    Optional<byte[]> fetchInvoicePdf(long fakturowniaId);
}
