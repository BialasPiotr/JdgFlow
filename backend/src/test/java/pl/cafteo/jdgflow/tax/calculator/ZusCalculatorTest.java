package pl.cafteo.jdgflow.tax.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pl.cafteo.jdgflow.common.exception.BusinessException;
import pl.cafteo.jdgflow.tax.domain.TaxRate;
import pl.cafteo.jdgflow.tax.domain.ZusBreakdown;
import pl.cafteo.jdgflow.tax.domain.ZusComponent;
import pl.cafteo.jdgflow.tax.domain.ZusMode;

import java.math.BigDecimal;
import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Each test is a named tax scenario. Numbers are hand-verified against kalkulatorB2B for 2025 inputs
 * (minimum wage 4806, preferential base 1441.80, standard base 5203.80) so any drift in the calculator
 * is immediately visible.
 */
class ZusCalculatorTest {

    private final ZusCalculator calculator = new ZusCalculator();
    private final TaxRate rates2025 = rates2025();

    @Nested
    @DisplayName("Tryb ULGA_NA_START — pierwsze 6 miesięcy działalności")
    class UlgaNaStart {

        @Test
        @DisplayName("brak składek społecznych, podstawa zerowa, suma zero")
        void no_social_zus_at_all() {
            ZusBreakdown result = calculator.calculate(ZusMode.ULGA_NA_START, rates2025, false, null);

            assertThat(result.mode()).isEqualTo(ZusMode.ULGA_NA_START);
            assertThat(result.base()).isEqualByComparingTo("0");
            assertThat(result.components()).isEmpty();
            assertThat(result.total()).isEqualByComparingTo("0");
        }
    }

    @Nested
    @DisplayName("Tryb PREFERENTIAL — kolejne 24 miesiące, podstawa 30% min. wynagrodzenia")
    class Preferential {

        @Test
        @DisplayName("składki bez chorobowej i bez Funduszu Pracy: 281.44 + 115.34 + 24.08 = 420.86")
        void preferential_zus_without_voluntary_sickness() {
            ZusBreakdown result = calculator.calculate(ZusMode.PREFERENTIAL, rates2025, false, null);

            assertThat(result.base()).isEqualByComparingTo("1441.80");
            assertThat(result.components().get(ZusComponent.PENSION)).isEqualByComparingTo("281.44");
            assertThat(result.components().get(ZusComponent.DISABILITY)).isEqualByComparingTo("115.34");
            assertThat(result.components().get(ZusComponent.ACCIDENT)).isEqualByComparingTo("24.08");
            assertThat(result.components()).doesNotContainKey(ZusComponent.SICKNESS);
            assertThat(result.components()).doesNotContainKey(ZusComponent.LABOR_FUND);
            assertThat(result.total()).isEqualByComparingTo("420.86");
        }

        @Test
        @DisplayName("z dobrowolną chorobową doliczamy 35.32 zł")
        void preferential_zus_with_voluntary_sickness() {
            ZusBreakdown result = calculator.calculate(ZusMode.PREFERENTIAL, rates2025, true, null);

            assertThat(result.components().get(ZusComponent.SICKNESS)).isEqualByComparingTo("35.32");
            assertThat(result.total()).isEqualByComparingTo("456.18"); // 420.86 + 35.32
        }

        @Test
        @DisplayName("Fundusz Pracy nie obowiązuje w trybie preferencyjnym")
        void preferential_excludes_labor_fund() {
            ZusBreakdown result = calculator.calculate(ZusMode.PREFERENTIAL, rates2025, false, null);

            assertThat(result.components()).doesNotContainKey(ZusComponent.LABOR_FUND);
        }
    }

    @Nested
    @DisplayName("Tryb STANDARD — duży ZUS, podstawa 60% przeciętnego wynagrodzenia")
    class Standard {

