package pl.cafteo.jdgflow.invoice.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID>, JpaSpecificationExecutor<Invoice> {

    Optional<Invoice> findByUserIdAndFakturowniaId(UUID userId, Long fakturowniaId);

    Page<Invoice> findByUserId(UUID userId, Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(i.netAmount), 0)  AS net,
                   COALESCE(SUM(i.vatAmount), 0)  AS vat,
                   COALESCE(SUM(i.grossAmount), 0) AS gross
            FROM Invoice i
            WHERE i.userId = :userId
              AND i.kind <> pl.cafteo.jdgflow.invoice.domain.InvoiceKind.PROFORMA
              AND i.status <> pl.cafteo.jdgflow.invoice.domain.InvoiceStatus.CANCELLED
              AND i.issueDate BETWEEN :from AND :to
            """)
    RevenueRow sumByIssueDateBetween(UUID userId, LocalDate from, LocalDate to);

    @Query("""
            SELECT COALESCE(SUM(i.netAmount), 0)  AS net,
                   COALESCE(SUM(i.vatAmount), 0)  AS vat,
                   COALESCE(SUM(i.grossAmount), 0) AS gross
            FROM Invoice i
            WHERE i.userId = :userId
              AND i.kind <> pl.cafteo.jdgflow.invoice.domain.InvoiceKind.PROFORMA
              AND i.status <> pl.cafteo.jdgflow.invoice.domain.InvoiceStatus.CANCELLED
              AND i.paidDate IS NOT NULL
              AND i.paidDate BETWEEN :from AND :to
            """)
    RevenueRow sumByPaidDateBetween(UUID userId, LocalDate from, LocalDate to);

    interface RevenueRow {
        BigDecimal getNet();
        BigDecimal getVat();
        BigDecimal getGross();
    }
}
