package pl.cafteo.jdgflow.dashboard.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.cafteo.jdgflow.auth.domain.User;
import pl.cafteo.jdgflow.auth.domain.UserRepository;
import pl.cafteo.jdgflow.common.exception.BusinessException;
import pl.cafteo.jdgflow.dashboard.api.dto.CashflowMonthEntry;
import pl.cafteo.jdgflow.dashboard.api.dto.CashflowResponse;
import pl.cafteo.jdgflow.dashboard.api.dto.CashflowTotal;
import pl.cafteo.jdgflow.tax.service.CostAggregator;
import pl.cafteo.jdgflow.tax.service.RevenueAggregator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CashflowService {

    private final UserRepository userRepository;
    private final RevenueAggregator revenueAggregator;
    private final CostAggregator costAggregator;

    @Transactional(readOnly = true)
    public CashflowResponse computeYear(UUID userId, int year) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found"));

        CashflowResponse current = buildYear(user, year);
        CashflowResponse previous = buildYear(user, year - 1);
        return new CashflowResponse(current.year(), current.months(), current.total(), previous);
    }

    private CashflowResponse buildYear(User user, int year) {
        List<CashflowMonthEntry> months = new ArrayList<>(12);
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalCosts = BigDecimal.ZERO;
        BigDecimal totalIncome = BigDecimal.ZERO;

        for (int month = 1; month <= 12; month++) {
            YearMonth ym = YearMonth.of(year, month);
            LocalDate from = ym.atDay(1);
            LocalDate to = ym.atEndOfMonth();

            RevenueAggregator.Revenue revenue = revenueAggregator.between(
                    user.getId(), user.getAccountingMethod(), from, to);
            CostAggregator.Costs costs = costAggregator.between(user.getId(), from, to);

            BigDecimal monthIncome = revenue.net().subtract(costs.deductible());

            months.add(new CashflowMonthEntry(month, revenue.net(), costs.total(), monthIncome));
            totalRevenue = totalRevenue.add(revenue.net());
            totalCosts = totalCosts.add(costs.total());
            totalIncome = totalIncome.add(monthIncome);
        }

        return new CashflowResponse(year, months,
                new CashflowTotal(totalRevenue, totalCosts, totalIncome), null);
    }
}
