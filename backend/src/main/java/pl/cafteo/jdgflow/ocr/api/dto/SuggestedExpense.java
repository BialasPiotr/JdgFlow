package pl.cafteo.jdgflow.ocr.api.dto;

import pl.cafteo.jdgflow.ocr.service.ParsedReceipt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SuggestedExpense(
        BigDecimal amount,
        String currency,
        BigDecimal vatAmount,
        Boolean vatDeductible,
        LocalDate expenseDate,
        String vendor,
        String description,
        String suggestedCategoryCode,
        UUID suggestedCategoryId,
        Double confidence
) {
    public static SuggestedExpense from(ParsedReceipt parsed, UUID suggestedCategoryId) {
        if (parsed == null) return null;
        return new SuggestedExpense(
                parsed.amount(),
                parsed.currency(),
                parsed.vatAmount(),
                parsed.vatDeductible(),
                parsed.expenseDate(),
                parsed.vendor(),
                parsed.description(),
                parsed.suggestedCategoryCode(),
                suggestedCategoryId,
                parsed.confidence()
        );
    }
}