        @Test
        @DisplayName("składki naliczane od 5203.80, łącznie z Funduszem Pracy")
        void standard_zus_includes_labor_fund() {
            ZusBreakdown result = calculator.calculate(ZusMode.STANDARD, rates2025, true, null);

            assertThat(result.base()).isEqualByComparingTo("5203.80");
            // 5203.80 × 0.1952 = 1015.78
            assertThat(result.components().get(ZusComponent.PENSION)).isEqualByComparingTo("1015.78");
            // 5203.80 × 0.0800 = 416.30
            assertThat(result.components().get(ZusComponent.DISABILITY)).isEqualByComparingTo("416.30");
            // 5203.80 × 0.0245 = 127.49
            assertThat(result.components().get(ZusComponent.SICKNESS)).isEqualByComparingTo("127.49");
            // 5203.80 × 0.0167 = 86.90
            assertThat(result.components().get(ZusComponent.ACCIDENT)).isEqualByComparingTo("86.90");
            // 5203.80 × 0.0245 = 127.49
            assertThat(result.components().get(ZusComponent.LABOR_FUND)).isEqualByComparingTo("127.49");
            assertThat(result.total()).isEqualByComparingTo("1773.96");
        }

        @Test
        @DisplayName("bez dobrowolnej chorobowej Fundusz Pracy nadal się liczy")
        void standard_zus_without_sickness_keeps_labor_fund() {
            ZusBreakdown result = calculator.calculate(ZusMode.STANDARD, rates2025, false, null);

            assertThat(result.components()).doesNotContainKey(ZusComponent.SICKNESS);
            assertThat(result.components()).containsKey(ZusComponent.LABOR_FUND);
        }
    }

    @Nested
    @DisplayName("Tryb MALY_ZUS_PLUS — podstawa 50% średniego dochodu, ograniczona do widełek")
    class MalyZusPlus {

        @Test
        @DisplayName("średni dochód 8000 zł → podstawa 4000 zł (mieści się w widełkach)")
        void mzp_base_within_brackets() {
            ZusBreakdown result = calculator.calculate(
                    ZusMode.MALY_ZUS_PLUS, rates2025, false, new BigDecimal("8000.00"));

            assertThat(result.base()).isEqualByComparingTo("4000.00");
            // 4000 × 0.1952 = 780.80
            assertThat(result.components().get(ZusComponent.PENSION)).isEqualByComparingTo("780.80");
        }

        @Test
        @DisplayName("średni dochód 1000 zł → podstawa podniesiona do preferencyjnej (1441.80)")
        void mzp_base_clamped_up_to_preferential_minimum() {
            ZusBreakdown result = calculator.calculate(
                    ZusMode.MALY_ZUS_PLUS, rates2025, false, new BigDecimal("1000.00"));

            assertThat(result.base()).isEqualByComparingTo("1441.80");
        }

        @Test
        @DisplayName("średni dochód 20 000 zł → podstawa ścięta do standardowej (5203.80)")
        void mzp_base_clamped_down_to_standard_maximum() {
            ZusBreakdown result = calculator.calculate(
                    ZusMode.MALY_ZUS_PLUS, rates2025, false, new BigDecimal("20000.00"));

            assertThat(result.base()).isEqualByComparingTo("5203.80");
        }

        @Test
        @DisplayName("brak danych o dochodzie → wyjątek 400")
        void mzp_without_income_throws() {
            assertThatThrownBy(() ->
                    calculator.calculate(ZusMode.MALY_ZUS_PLUS, rates2025, false, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Mały ZUS Plus");
        }

        @Test
        @DisplayName("Fundusz Pracy nie obowiązuje w MZP nawet przy wysokiej podstawie")
        void mzp_excludes_labor_fund_even_at_max_base() {
            ZusBreakdown result = calculator.calculate(
                    ZusMode.MALY_ZUS_PLUS, rates2025, false, new BigDecimal("20000.00"));

            assertThat(result.components()).doesNotContainKey(ZusComponent.LABOR_FUND);
        }
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
