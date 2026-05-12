package pl.cafteo.jdgflow.tax.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.cafteo.jdgflow.common.audit.AuditableEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
@Table(name = "tax_obligations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaxObligation extends AuditableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "period_id", nullable = false)
    private UUID periodId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private ObligationType type;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "deadline", nullable = false)
    private LocalDate deadline;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Column(name = "paid_amount", precision = 15, scale = 2)
    private BigDecimal paidAmount;

    public static TaxObligation create(UUID periodId, UUID userId, ObligationType type,
                                        BigDecimal amount, LocalDate deadline) {
        TaxObligation o = new TaxObligation();
        o.id = UUID.randomUUID();
        o.periodId = periodId;
        o.userId = userId;
        o.type = type;
        o.amount = amount;
        o.deadline = deadline;
        return o;
    }

    public void markPaid(LocalDate paidDate, BigDecimal paidAmount) {
        this.paidDate = paidDate;
        this.paidAmount = paidAmount;
    }

    public void unmarkPaid() {
        this.paidDate = null;
        this.paidAmount = null;
    }

    public boolean isPaid() {
        return paidDate != null;
    }
}
