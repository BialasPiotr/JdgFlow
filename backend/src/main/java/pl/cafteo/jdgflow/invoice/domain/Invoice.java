package pl.cafteo.jdgflow.invoice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.cafteo.jdgflow.common.audit.AuditableEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
@Table(name = "invoices")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Invoice extends AuditableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "fakturownia_id", nullable = false)
    private Long fakturowniaId;

    @Column(name = "invoice_number", nullable = false, length = 64)
    private String invoiceNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 32)
    private InvoiceKind kind;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private InvoiceStatus status;

    @Column(name = "net_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "vat_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal vatAmount;

    @Column(name = "gross_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "exchange_rate", precision = 12, scale = 6)
    private BigDecimal exchangeRate;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "sale_date", nullable = false)
    private LocalDate saleDate;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Column(name = "buyer_name")
    private String buyerName;

    @Column(name = "buyer_nip", length = 20)
    private String buyerNip;

    @Column(name = "buyer_email")
    private String buyerEmail;

    @Column(name = "pdf_s3_key", length = 512)
    private String pdfS3Key;

    public static Invoice fromExternal(UUID userId, ExternalInvoice ext) {
        Invoice i = new Invoice();
        i.id = UUID.randomUUID();
        i.userId = userId;
        i.applyExternal(ext);
        return i;
    }

    public void applyExternal(ExternalInvoice ext) {
        this.fakturowniaId = ext.id();
        this.invoiceNumber = ext.number();
        this.kind = ext.kind();
        this.status = ext.status();
        this.netAmount = ext.netAmount();
        this.vatAmount = ext.vatAmount();
        this.grossAmount = ext.grossAmount();
        this.currency = ext.currency();
        this.exchangeRate = ext.exchangeRate();
        this.issueDate = ext.issueDate();
        this.saleDate = ext.saleDate();
        this.paymentDate = ext.paymentDate();
        this.paidDate = ext.paidDate();
        this.buyerName = ext.buyerName();
        this.buyerNip = ext.buyerNip();
        this.buyerEmail = ext.buyerEmail();
    }

    public void attachPdfKey(String s3Key) {
        this.pdfS3Key = s3Key;
    }
}
