package pl.cafteo.jdgflow.tax.calculator;

import pl.cafteo.jdgflow.tax.domain.TaxRate;

import java.lang.reflect.Field;
import java.math.BigDecimal;

/**
 * Shared {@link TaxRate} fixture for calculator unit tests. Mirrors the V4 seed for 2025
 * so test expectations align with what production code will see at runtime.
 */
final class TestRates {

    private TestRates() {}

    static TaxRate rates2025() {
        TaxRate r = newTaxRate();
        set(r, "year", 2025);
        set(r, "minimumWage", new BigDecimal("4806.00"));
        set(r, "averageWageForecast", new BigDecimal("8673.00"));
        set(r, "preferentialZusBase", new BigDecimal("1441.80"));
        set(r, "standardZusBase", new BigDecimal("5203.80"));
        set(r, "pensionRate", new BigDecimal("0.1952"));
        set(r, "disabilityRate", new BigDecimal("0.0800"));
        set(r, "sicknessRate", new BigDecimal("0.0245"));
        set(r, "accidentRate", new BigDecimal("0.0167"));
        set(r, "laborFundRate", new BigDecimal("0.0245"));
        set(r, "scaleFirstThreshold", new BigDecimal("120000.00"));
        set(r, "scaleLowerRate", new BigDecimal("0.12"));
        set(r, "scaleUpperRate", new BigDecimal("0.32"));
        set(r, "scaleTaxFreeAmount", new BigDecimal("30000.00"));
        set(r, "linearRate", new BigDecimal("0.19"));
        set(r, "ipBoxRate", new BigDecimal("0.05"));
        set(r, "healthRateScale", new BigDecimal("0.09"));
        set(r, "healthRateLinear", new BigDecimal("0.049"));
        set(r, "healthMinBase", new BigDecimal("4806.00"));
        set(r, "healthLinearDeductionCap", new BigDecimal("14500.00"));
        set(r, "mzpRevenueThreshold", new BigDecimal("200000.00"));
        set(r, "mzpFactor", new BigDecimal("0.5"));
        return r;
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

    private static void set(Object target, String name, Object value) {
        try {
            Field f = TaxRate.class.getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
