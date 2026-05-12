package pl.cafteo.jdgflow.invoice.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExternalInvoice(
        Long id,
        String number,
        InvoiceKind kind,
        InvoiceStatus status,
        BigDecimal netAmount,
        BigDecimal vatAmount,
        BigDecimal grossAmount,
        String currency,
        BigDecimal exchangeRate,
        LocalDate issueDate,
        LocalDate saleDate,
        LocalDate paymentDate,
        LocalDate paidDate,
        String buyerName,
        String buyerNip,
        String buyerEmail
) {}
