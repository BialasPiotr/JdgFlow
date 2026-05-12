package pl.cafteo.jdgflow.tax.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MarkPaidRequest(LocalDate paidDate, BigDecimal paidAmount) {}
