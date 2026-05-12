package pl.cafteo.jdgflow.tax.domain;

import java.math.BigDecimal;
import java.util.Map;

public record ZusBreakdown(
        ZusMode mode,
        BigDecimal base,
        Map<ZusComponent, BigDecimal> components,
        BigDecimal total
) {}
