package pl.cafteo.jdgflow.tax.domain;

import java.math.BigDecimal;
import java.util.List;

public record TaxBreakdown(List<Step> steps, BigDecimal total) {

    public record Step(String label, BigDecimal amount, String formula) {
        public static Step of(String label, BigDecimal amount) {
            return new Step(label, amount, null);
        }

        public static Step of(String label, BigDecimal amount, String formula) {
            return new Step(label, amount, formula);
        }
    }
}
