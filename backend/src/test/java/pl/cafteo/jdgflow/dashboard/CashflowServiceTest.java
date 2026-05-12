package pl.cafteo.jdgflow.dashboard;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.cafteo.jdgflow.auth.domain.User;
import pl.cafteo.jdgflow.auth.domain.UserRepository;
import pl.cafteo.jdgflow.common.exception.BusinessException;
import pl.cafteo.jdgflow.dashboard.api.dto.CashflowResponse;
import pl.cafteo.jdgflow.dashboard.service.CashflowService;
import pl.cafteo.jdgflow.tax.domain.AccountingMethod;
import pl.cafteo.jdgflow.tax.service.CostAggregator;
import pl.cafteo.jdgflow.tax.service.RevenueAggregator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CashflowService}. Verifies that:
 * - the response carries 12 months for both the requested year and the previous year,
 * - totals match the sum of monthly revenue/costs,
 * - aggregator queries respect the user's accounting method.
 */
@ExtendWith(MockitoExtension.class)
class CashflowServiceTest {

    @Mock UserRepository userRepository;
    @Mock RevenueAggregator revenueAggregator;
    @Mock CostAggregator costAggregator;

    @InjectMocks CashflowService service;

    @Test
    @DisplayName("computeYear — pełne 12 miesięcy + previousYear")
    void returns_full_year_with_previous_year_attached() {
        User user = stubUser(AccountingMethod.ACCRUAL);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(revenueAggregator.between(eq(user.getId()), any(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new RevenueAggregator.Revenue(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        when(costAggregator.between(eq(user.getId()), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new CostAggregator.Costs(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

        CashflowResponse response = service.computeYear(user.getId(), 2026);

        assertThat(response.year()).isEqualTo(2026);
        assertThat(response.months()).hasSize(12);
        assertThat(response.previousYear()).isNotNull();
        assertThat(response.previousYear().year()).isEqualTo(2025);
        assertThat(response.previousYear().months()).hasSize(12);
        assertThat(response.previousYear().previousYear()).isNull();
    }

    @Test
    @DisplayName("computeYear — totale to suma miesięcy")
    void totals_equal_sum_of_monthly_entries() {
        User user = stubUser(AccountingMethod.ACCRUAL);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(revenueAggregator.between(eq(user.getId()), any(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new RevenueAggregator.Revenue(
                        new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("123.00")));
        when(costAggregator.between(eq(user.getId()), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new CostAggregator.Costs(
                        new BigDecimal("30.00"), new BigDecimal("30.00"), BigDecimal.ZERO));

        CashflowResponse response = service.computeYear(user.getId(), 2026);

        // 12 × 100 = 1200 revenue, 12 × 30 = 360 costs, 12 × (100-30) = 840 income
        assertThat(response.total().revenue()).isEqualByComparingTo("1200.00");
        assertThat(response.total().costs()).isEqualByComparingTo("360.00");
        assertThat(response.total().income()).isEqualByComparingTo("840.00");
    }

    @Test
    @DisplayName("computeYear — używa metody księgowania użytkownika (CASH → paid_date)")
    void respects_user_accounting_method() {
        User user = stubUser(AccountingMethod.CASH);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(revenueAggregator.between(eq(user.getId()), eq(AccountingMethod.CASH), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new RevenueAggregator.Revenue(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        when(costAggregator.between(eq(user.getId()), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new CostAggregator.Costs(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

        service.computeYear(user.getId(), 2026);

        // 12 months for current year + 12 for previous = 24 calls each
        verify(revenueAggregator, times(24))
                .between(eq(user.getId()), eq(AccountingMethod.CASH), any(LocalDate.class), any(LocalDate.class));
        verify(costAggregator, times(24))
                .between(eq(user.getId()), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    @DisplayName("computeYear — brakujący użytkownik → 404")
    void missing_user_throws_not_found() {
        UUID missing = UUID.randomUUID();
        when(userRepository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.computeYear(missing, 2026))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("User not found");
    }

    private static User stubUser(AccountingMethod method) {
        User u = User.register("test@example.com", "hash", "Test");
        // Re-use existing setter — nothing else to do because register() already provides defaults.
        u.updateTaxProfile(u.getZusMode(), u.getTaxForm(), u.isVatPayer(),
                method, u.isVoluntarySickness(), u.isIpBoxEligible(),
                u.isJointSettlement(), null, null, null);
        return u;
    }
}
