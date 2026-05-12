package pl.cafteo.jdgflow.expense.api.dto;

import pl.cafteo.jdgflow.expense.domain.Expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ExpenseResponse(
        UUID id,
        CategoryResponse category,
        BigDecimal amount,
        String currency,
        BigDecimal exchangeRate,
        LocalDate expenseDate,
        String description,
        String vendor,
        boolean vatDeductible,
        BigDecimal vatAmount
) {
    public static ExpenseResponse from(Expense e) {
        return new ExpenseResponse(
                e.getId(),
                CategoryResponse.from(e.getCategory()),
                e.getAmount(),
                e.getCurrency(),
                e.getExchangeRate(),
                e.getExpenseDate(),
                e.getDescription(),
                e.getVendor(),
                e.isVatDeductible(),
                e.getVatAmount());
    }
}
