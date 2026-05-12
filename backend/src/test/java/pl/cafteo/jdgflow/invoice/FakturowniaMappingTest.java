package pl.cafteo.jdgflow.invoice;

import org.junit.jupiter.api.Test;
import pl.cafteo.jdgflow.integration.fakturownia.FakturowniaInvoiceDto;
import pl.cafteo.jdgflow.integration.fakturownia.FakturowniaInvoiceMapper;
import pl.cafteo.jdgflow.integration.fakturownia.FakturowniaKindMapper;
import pl.cafteo.jdgflow.integration.fakturownia.FakturowniaStatusMapper;
import pl.cafteo.jdgflow.invoice.domain.ExternalInvoice;
import pl.cafteo.jdgflow.invoice.domain.InvoiceKind;
import pl.cafteo.jdgflow.invoice.domain.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class FakturowniaMappingTest {

    private final FakturowniaStatusMapper statusMapper = new FakturowniaStatusMapper();
    private final FakturowniaKindMapper kindMapper = new FakturowniaKindMapper();
    private final FakturowniaInvoiceMapper invoiceMapper = new FakturowniaInvoiceMapper(statusMapper, kindMapper);

    @Test
    void maps_paid_vat_invoice_with_polish_decimal_separator() {
        FakturowniaInvoiceDto dto = new FakturowniaInvoiceDto(
                12345L, "FV/2026/04/01", "vat", "paid",
                "1000,00", "1230,00", "1230,00",
                "PLN", null,
                "2026-04-15", "2026-04-15", "2026-04-29", "2026-04-22",
                "Acme Sp. z o.o.", "5252111111", "billing@acme.test"
        );

        ExternalInvoice ext = invoiceMapper.toExternal(dto);

        assertThat(ext.id()).isEqualTo(12345L);
        assertThat(ext.kind()).isEqualTo(InvoiceKind.VAT);
        assertThat(ext.status()).isEqualTo(InvoiceStatus.PAID);
        assertThat(ext.netAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(ext.grossAmount()).isEqualByComparingTo(new BigDecimal("1230.00"));
        assertThat(ext.vatAmount()).isEqualByComparingTo(new BigDecimal("230.00"));
        assertThat(ext.currency()).isEqualTo("PLN");
        assertThat(ext.issueDate()).isEqualTo(LocalDate.of(2026, 4, 15));
        assertThat(ext.paidDate()).isEqualTo(LocalDate.of(2026, 4, 22));
        assertThat(ext.buyerName()).isEqualTo("Acme Sp. z o.o.");
        assertThat(ext.buyerNip()).isEqualTo("5252111111");
    }

    @Test
    void uses_issue_date_when_sell_date_missing() {
        FakturowniaInvoiceDto dto = new FakturowniaInvoiceDto(
                1L, "X", "vat", "sent", "100", "123", "123", "PLN",
                null, "2026-01-10", null, null, null, null, null, null);

        ExternalInvoice ext = invoiceMapper.toExternal(dto);

        assertThat(ext.saleDate()).isEqualTo(LocalDate.of(2026, 1, 10));
    }

    @Test
    void defaults_currency_to_PLN_when_blank() {
        FakturowniaInvoiceDto dto = new FakturowniaInvoiceDto(
                1L, "X", "vat", "sent", "100", "100", "100", null,
                null, "2026-01-10", "2026-01-10", null, null, null, null, null);

        ExternalInvoice ext = invoiceMapper.toExternal(dto);

        assertThat(ext.currency()).isEqualTo("PLN");
    }

    @Test
    void status_mapper_falls_back_to_DRAFT_for_unknown_or_null() {
        assertThat(statusMapper.toDomain("issued")).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(statusMapper.toDomain(null)).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(statusMapper.toDomain("")).isEqualTo(InvoiceStatus.DRAFT);
    }

    @Test
    void status_mapper_recognizes_known_statuses() {
        assertThat(statusMapper.toDomain("paid")).isEqualTo(InvoiceStatus.PAID);
        assertThat(statusMapper.toDomain("PAID")).isEqualTo(InvoiceStatus.PAID);
        assertThat(statusMapper.toDomain("partial")).isEqualTo(InvoiceStatus.PARTIAL);
        assertThat(statusMapper.toDomain("rejected")).isEqualTo(InvoiceStatus.CANCELLED);
        assertThat(statusMapper.toDomain("declined")).isEqualTo(InvoiceStatus.CANCELLED);
    }

    @Test
    void kind_mapper_falls_back_to_OTHER_for_unknown() {
        assertThat(kindMapper.toDomain("strange-kind")).isEqualTo(InvoiceKind.OTHER);
        assertThat(kindMapper.toDomain(null)).isEqualTo(InvoiceKind.OTHER);
        assertThat(kindMapper.toDomain("vat")).isEqualTo(InvoiceKind.VAT);
        assertThat(kindMapper.toDomain("CORRECTION")).isEqualTo(InvoiceKind.CORRECTION);
    }
}
