package pl.cafteo.jdgflow.tax.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Getter
@Table(name = "tax_rates")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaxRate {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "year", nullable = false, unique = true)
    private int year;

    @Column(name = "minimum_wage", nullable = false, precision = 12, scale = 2)
    private BigDecimal minimumWage;

    @Column(name = "average_wage_forecast", nullable = false, precision = 12, scale = 2)
    private BigDecimal averageWageForecast;

    @Column(name = "preferential_zus_base", nullable = false, precision = 12, scale = 2)
    private BigDecimal preferentialZusBase;

    @Column(name = "standard_zus_base", nullable = false, precision = 12, scale = 2)
    private BigDecimal standardZusBase;

    @Column(name = "pension_rate",    nullable = false, precision = 6, scale = 4) private BigDecimal pensionRate;
    @Column(name = "disability_rate", nullable = false, precision = 6, scale = 4) private BigDecimal disabilityRate;
    @Column(name = "sickness_rate",   nullable = false, precision = 6, scale = 4) private BigDecimal sicknessRate;
    @Column(name = "accident_rate",   nullable = false, precision = 6, scale = 4) private BigDecimal accidentRate;
    @Column(name = "labor_fund_rate", nullable = false, precision = 6, scale = 4) private BigDecimal laborFundRate;

    @Column(name = "scale_first_threshold",  nullable = false, precision = 15, scale = 2) private BigDecimal scaleFirstThreshold;
    @Column(name = "scale_lower_rate",       nullable = false, precision = 6,  scale = 4) private BigDecimal scaleLowerRate;
    @Column(name = "scale_upper_rate",       nullable = false, precision = 6,  scale = 4) private BigDecimal scaleUpperRate;
    @Column(name = "scale_tax_free_amount",  nullable = false, precision = 15, scale = 2) private BigDecimal scaleTaxFreeAmount;

    @Column(name = "linear_rate",  nullable = false, precision = 6, scale = 4) private BigDecimal linearRate;
    @Column(name = "ip_box_rate",  nullable = false, precision = 6, scale = 4) private BigDecimal ipBoxRate;

    @Column(name = "health_rate_scale",            nullable = false, precision = 6,  scale = 4) private BigDecimal healthRateScale;
    @Column(name = "health_rate_linear",           nullable = false, precision = 6,  scale = 4) private BigDecimal healthRateLinear;
    @Column(name = "health_min_base",              nullable = false, precision = 12, scale = 2) private BigDecimal healthMinBase;
    @Column(name = "health_linear_deduction_cap",  nullable = false, precision = 12, scale = 2) private BigDecimal healthLinearDeductionCap;

    @Column(name = "mzp_revenue_threshold", nullable = false, precision = 15, scale = 2) private BigDecimal mzpRevenueThreshold;
    @Column(name = "mzp_factor",            nullable = false, precision = 6,  scale = 4) private BigDecimal mzpFactor;

    @Column(name = "notes")
    private String notes;
}
