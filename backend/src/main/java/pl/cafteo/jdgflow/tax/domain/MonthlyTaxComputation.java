package pl.cafteo.jdgflow.tax.domain;

import java.math.BigDecimal;

public record MonthlyTaxComputation(
        int year,
        int month,
        BigDecimal revenue,
        BigDecimal costs,
        BigDecimal income,
        ZusBreakdown zus,
        TaxBreakdown health,
        TaxBreakdown pit,
        BigDecimal vatDue
) {}
