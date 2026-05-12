package pl.cafteo.jdgflow.tax.calculator;

import java.math.BigDecimal;

public record PitContext(
        BigDecimal cumulativeIncome,
        BigDecimal cumulativeZusSocial,
        BigDecimal cumulativeHealthPaid,
        BigDecimal advancesPaidYearToDate
) {
    public static PitContext zero() {
        return new PitContext(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
