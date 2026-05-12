package pl.cafteo.jdgflow.expense.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ExpenseSummaryResponse(
        LocalDate from,
        LocalDate to,
        BigDecimal total,
        long count,
        List<CategoryBreakdown> byCategory
) {
    public record CategoryBreakdown(
            CategoryResponse category,
            BigDecimal total,
            long count
    ) {}
}
