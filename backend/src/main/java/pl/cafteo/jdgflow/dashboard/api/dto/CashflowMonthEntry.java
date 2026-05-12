package pl.cafteo.jdgflow.dashboard.api.dto;

import java.math.BigDecimal;

public record CashflowMonthEntry(
        int month,
        BigDecimal revenue,
        BigDecimal costs,
        BigDecimal income
) {}
