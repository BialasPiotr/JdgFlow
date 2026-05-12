package pl.cafteo.jdgflow.expense.domain;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, UUID> {

    default List<ExpenseCategory> findAllOrdered() {
        return findAll(Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("name")));
    }
}
