package pl.cafteo.jdgflow.tax.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.cafteo.jdgflow.invoice.domain.InvoiceRepository;
import pl.cafteo.jdgflow.tax.domain.AccountingMethod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RevenueAggregator {

    private final InvoiceRepository invoiceRepository;

    public Revenue between(UUID userId, AccountingMethod method, LocalDate from, LocalDate to) {
        InvoiceRepository.RevenueRow row = method == AccountingMethod.CASH
                ? invoiceRepository.sumByPaidDateBetween(userId, from, to)
                : invoiceRepository.sumByIssueDateBetween(userId, from, to);

        return new Revenue(
                nz(row.getNet()),
                nz(row.getVat()),
                nz(row.getGross()));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    public record Revenue(BigDecimal net, BigDecimal vat, BigDecimal gross) {}
}
