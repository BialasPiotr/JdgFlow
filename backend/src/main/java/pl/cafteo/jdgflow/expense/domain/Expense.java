package pl.cafteo.jdgflow.expense.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "expenses")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Expense extends AuditableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ExpenseCategory category;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "exchange_rate", precision = 12, scale = 6)
    private BigDecimal exchangeRate;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "vendor", length = 255)
    private String vendor;

    @Column(name = "vat_deductible", nullable = false)
    private boolean vatDeductible;

    @Column(name = "vat_amount", precision = 15, scale = 2)
    private BigDecimal vatAmount;

    @Column(name = "receipt_id")
    private UUID receiptId;

    public static Expense create(UUID userId, ExpenseCategory category, BigDecimal amount,
                                  String currency, LocalDate expenseDate, String description) {
        Expense e = new Expense();
        e.id = UUID.randomUUID();
        e.userId = userId;
        e.category = category;
        e.amount = amount;
        e.currency = currency;
        e.expenseDate = expenseDate;
        e.description = description;
        e.vatDeductible = false;
        return e;
    }

    public void changeCategory(ExpenseCategory category) {
        this.category = category;
    }

    public void updateDetails(BigDecimal amount, String currency, LocalDate expenseDate,
                              String description, String vendor) {
        this.amount = amount;
        this.currency = currency;
        this.expenseDate = expenseDate;
        this.description = description;
        this.vendor = vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public void applyVat(boolean deductible, BigDecimal vatAmount) {
        this.vatDeductible = deductible;
        this.vatAmount = deductible ? vatAmount : null;
    }

    public void applyExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public void attachReceipt(UUID receiptId) {
        this.receiptId = receiptId;
    }
}
