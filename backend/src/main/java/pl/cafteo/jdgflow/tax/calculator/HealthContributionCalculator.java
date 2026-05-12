package pl.cafteo.jdgflow.tax.calculator;

import org.springframework.stereotype.Component;
import pl.cafteo.jdgflow.auth.domain.TaxForm;
import pl.cafteo.jdgflow.common.exception.BusinessException;
import pl.cafteo.jdgflow.tax.domain.TaxBreakdown;
import pl.cafteo.jdgflow.tax.domain.TaxRate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class HealthContributionCalculator {

    public TaxBreakdown calculate(TaxForm taxForm, BigDecimal monthlyIncome, TaxRate rates) {
        BigDecimal income = monthlyIncome == null ? BigDecimal.ZERO : monthlyIncome;
        BigDecimal base = income.max(rates.getHealthMinBase()).setScale(2, RoundingMode.HALF_UP);

        BigDecimal rate = switch (taxForm) {
            case SCALE  -> rates.getHealthRateScale();
            case LINEAR -> rates.getHealthRateLinear();
            case RYCZALT -> throw BusinessException.badRequest(
                    "Składka zdrowotna dla ryczałtu nie jest jeszcze zaimplementowana");
        };

        BigDecimal amount = base.multiply(rate).setScale(2, RoundingMode.HALF_UP);

        List<TaxBreakdown.Step> steps = new ArrayList<>();
        steps.add(TaxBreakdown.Step.of("Dochód miesięczny", income));
        if (income.compareTo(rates.getHealthMinBase()) < 0) {
            steps.add(TaxBreakdown.Step.of(
                    "Podstawa podniesiona do minimum (min. wynagrodzenie)", base));
        } else {
            steps.add(TaxBreakdown.Step.of("Podstawa", base));
        }
        steps.add(TaxBreakdown.Step.of(
                "Składka zdrowotna", amount,
                "%s × %s%%".formatted(base, rate.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString())));

        return new TaxBreakdown(steps, amount);
    }
}
