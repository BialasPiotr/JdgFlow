package pl.cafteo.jdgflow.expense.api.dto;

import pl.cafteo.jdgflow.expense.domain.ExpenseCategory;

import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String code,
        String name,
        String icon,
        String color,
        boolean deductible
) {
    public static CategoryResponse from(ExpenseCategory category) {
        return new CategoryResponse(
                category.getId(),
                category.getCode(),
                category.getName(),
                category.getIcon(),
                category.getColor(),
                category.isDeductible());
    }
}
