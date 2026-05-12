package pl.cafteo.jdgflow.tax.calculator;

import org.springframework.stereotype.Component;
import pl.cafteo.jdgflow.tax.domain.TaxBreakdown;
import pl.cafteo.jdgflow.tax.domain.TaxRate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class LinearPitCalculator {

    public TaxBreakdown calculate(PitContext ctx, boolean ipBox, TaxRate rates) {
        BigDecimal income = nz(ctx.cumulativeIncome());
        BigDecimal zusSocial = nz(ctx.cumulativeZusSocial());
        BigDecimal healthPaid = nz(ctx.cumulativeHealthPaid());

        BigDecimal deductibleHealth = healthPaid.min(rates.getHealthLinearDeductionCap());
        BigDecimal base = income.subtract(zusSocial).subtract(deductibleHealth).max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal rate = ipBox ? rates.getIpBoxRate() : rates.getLinearRate();
        BigDecimal cumulativeTax = base.multiply(rate).setScale(2, RoundingMode.HALF_UP);

        BigDecimal advancesPaid = nz(ctx.advancesPaidYearToDate());
        BigDecimal monthlyAdvance = cumulativeTax.subtract(advancesPaid).max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        List<TaxBreakdown.Step> steps = new ArrayList<>();
        steps.add(TaxBreakdown.Step.of("Dochód narastająco (rok)", income));
        steps.add(TaxBreakdown.Step.of("− ZUS społeczny narastająco", zusSocial));
        if (healthPaid.compareTo(BigDecimal.ZERO) > 0) {
            steps.add(TaxBreakdown.Step.of(
                    "− Składka zdrowotna odliczalna (limit %s)".formatted(rates.getHealthLinearDeductionCap()),
                    deductibleHealth));
        }
        steps.add(TaxBreakdown.Step.of("= Podstawa opodatkowania", base));
        steps.add(TaxBreakdown.Step.of(
                "× stawka",
                cumulativeTax,
                "%s × %s%%".formatted(base, rate.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString())));
        steps.add(TaxBreakdown.Step.of("− Zapłacone zaliczki", advancesPaid));
        steps.add(TaxBreakdown.Step.of("= Zaliczka PIT za miesiąc", monthlyAdvance));

        return new TaxBreakdown(steps, monthlyAdvance);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
