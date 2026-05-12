package pl.cafteo.jdgflow.tax.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.cafteo.jdgflow.tax.domain.TaxBreakdown;
import pl.cafteo.jdgflow.tax.domain.TaxRate;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class LinearPitCalculatorTest {

    private final LinearPitCalculator calculator = new LinearPitCalculator();
    private final TaxRate rates = TestRates.rates2025();

    @Test
    @DisplayName("Pierwszy miesiąc — dochód 10 000, ZUS 1773.96, zdrowotna 490 → zaliczka 1469.85")
    void first_month_with_full_zus_and_health() {
        PitContext ctx = new PitContext(
                new BigDecimal("10000"),    // income YTD
                new BigDecimal("1773.96"),  // zus YTD
                new BigDecimal("490.00"),   // health paid YTD
                BigDecimal.ZERO);           // advances paid YTD

        TaxBreakdown result = calculator.calculate(ctx, false, rates);

        // base = 10000 - 1773.96 - 490.00 = 7736.04
        // tax  = 7736.04 × 0.19 = 1469.8476 → 1469.85 HALF_UP
        assertThat(result.total()).isEqualByComparingTo("1469.85");
    }

    @Test
    @DisplayName("Po pięciu miesiącach z zapłaconymi zaliczkami — odejmuje YTD advances")
    void subtracts_paid_advances_year_to_date() {
        PitContext ctx = new PitContext(
                new BigDecimal("50000"),
                new BigDecimal("8869.80"),
                new BigDecimal("2450.00"),
                new BigDecimal("5000.00"));

        TaxBreakdown result = calculator.calculate(ctx, false, rates);

        // base = 50000 - 8869.80 - 2450 = 38680.20
        // total tax YTD = 38680.20 × 0.19 = 7349.238 → 7349.24
        // monthly advance = 7349.24 - 5000.00 = 2349.24
        assertThat(result.total()).isEqualByComparingTo("2349.24");
    }

    @Test
    @DisplayName("Składka zdrowotna powyżej rocznego limitu — odliczamy tylko 14 500")
    void caps_health_deduction_at_annual_limit() {
        PitContext ctx = new PitContext(
                new BigDecimal("200000"),
                new BigDecimal("20000"),
                new BigDecimal("20000"),     // health paid > 14500 cap
                BigDecimal.ZERO);

        TaxBreakdown result = calculator.calculate(ctx, false, rates);

        // deductible = min(20000, 14500) = 14500
        // base = 200000 - 20000 - 14500 = 165500
        // tax  = 165500 × 0.19 = 31445.00
        assertThat(result.total()).isEqualByComparingTo("31445.00");
    }

    @Test
    @DisplayName("Strata (ZUS > dochód) — podatek 0, nie idzie ujemnie")
    void no_negative_tax_when_zus_exceeds_income() {
        PitContext ctx = new PitContext(
                new BigDecimal("1000"),
                new BigDecimal("5000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO);

        TaxBreakdown result = calculator.calculate(ctx, false, rates);

        assertThat(result.total()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("IP BOX — stosujemy 5% zamiast 19%")
    void ip_box_uses_five_percent() {
        PitContext ctx = new PitContext(
                new BigDecimal("100000"),
                new BigDecimal("10000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO);

        TaxBreakdown linear = calculator.calculate(ctx, false, rates);
        TaxBreakdown ipBox = calculator.calculate(ctx, true, rates);

        // base = 90 000 → linear 17 100, ip-box 4 500
        assertThat(linear.total()).isEqualByComparingTo("17100.00");
        assertThat(ipBox.total()).isEqualByComparingTo("4500.00");
    }

    @Test
    @DisplayName("ULGA NA START — health 0, base = income − ZUS, podatek liczony normalnie")
    void no_health_paid_means_no_deduction() {
        PitContext ctx = new PitContext(
                new BigDecimal("10000"),
                new BigDecimal("0"),       // ULGA = no social ZUS
                new BigDecimal("432.54"),  // health is paid, deductible
                BigDecimal.ZERO);

        TaxBreakdown result = calculator.calculate(ctx, false, rates);

        // base = 10000 - 0 - 432.54 = 9567.46
        // tax  = 9567.46 × 0.19 = 1817.8174 → 1817.82
        assertThat(result.total()).isEqualByComparingTo("1817.82");
    }

    @Test
    @DisplayName("Zaliczki YTD wyższe niż łączny PIT — zaliczka miesięczna 0 (nadpłata zostaje)")
    void overpaid_advances_yield_zero_this_month() {
        PitContext ctx = new PitContext(
                new BigDecimal("50000"),
                new BigDecimal("8869.80"),
                BigDecimal.ZERO,
                new BigDecimal("99999"));   // way over

        TaxBreakdown result = calculator.calculate(ctx, false, rates);

        assertThat(result.total()).isEqualByComparingTo("0");
    }
}
