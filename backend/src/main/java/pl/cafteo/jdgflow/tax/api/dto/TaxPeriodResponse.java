package pl.cafteo.jdgflow.tax.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pl.cafteo.jdgflow.tax.domain.TaxObligation;
import pl.cafteo.jdgflow.tax.domain.TaxPeriod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TaxPeriodResponse(
        UUID id,
        int year,
        int month,
        BigDecimal revenue,
        BigDecimal costs,
        BigDecimal income,
        BigDecimal zusSocialTotal,
        BigDecimal healthAmount,
        BigDecimal pitAdvance,
        BigDecimal vatDue,
        JsonNode breakdown,
        Instant computedAt,
        List<TaxObligationResponse> obligations
) {
    public static TaxPeriodResponse from(TaxPeriod p, List<TaxObligation> obligations, ObjectMapper mapper) {
        JsonNode breakdownNode;
        try {
            breakdownNode = p.getBreakdown() == null ? null : mapper.readTree(p.getBreakdown());
        } catch (Exception e) {
            breakdownNode = null;
        }
        return new TaxPeriodResponse(
                p.getId(),
                p.getYear(),
                p.getMonth(),
                p.getRevenue(),
                p.getCosts(),
                p.getIncome(),
                p.getZusSocialTotal(),
                p.getHealthAmount(),
                p.getPitAdvance(),
                p.getVatDue(),
                breakdownNode,
                p.getComputedAt(),
                obligations.stream().map(TaxObligationResponse::from).toList());
    }
}
