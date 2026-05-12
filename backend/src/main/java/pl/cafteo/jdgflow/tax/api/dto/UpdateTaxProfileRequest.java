package pl.cafteo.jdgflow.tax.api.dto;

import jakarta.validation.constraints.NotNull;
import pl.cafteo.jdgflow.auth.domain.TaxForm;
import pl.cafteo.jdgflow.tax.domain.AccountingMethod;
import pl.cafteo.jdgflow.tax.domain.ZusMode;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateTaxProfileRequest(
        @NotNull ZusMode zusMode,
        @NotNull TaxForm taxForm,
        boolean vatPayer,
        @NotNull AccountingMethod accountingMethod,
        boolean voluntarySickness,
        boolean ipBoxEligible,
        boolean jointSettlement,
        LocalDate businessStartDate,
        BigDecimal previousYearRevenue,
        BigDecimal previousYearIncome
) {}
