package pl.cafteo.jdgflow.expense.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.cafteo.jdgflow.common.exception.BusinessException;
import pl.cafteo.jdgflow.expense.api.dto.CategoryResponse;
import pl.cafteo.jdgflow.expense.api.dto.ExpenseSummaryResponse;
import pl.cafteo.jdgflow.expense.domain.Expense;
import pl.cafteo.jdgflow.expense.domain.ExpenseCategory;
import pl.cafteo.jdgflow.expense.domain.ExpenseCategoryRepository;
import pl.cafteo.jdgflow.expense.domain.ExpenseRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseQueryService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseCategoryRepository categoryRepository;

    public Page<Expense> list(UUID userId, UUID categoryId, LocalDate from, LocalDate to,
                              Boolean vatDeductible, Pageable pageable) {
        return expenseRepository.findAll(buildSpec(userId, categoryId, from, to, vatDeductible), pageable);
    }

    public List<Expense> listAll(UUID userId, UUID categoryId, LocalDate from, LocalDate to,
                                  Boolean vatDeductible) {
        return expenseRepository.findAll(
                buildSpec(userId, categoryId, from, to, vatDeductible),
                Sort.by(Sort.Direction.DESC, "expenseDate"));
    }

    private static Specification<Expense> buildSpec(UUID userId, UUID categoryId, LocalDate from,
                                                    LocalDate to, Boolean vatDeductible) {
        Specification<Expense> spec = (root, q, cb) -> cb.equal(root.get("userId"), userId);
        if (categoryId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("category").get("id"), categoryId));
        }
        if (from != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("expenseDate"), from));
        }
        if (to != null) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("expenseDate"), to));
        }
        if (vatDeductible != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("vatDeductible"), vatDeductible));
        }
        return spec;
    }

    public Expense byId(UUID userId, UUID expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> BusinessException.notFound("Expense not found"));
        if (!expense.getUserId().equals(userId)) {
            throw BusinessException.notFound("Expense not found");
        }
        return expense;
    }

    public List<ExpenseCategory> listCategories() {
        return categoryRepository.findAllOrdered();
    }

    public ExpenseSummaryResponse summary(UUID userId, LocalDate from, LocalDate to) {
        var rows = expenseRepository.sumByCategoryBetween(userId, from, to);

        Map<UUID, ExpenseCategory> categoriesById = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(ExpenseCategory::getId, Function.identity()));

        BigDecimal total = rows.stream()
                .map(ExpenseRepository.CategorySummaryRow::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long count = rows.stream()
                .mapToLong(ExpenseRepository.CategorySummaryRow::getCount)
                .sum();

        var breakdown = rows.stream()
                .map(row -> new ExpenseSummaryResponse.CategoryBreakdown(
                        CategoryResponse.from(categoriesById.get(row.getCategoryId())),
                        row.getTotal(),
                        row.getCount()))
                .sorted((a, b) -> b.total().compareTo(a.total()))
                .toList();

        return new ExpenseSummaryResponse(from, to, total, count, breakdown);
    }
}
