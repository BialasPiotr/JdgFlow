package pl.cafteo.jdgflow.expense.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID>, JpaSpecificationExecutor<Expense> {

    @Query("""
            SELECT e.category.id AS categoryId, COALESCE(SUM(e.amount), 0) AS total, COUNT(e) AS count
            FROM Expense e
            WHERE e.userId = :userId
              AND e.expenseDate BETWEEN :from AND :to
            GROUP BY e.category.id
            """)
    List<CategorySummaryRow> sumByCategoryBetween(UUID userId, LocalDate from, LocalDate to);
    @Query("""
            SELECT COALESCE(SUM(e.amount), 0) AS total,
                   COALESCE(SUM(CASE WHEN e.category.deductible = TRUE THEN e.amount ELSE 0 END), 0) AS deductible,
                   COALESCE(SUM(CASE WHEN e.vatDeductible = TRUE THEN e.vatAmount ELSE 0 END), 0) AS vatDeductible
            FROM Expense e
            WHERE e.userId = :userId
              AND e.expenseDate BETWEEN :from AND :to
            """)
    CostRow sumBetween(UUID userId, LocalDate from, LocalDate to);

    interface CategorySummaryRow {
        UUID getCategoryId();
        BigDecimal getTotal();
        long getCount();
    }

    interface CostRow {
        BigDecimal getTotal();
        BigDecimal getDeductible();
        BigDecimal getVatDeductible();
    }
}
