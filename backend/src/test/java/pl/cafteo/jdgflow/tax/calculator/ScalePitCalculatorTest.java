package pl.cafteo.jdgflow.tax.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.cafteo.jdgflow.tax.domain.TaxBreakdown;
import pl.cafteo.jdgflow.tax.domain.TaxRate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ScalePitCalculatorTest {

    private final ScalePitCalculator calculator = new ScalePitCalculator();
    private final TaxRate rates = TestRates.rates2025();

    @Test
    @DisplayName("Niskie dochody — kwota wolna pokrywa cały PIT, zaliczka = 0")
    void low_income_fully_covered_by_tax_free_allowance() {
        // base 25k → tax 12% = 3000, allowance 3600 → 0
        PitContext ctx = new PitContext(
                new BigDecimal("30000"),
                new BigDecimal("5000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO);

        TaxBreakdown result = calculator.calculate(ctx, rates);

        assertThat(result.total()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Średnie dochody — pierwszy próg 12%, kwota wolna odjęta")
    void mid_income_uses_only_first_bracket() {
        // base = 100000 - 10000 = 90000
        // tax  = 90000 × 12% = 10800
        // − allowance 3600 → 7200
        PitContext ctx = new PitContext(
                new BigDecimal("100000"),
                new BigDecimal("10000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO);

        TaxBreakdown result = calculator.calculate(ctx, rates);

        assertThat(result.total()).isEqualByComparingTo("7200.00");
    }

    @Test
    @DisplayName("Przekroczenie progu 120k — łączymy 12% i 32%")
    void crosses_into_upper_bracket() {
        // base = 200000 - 20000 = 180000
        // I próg: 120000 × 12% = 14400
        // II próg: 60000 × 32%  = 19200
        // suma   33600
        // − allowance 3600 = 30000
        PitContext ctx = new PitContext(
                new BigDecimal("200000"),
                new BigDecimal("20000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO);

        TaxBreakdown result = calculator.calculate(ctx, rates);

        assertThat(result.total()).isEqualByComparingTo("30000.00");
    }

    @Test
    @DisplayName("Składka zdrowotna NIE jest odliczana na skali (Polski Ład)")
    void health_is_not_deductible_on_scale() {
        // Same case as mid_income but with health_paid = 5000 — expected tax unchanged
        PitContext withHealth = new PitContext(
                new BigDecimal("100000"),
                new BigDecimal("10000"),
                new BigDecimal("5000"),    // ignored
                BigDecimal.ZERO);

        TaxBreakdown result = calculator.calculate(withHealth, rates);

        assertThat(result.total()).isEqualByComparingTo("7200.00");
    }

    @Test
    @DisplayName("Zapłacone zaliczki YTD pomniejszają tegomiesięczną zaliczkę")
    void advances_already_paid_reduce_this_month() {
        // tax YTD = 7200 (jak wyżej), advances paid 5000 → this month 2200
        PitContext ctx = new PitContext(
                new BigDecimal("100000"),
                new BigDecimal("10000"),
                BigDecimal.ZERO,
                new BigDecimal("5000"));

        TaxBreakdown result = calculator.calculate(ctx, rates);

        assertThat(result.total()).isEqualByComparingTo("2200.00");
    }

    @Test
    @DisplayName("ZUS > dochód → podstawa 0, podatek 0")
    void no_tax_when_zus_exceeds_income() {
        PitContext ctx = new PitContext(
                new BigDecimal("5000"),
                new BigDecimal("8000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO);

        TaxBreakdown result = calculator.calculate(ctx, rates);

        assertThat(result.total()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Breakdown na wysokich dochodach pokazuje obie sekcje progowe")
    void breakdown_shows_both_brackets_when_above_threshold() {
        PitContext ctx = new PitContext(
                new BigDecimal("200000"),
                new BigDecimal("20000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO);

        TaxBreakdown result = calculator.calculate(ctx, rates);

        assertThat(result.steps()).anyMatch(s -> s.label().startsWith("I próg"));
        assertThat(result.steps()).anyMatch(s -> s.label().startsWith("II próg"));
    }

    @Test
    @DisplayName("Breakdown na niskich dochodach pomija II próg")
    void breakdown_skips_upper_bracket_when_below_threshold() {
        PitContext ctx = new PitContext(
                new BigDecimal("100000"),
                new BigDecimal("10000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO);

        TaxBreakdown result = calculator.calculate(ctx, rates);

        assertThat(result.steps()).noneMatch(s -> s.label().startsWith("II próg"));
    }
}
