package pl.cafteo.jdgflow.tax.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.cafteo.jdgflow.auth.domain.TaxForm;
import pl.cafteo.jdgflow.auth.domain.User;
import pl.cafteo.jdgflow.auth.domain.UserRepository;
import pl.cafteo.jdgflow.common.exception.BusinessException;
import pl.cafteo.jdgflow.tax.calculator.HealthContributionCalculator;
import pl.cafteo.jdgflow.tax.calculator.LinearPitCalculator;
import pl.cafteo.jdgflow.tax.calculator.PitContext;
import pl.cafteo.jdgflow.tax.calculator.ScalePitCalculator;
import pl.cafteo.jdgflow.tax.calculator.ZusCalculator;
import pl.cafteo.jdgflow.tax.domain.MonthlyTaxComputation;
import pl.cafteo.jdgflow.tax.domain.ObligationType;
import pl.cafteo.jdgflow.tax.domain.TaxBreakdown;
import pl.cafteo.jdgflow.tax.domain.TaxObligation;
import pl.cafteo.jdgflow.tax.domain.TaxObligationRepository;
import pl.cafteo.jdgflow.tax.domain.TaxPeriod;
import pl.cafteo.jdgflow.tax.domain.TaxPeriodRepository;
import pl.cafteo.jdgflow.tax.domain.TaxRate;
import pl.cafteo.jdgflow.tax.domain.TaxRateRepository;
import pl.cafteo.jdgflow.tax.domain.ZusBreakdown;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaxPeriodService {

    private static final LocalDate FAR_PAST = LocalDate.of(1970, 1, 1);
    private static final LocalDate FAR_FUTURE = LocalDate.of(9999, 12, 31);

    private final UserRepository userRepository;
    private final TaxRateRepository taxRateRepository;
    private final TaxPeriodRepository taxPeriodRepository;
    private final TaxObligationRepository obligationRepository;
    private final RevenueAggregator revenueAggregator;
    private final CostAggregator costAggregator;
    private final ZusCalculator zusCalculator;
    private final HealthContributionCalculator healthCalculator;
    private final LinearPitCalculator linearPit;
    private final ScalePitCalculator scalePit;
    private final ObjectMapper objectMapper;

    @Transactional
    public List<MonthlyTaxComputation> recomputeYear(UUID userId, int year) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found"));
        TaxRate rates = taxRateRepository.findByYear(year)
                .orElseThrow(() -> BusinessException.badRequest(
                        "Brak stawek podatkowych dla roku " + year + ". Dodaj wpis do tax_rates."));

        int upTo = upperMonthFor(year);
        BigDecimal cumulativeZus = BigDecimal.ZERO;
        BigDecimal cumulativeHealth = BigDecimal.ZERO;
        BigDecimal cumulativeAdvances = BigDecimal.ZERO;

        List<MonthlyTaxComputation> results = new ArrayList<>(upTo);

        for (int month = 1; month <= upTo; month++) {
            YearMonth ym = YearMonth.of(year, month);
            LocalDate monthStart = ym.atDay(1);
            LocalDate monthEnd = ym.atEndOfMonth();
            LocalDate yearStart = LocalDate.of(year, 1, 1);

            RevenueAggregator.Revenue monthRevenue = revenueAggregator.between(
                    userId, user.getAccountingMethod(), monthStart, monthEnd);
            CostAggregator.Costs monthCosts = costAggregator.between(userId, monthStart, monthEnd);

            RevenueAggregator.Revenue ytdRevenue = revenueAggregator.between(
                    userId, user.getAccountingMethod(), yearStart, monthEnd);
            CostAggregator.Costs ytdCosts = costAggregator.between(userId, yearStart, monthEnd);

            BigDecimal monthlyIncome = monthRevenue.net().subtract(monthCosts.deductible());

            ZusBreakdown zus = zusCalculator.calculate(
                    user.getZusMode(), rates, user.isVoluntarySickness(),
                    user.getPreviousYearIncome());
            cumulativeZus = cumulativeZus.add(zus.total());

            TaxBreakdown health = healthCalculator.calculate(user.getTaxForm(), monthlyIncome, rates);
            cumulativeHealth = cumulativeHealth.add(health.total());

            BigDecimal ytdIncome = ytdRevenue.net().subtract(ytdCosts.deductible());
            PitContext pitCtx = new PitContext(ytdIncome, cumulativeZus, cumulativeHealth, cumulativeAdvances);

            TaxBreakdown pit = computePit(user.getTaxForm(), pitCtx, user.isIpBoxEligible(), rates);
            cumulativeAdvances = cumulativeAdvances.add(pit.total());

            BigDecimal vatDue = user.isVatPayer()
                    ? monthRevenue.vat().subtract(monthCosts.vatDeductible())
                            .max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            MonthlyTaxComputation computation = new MonthlyTaxComputation(
                    year, month,
                    monthRevenue.net(), monthCosts.total(), monthlyIncome,
                    zus, health, pit, vatDue);

            TaxPeriod period = upsertPeriod(userId, computation);
            replaceUnpaidObligations(period, user.isVatPayer(), zus.total(), health.total(), pit.total(), vatDue);

            results.add(computation);
        }

        return results;
    }

    private TaxBreakdown computePit(TaxForm form, PitContext ctx, boolean ipBox, TaxRate rates) {
        return switch (form) {
            case LINEAR  -> linearPit.calculate(ctx, ipBox, rates);
            case SCALE   -> scalePit.calculate(ctx, rates);
            case RYCZALT -> throw BusinessException.badRequest(
                    "Ryczałt nie jest jeszcze zaimplementowany");
        };
    }

    private TaxPeriod upsertPeriod(UUID userId, MonthlyTaxComputation c) {
        String breakdownJson = serialize(c);
        return taxPeriodRepository.findByUserIdAndYearAndMonth(userId, c.year(), c.month())
                .map(existing -> {
                    existing.replaceComputation(
                            c.revenue(), c.costs(), c.income(),
                            c.zus().total(), c.health().total(), c.pit().total(), c.vatDue(),
                            breakdownJson);
                    return existing;
                })
                .orElseGet(() -> taxPeriodRepository.save(TaxPeriod.create(
                        userId, c.year(), c.month(),
                        c.revenue(), c.costs(), c.income(),
                        c.zus().total(), c.health().total(), c.pit().total(), c.vatDue(),
                        breakdownJson)));
    }

    private void replaceUnpaidObligations(TaxPeriod period, boolean vatPayer,
                                           BigDecimal zusSocial, BigDecimal zusHealth,
                                           BigDecimal pitAdvance, BigDecimal vatDue) {
        YearMonth nextMonth = YearMonth.of(period.getYear(), period.getMonth()).plusMonths(1);
        LocalDate zusPitDeadline = nextMonth.atDay(20);
        LocalDate vatDeadline = nextMonth.atDay(25);

        List<TaxObligation> existing = obligationRepository.findByPeriodId(period.getId());
        List<TaxObligation> toDelete = existing.stream().filter(o -> !o.isPaid()).toList();
        obligationRepository.deleteAll(toDelete);

        UUID userId = period.getUserId();
        if (zusSocial.compareTo(BigDecimal.ZERO) > 0 && unpaidNotExisting(existing, ObligationType.ZUS_SOCIAL)) {
            obligationRepository.save(TaxObligation.create(period.getId(), userId,
                    ObligationType.ZUS_SOCIAL, zusSocial, zusPitDeadline));
        }
        if (zusHealth.compareTo(BigDecimal.ZERO) > 0 && unpaidNotExisting(existing, ObligationType.ZUS_HEALTH)) {
            obligationRepository.save(TaxObligation.create(period.getId(), userId,
                    ObligationType.ZUS_HEALTH, zusHealth, zusPitDeadline));
        }
        if (pitAdvance.compareTo(BigDecimal.ZERO) > 0 && unpaidNotExisting(existing, ObligationType.PIT_ADVANCE)) {
            obligationRepository.save(TaxObligation.create(period.getId(), userId,
                    ObligationType.PIT_ADVANCE, pitAdvance, zusPitDeadline));
        }
        if (vatPayer && vatDue.compareTo(BigDecimal.ZERO) > 0 && unpaidNotExisting(existing, ObligationType.VAT)) {
            obligationRepository.save(TaxObligation.create(period.getId(), userId,
                    ObligationType.VAT, vatDue, vatDeadline));
        }
    }

    private boolean unpaidNotExisting(List<TaxObligation> existing, ObligationType type) {
        return existing.stream().filter(TaxObligation::isPaid).noneMatch(o -> o.getType() == type);
    }

    @Transactional(readOnly = true)
    public List<TaxPeriod> findYear(UUID userId, int year) {
        return taxPeriodRepository.findByUserIdAndYearOrderByMonthAsc(userId, year);
    }

    @Transactional(readOnly = true)
    public TaxPeriod findMonthOrThrow(UUID userId, int year, int month) {
        return taxPeriodRepository.findByUserIdAndYearAndMonth(userId, year, month)
                .orElseThrow(() -> BusinessException.notFound(
                        "Okres %d-%02d nie został jeszcze policzony".formatted(year, month)));
    }

    @Transactional(readOnly = true)
    public List<TaxObligation> upcomingObligations(UUID userId, LocalDate horizonEnd) {
        return obligationRepository.findByUserIdAndDeadlineBetweenOrderByDeadlineAsc(
                userId, FAR_PAST, horizonEnd == null ? FAR_FUTURE : horizonEnd);
    }

    @Transactional
    public TaxObligation markPaid(UUID userId, UUID obligationId, LocalDate paidDate, BigDecimal paidAmount) {
        TaxObligation obligation = ownedObligation(userId, obligationId);
        obligation.markPaid(
                paidDate == null ? LocalDate.now() : paidDate,
                paidAmount == null ? obligation.getAmount() : paidAmount);
        return obligation;
    }

    @Transactional
    public TaxObligation unmarkPaid(UUID userId, UUID obligationId) {
        TaxObligation obligation = ownedObligation(userId, obligationId);
        obligation.unmarkPaid();
        return obligation;
    }

    private TaxObligation ownedObligation(UUID userId, UUID obligationId) {
        TaxObligation obligation = obligationRepository.findById(obligationId)
                .orElseThrow(() -> BusinessException.notFound("Obowiązek nie znaleziony"));
        if (!obligation.getUserId().equals(userId)) {
            throw BusinessException.notFound("Obowiązek nie znaleziony");
        }
        return obligation;
    }

    private String serialize(MonthlyTaxComputation computation) {
        try {
            return objectMapper.writeValueAsString(computation);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize tax computation", e);
        }
    }

    private static int upperMonthFor(int year) {
        LocalDate today = LocalDate.now();
        if (year < today.getYear()) return 12;
        if (year > today.getYear()) return 12;
        return today.getMonthValue();
    }
}
