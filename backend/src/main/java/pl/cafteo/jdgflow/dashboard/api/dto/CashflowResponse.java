package pl.cafteo.jdgflow.dashboard.api.dto;

import java.util.List;

public record CashflowResponse(
        int year,
        List<CashflowMonthEntry> months,
        CashflowTotal total,
        CashflowResponse previousYear
) {}
