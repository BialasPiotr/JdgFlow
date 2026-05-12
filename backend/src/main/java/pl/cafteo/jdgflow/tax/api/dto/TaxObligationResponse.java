package pl.cafteo.jdgflow.tax.api.dto;

import pl.cafteo.jdgflow.tax.domain.ObligationType;
import pl.cafteo.jdgflow.tax.domain.TaxObligation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TaxObligationResponse(
        UUID id,
        UUID periodId,
        ObligationType type,
        BigDecimal amount,
        LocalDate deadline,
        LocalDate paidDate,
        BigDecimal paidAmount,
        boolean isPaid
) {
    public static TaxObligationResponse from(TaxObligation o) {
        return new TaxObligationResponse(
                o.getId(),
                o.getPeriodId(),
                o.getType(),
                o.getAmount(),
                o.getDeadline(),
                o.getPaidDate(),
                o.getPaidAmount(),
                o.isPaid());
    }
}
