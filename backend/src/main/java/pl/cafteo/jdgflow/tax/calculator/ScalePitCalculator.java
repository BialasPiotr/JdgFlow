package pl.cafteo.jdgflow.tax.calculator;

import org.springframework.stereotype.Component;
import pl.cafteo.jdgflow.tax.domain.TaxBreakdown;
import pl.cafteo.jdgflow.tax.domain.TaxRate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class ScalePitCalculator {

    public TaxBreakdown calculate(PitContext ctx, TaxRate rates) {
        BigDecimal income = nz(ctx.cumulativeIncome());
        BigDecimal zusSocial = nz(ctx.cumulativeZusSocial());

        BigDecimal base = income.subtract(zusSocial).max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal threshold = rates.getScaleFirstThreshold();
        BigDecimal lowerRate = rates.getScaleLowerRate();
        BigDecimal upperRate = rates.getScaleUpperRate();

        BigDecimal lowerBracket = base.min(threshold);
        BigDecimal upperBracket = base.subtract(threshold).max(BigDecimal.ZERO);
        BigDecimal taxBeforeAllowance = lowerBracket.multiply(lowerRate)
                .add(upperBracket.multiply(upperRate))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal allowance = rates.getScaleTaxFreeAmount().multiply(lowerRate)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal cumulativeTax = taxBeforeAllowance.subtract(allowance).max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal advancesPaid = nz(ctx.advancesPaidYearToDate());
        BigDecimal monthlyAdvance = cumulativeTax.subtract(advancesPaid).max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        List<TaxBreakdown.Step> steps = new ArrayList<>();
        steps.add(TaxBreakdown.Step.of("Dochód narastająco (rok)", income));
        steps.add(TaxBreakdown.Step.of("− ZUS społeczny narastająco", zusSocial));
        steps.add(TaxBreakdown.Step.of("= Podstawa opodatkowania", base));
        steps.add(TaxBreakdown.Step.of(
                "I próg (do %s)".formatted(threshold),
                lowerBracket.multiply(lowerRate).setScale(2, RoundingMode.HALF_UP),
                "%s × %s%%".formatted(lowerBracket, lowerRate.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString())));
        if (upperBracket.compareTo(BigDecimal.ZERO) > 0) {
            steps.add(TaxBreakdown.Step.of(
                    "II próg (powyżej %s)".formatted(threshold),
                    upperBracket.multiply(upperRate).setScale(2, RoundingMode.HALF_UP),
                    "%s × %s%%".formatted(upperBracket, upperRate.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString())));
        }
        steps.add(TaxBreakdown.Step.of(
                "− Kwota zmniejszająca podatek",
                allowance,
                "30 000 × 12%%"));
        steps.add(TaxBreakdown.Step.of("− Zapłacone zaliczki", advancesPaid));
        steps.add(TaxBreakdown.Step.of("= Zaliczka PIT za miesiąc", monthlyAdvance));

        return new TaxBreakdown(steps, monthlyAdvance);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
