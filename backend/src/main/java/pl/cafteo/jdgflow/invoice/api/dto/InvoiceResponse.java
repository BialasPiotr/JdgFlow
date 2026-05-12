package pl.cafteo.jdgflow.invoice.api.dto;

import pl.cafteo.jdgflow.invoice.domain.Invoice;
import pl.cafteo.jdgflow.invoice.domain.InvoiceKind;
import pl.cafteo.jdgflow.invoice.domain.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        String number,
        InvoiceKind kind,
        InvoiceStatus status,
        BigDecimal netAmount,
        BigDecimal vatAmount,
        BigDecimal grossAmount,
        String currency,
        LocalDate issueDate,
        LocalDate saleDate,
        LocalDate paymentDate,
        LocalDate paidDate,
        String buyerName,
        String buyerNip,
        String buyerEmail
) {
    public static InvoiceResponse from(Invoice invoice) {
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getKind(),
                invoice.getStatus(),
                invoice.getNetAmount(),
                invoice.getVatAmount(),
                invoice.getGrossAmount(),
                invoice.getCurrency(),
                invoice.getIssueDate(),
                invoice.getSaleDate(),
                invoice.getPaymentDate(),
                invoice.getPaidDate(),
                invoice.getBuyerName(),
                invoice.getBuyerNip(),
                invoice.getBuyerEmail()
        );
    }
}
