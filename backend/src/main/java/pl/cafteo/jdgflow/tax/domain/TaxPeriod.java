package pl.cafteo.jdgflow.tax.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pl.cafteo.jdgflow.common.audit.AuditableEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Table(name = "tax_periods")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaxPeriod extends AuditableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "year", nullable = false)
    private int year;

    @Column(name = "month", nullable = false)
    private int month;

    @Column(name = "revenue", nullable = false, precision = 15, scale = 2)
    private BigDecimal revenue;

    @Column(name = "costs", nullable = false, precision = 15, scale = 2)
    private BigDecimal costs;

    @Column(name = "income", nullable = false, precision = 15, scale = 2)
    private BigDecimal income;

    @Column(name = "zus_social_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal zusSocialTotal;

    @Column(name = "health_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal healthAmount;

    @Column(name = "pit_advance", nullable = false, precision = 15, scale = 2)
    private BigDecimal pitAdvance;

    @Column(name = "vat_due", nullable = false, precision = 15, scale = 2)
    private BigDecimal vatDue;

    @Column(name = "breakdown", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String breakdown;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;

    public static TaxPeriod create(UUID userId, int year, int month,
                                    BigDecimal revenue, BigDecimal costs, BigDecimal income,
                                    BigDecimal zusSocialTotal, BigDecimal healthAmount,
                                    BigDecimal pitAdvance, BigDecimal vatDue,
                                    String breakdownJson) {
        TaxPeriod p = new TaxPeriod();
        p.id = UUID.randomUUID();
        p.userId = userId;
        p.year = year;
        p.month = month;
        p.revenue = revenue;
        p.costs = costs;
        p.income = income;
        p.zusSocialTotal = zusSocialTotal;
        p.healthAmount = healthAmount;
        p.pitAdvance = pitAdvance;
        p.vatDue = vatDue;
        p.breakdown = breakdownJson;
        p.computedAt = Instant.now();
        return p;
    }

    public void replaceComputation(BigDecimal revenue, BigDecimal costs, BigDecimal income,
                                    BigDecimal zusSocialTotal, BigDecimal healthAmount,
                                    BigDecimal pitAdvance, BigDecimal vatDue,
                                    String breakdownJson) {
        this.revenue = revenue;
        this.costs = costs;
        this.income = income;
        this.zusSocialTotal = zusSocialTotal;
        this.healthAmount = healthAmount;
        this.pitAdvance = pitAdvance;
        this.vatDue = vatDue;
        this.breakdown = breakdownJson;
        this.computedAt = Instant.now();
    }
}
