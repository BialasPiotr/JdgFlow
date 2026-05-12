package pl.cafteo.jdgflow.tax.calculator;

import org.springframework.stereotype.Component;
import pl.cafteo.jdgflow.common.exception.BusinessException;
import pl.cafteo.jdgflow.tax.domain.TaxRate;
import pl.cafteo.jdgflow.tax.domain.ZusBreakdown;
import pl.cafteo.jdgflow.tax.domain.ZusComponent;
import pl.cafteo.jdgflow.tax.domain.ZusMode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;

@Component
public class ZusCalculator {

    public ZusBreakdown calculate(ZusMode mode, TaxRate rates, boolean voluntarySickness,
                                  BigDecimal previousYearAverageMonthlyIncome) {
        if (mode == ZusMode.ULGA_NA_START) {
            return new ZusBreakdown(mode, BigDecimal.ZERO, Map.of(), BigDecimal.ZERO);
        }

        BigDecimal base = resolveBase(mode, rates, previousYearAverageMonthlyIncome);
        Map<ZusComponent, BigDecimal> components = new EnumMap<>(ZusComponent.class);
        components.put(ZusComponent.PENSION,    multiply(base, rates.getPensionRate()));
        components.put(ZusComponent.DISABILITY, multiply(base, rates.getDisabilityRate()));
        components.put(ZusComponent.ACCIDENT,   multiply(base, rates.getAccidentRate()));
        if (voluntarySickness) {
            components.put(ZusComponent.SICKNESS, multiply(base, rates.getSicknessRate()));
        }
        if (mode == ZusMode.STANDARD) {
            components.put(ZusComponent.LABOR_FUND, multiply(base, rates.getLaborFundRate()));
        }

        BigDecimal total = components.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ZusBreakdown(mode, base, components, total);
    }

    private BigDecimal resolveBase(ZusMode mode, TaxRate rates, BigDecimal averageMonthlyIncome) {
        return switch (mode) {
            case ULGA_NA_START -> BigDecimal.ZERO;
            case PREFERENTIAL  -> rates.getPreferentialZusBase();
            case STANDARD      -> rates.getStandardZusBase();
            case MALY_ZUS_PLUS -> {
                if (averageMonthlyIncome == null) {
                    throw BusinessException.badRequest(
                            "Mały ZUS Plus requires previous year average monthly income");
                }
                BigDecimal raw = averageMonthlyIncome.multiply(rates.getMzpFactor());
                BigDecimal clamped = raw.max(rates.getPreferentialZusBase())
                                        .min(rates.getStandardZusBase());
                yield clamped.setScale(2, RoundingMode.HALF_UP);
            }
        };
    }

    private static BigDecimal multiply(BigDecimal base, BigDecimal rate) {
        return base.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
}
