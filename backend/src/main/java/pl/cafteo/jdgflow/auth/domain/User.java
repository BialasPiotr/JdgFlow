package pl.cafteo.jdgflow.auth.domain;

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
import pl.cafteo.jdgflow.tax.domain.AccountingMethod;
import pl.cafteo.jdgflow.tax.domain.ZusMode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends AuditableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "nip")
    private String nip;

    @Column(name = "business_name")
    private String businessName;

    @Enumerated(EnumType.STRING)
    @Column(name = "zus_mode", nullable = false, length = 32)
    private ZusMode zusMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_form", nullable = false, length = 32)
    private TaxForm taxForm;

    @Column(name = "vat_payer", nullable = false)
    private boolean vatPayer;

    @Enumerated(EnumType.STRING)
    @Column(name = "accounting_method", nullable = false, length = 16)
    private AccountingMethod accountingMethod;

    @Column(name = "voluntary_sickness", nullable = false)
    private boolean voluntarySickness;

    @Column(name = "ip_box_eligible", nullable = false)
    private boolean ipBoxEligible;

    @Column(name = "joint_settlement", nullable = false)
    private boolean jointSettlement;

    @Column(name = "business_start_date")
    private LocalDate businessStartDate;

    @Column(name = "previous_year_revenue", precision = 15, scale = 2)
    private BigDecimal previousYearRevenue;

    @Column(name = "previous_year_income", precision = 15, scale = 2)
    private BigDecimal previousYearIncome;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    private User(String email, String passwordHash, String fullName) {
        this.id = UUID.randomUUID();
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.zusMode = ZusMode.MALY_ZUS_PLUS;
        this.taxForm = TaxForm.LINEAR;
        this.vatPayer = true;
        this.accountingMethod = AccountingMethod.ACCRUAL;
        this.voluntarySickness = false;
        this.ipBoxEligible = false;
        this.jointSettlement = false;
        this.enabled = true;
    }

    public static User register(String email, String passwordHash, String fullName) {
        return new User(email, passwordHash, fullName);
    }

    public void updateProfile(String fullName, String nip, String businessName) {
        this.fullName = fullName;
        this.nip = nip;
        this.businessName = businessName;
    }

    public void updateTaxProfile(ZusMode zusMode, TaxForm taxForm, boolean vatPayer,
                                 AccountingMethod accountingMethod, boolean voluntarySickness,
                                 boolean ipBoxEligible, boolean jointSettlement,
                                 LocalDate businessStartDate,
                                 BigDecimal previousYearRevenue, BigDecimal previousYearIncome) {
        this.zusMode = zusMode;
        this.taxForm = taxForm;
        this.vatPayer = vatPayer;
        this.accountingMethod = accountingMethod;
        this.voluntarySickness = voluntarySickness;
        this.ipBoxEligible = ipBoxEligible;
        this.jointSettlement = jointSettlement;
        this.businessStartDate = businessStartDate;
        this.previousYearRevenue = previousYearRevenue;
        this.previousYearIncome = previousYearIncome;
    }

    public void disable() {
        this.enabled = false;
    }
}
