package pl.cafteo.jdgflow.ocr.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ParsedReceipt(
        BigDecimal amount,
        String currency,
        BigDecimal vatAmount,
        Boolean vatDeductible,
        LocalDate expenseDate,
        String vendor,
        String description,
        String suggestedCategoryCode,
        Double confidence
) {}
