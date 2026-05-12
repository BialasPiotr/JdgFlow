package pl.cafteo.jdgflow.tax.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.cafteo.jdgflow.auth.domain.TaxForm;
import pl.cafteo.jdgflow.common.exception.BusinessException;
import pl.cafteo.jdgflow.tax.domain.TaxBreakdown;
import pl.cafteo.jdgflow.tax.domain.TaxRate;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HealthContributionCalculatorTest {

    private final HealthContributionCalculator calculator = new HealthContributionCalculator();
    private final TaxRate rates = rates2025();

    @Test
    @DisplayName("Skala — dochód 10 000, składka = 9% × 10 000 = 900")
    void scale_above_minimum() {
        TaxBreakdown result = calculator.calculate(TaxForm.SCALE, new BigDecimal("10000"), rates);
        assertThat(result.total()).isEqualByComparingTo("900.00");
    }

    @Test
    @DisplayName("Skala — niski dochód 1000 zł podnoszony do podstawy minimalnej (4806), składka = 432.54")
    void scale_below_minimum_uses_minimum_base() {
        TaxBreakdown result = calculator.calculate(TaxForm.SCALE, new BigDecimal("1000"), rates);
        // 4806.00 × 0.09 = 432.54
        assertThat(result.total()).isEqualByComparingTo("432.54");
    }

    @Test
    @DisplayName("Liniowy — dochód 10 000, składka = 4.9% × 10 000 = 490")
    void linear_above_minimum() {
        TaxBreakdown result = calculator.calculate(TaxForm.LINEAR, new BigDecimal("10000"), rates);
        assertThat(result.total()).isEqualByComparingTo("490.00");
    }

    @Test
    @DisplayName("Liniowy — niski dochód 0 zł podnoszony do podstawy minimalnej, składka = 235.49")
    void linear_zero_income_uses_minimum_base() {
        TaxBreakdown result = calculator.calculate(TaxForm.LINEAR, BigDecimal.ZERO, rates);
        // 4806.00 × 0.049 = 235.494 → 235.49 HALF_UP
        assertThat(result.total()).isEqualByComparingTo("235.49");
    }

    @Test
    @DisplayName("Liniowy — null income traktujemy jak zero (fallback do minimum)")
    void linear_null_income_falls_back_to_minimum() {
        TaxBreakdown result = calculator.calculate(TaxForm.LINEAR, null, rates);
        assertThat(result.total()).isEqualByComparingTo("235.49");
    }

    @Test
    @DisplayName("Ryczałt — wyjątek 400 'jeszcze nie zaimplementowane'")
    void ryczalt_throws() {
        assertThatThrownBy(() -> calculator.calculate(TaxForm.RYCZALT, new BigDecimal("5000"), rates))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ryczałtu");
    }

    @Test
    @DisplayName("Breakdown zawiera oznaczenie 'Podstawa podniesiona do minimum' gdy income < minimum")
    void breakdown_marks_minimum_uplift_when_applicable() {
        TaxBreakdown result = calculator.calculate(TaxForm.LINEAR, new BigDecimal("100"), rates);
        assertThat(result.steps())
                .anyMatch(s -> s.label().contains("podniesiona do minimum"));
    }

    @Test
    @DisplayName("Breakdown nie podnosi do minimum gdy income > minimum")
    void breakdown_keeps_actual_base_when_above_minimum() {
        TaxBreakdown result = calculator.calculate(TaxForm.LINEAR, new BigDecimal("10000"), rates);
        assertThat(result.steps())
                .noneMatch(s -> s.label().contains("podniesiona do minimum"));
    }

    private static TaxRate rates2025() {
        TaxRate r = newTaxRate();
        setField(r, "year", 2025);
        setField(r, "minimumWage", new BigDecimal("4806.00"));
        setField(r, "averageWageForecast", new BigDecimal("8673.00"));
        setField(r, "preferentialZusBase", new BigDecimal("1441.80"));
        setField(r, "standardZusBase", new BigDecimal("5203.80"));
        setField(r, "pensionRate", new BigDecimal("0.1952"));
        setField(r, "disabilityRate", new BigDecimal("0.0800"));
        setField(r, "sicknessRate", new BigDecimal("0.0245"));
        setField(r, "accidentRate", new BigDecimal("0.0167"));
        setField(r, "laborFundRate", new BigDecimal("0.0245"));
        setField(r, "scaleFirstThreshold", new BigDecimal("120000.00"));
        setField(r, "scaleLowerRate", new BigDecimal("0.12"));
        setField(r, "scaleUpperRate", new BigDecimal("0.32"));
        setField(r, "scaleTaxFreeAmount", new BigDecimal("30000.00"));
        setField(r, "linearRate", new BigDecimal("0.19"));
        setField(r, "ipBoxRate", new BigDecimal("0.05"));
        setField(r, "healthRateScale", new BigDecimal("0.09"));
        setField(r, "healthRateLinear", new BigDecimal("0.049"));
        setField(r, "healthMinBase", new BigDecimal("4806.00"));
        setField(r, "healthLinearDeductionCap", new BigDecimal("14500.00"));
        setField(r, "mzpRevenueThreshold", new BigDecimal("200000.00"));
        setField(r, "mzpFactor", new BigDecimal("0.5"));
        return r;
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field f = TaxRate.class.getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static TaxRate newTaxRate() {
        try {
            var c = TaxRate.class.getDeclaredConstructor();
            c.setAccessible(true);
            return c.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
