package pl.cafteo.jdgflow.tax.api.dto;

import pl.cafteo.jdgflow.auth.domain.TaxForm;
import pl.cafteo.jdgflow.auth.domain.User;
import pl.cafteo.jdgflow.tax.domain.AccountingMethod;
import pl.cafteo.jdgflow.tax.domain.ZusMode;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TaxProfileResponse(
        ZusMode zusMode,
        TaxForm taxForm,
        boolean vatPayer,
        AccountingMethod accountingMethod,
        boolean voluntarySickness,
        boolean ipBoxEligible,
        boolean jointSettlement,
        LocalDate businessStartDate,
        BigDecimal previousYearRevenue,
        BigDecimal previousYearIncome
) {
    public static TaxProfileResponse from(User u) {
        return new TaxProfileResponse(
                u.getZusMode(),
                u.getTaxForm(),
                u.isVatPayer(),
                u.getAccountingMethod(),
                u.isVoluntarySickness(),
                u.isIpBoxEligible(),
                u.isJointSettlement(),
                u.getBusinessStartDate(),
                u.getPreviousYearRevenue(),
                u.getPreviousYearIncome());
    }
}
