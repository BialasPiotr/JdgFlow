package pl.cafteo.jdgflow.tax.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.cafteo.jdgflow.expense.domain.ExpenseRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CostAggregator {

    private final ExpenseRepository expenseRepository;

    public Costs between(UUID userId, LocalDate from, LocalDate to) {
        ExpenseRepository.CostRow row = expenseRepository.sumBetween(userId, from, to);
        return new Costs(
                nz(row.getTotal()),
                nz(row.getDeductible()),
                nz(row.getVatDeductible()));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    public record Costs(BigDecimal total, BigDecimal deductible, BigDecimal vatDeductible) {}
}
