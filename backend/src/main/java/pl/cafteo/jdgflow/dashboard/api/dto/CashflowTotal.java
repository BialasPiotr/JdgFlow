package pl.cafteo.jdgflow.dashboard.api.dto;

import java.math.BigDecimal;

public record CashflowTotal(
        BigDecimal revenue,
        BigDecimal costs,
        BigDecimal income
) {
    public static CashflowTotal zero() {
        return new CashflowTotal(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
