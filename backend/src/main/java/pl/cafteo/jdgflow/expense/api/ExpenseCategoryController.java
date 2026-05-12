package pl.cafteo.jdgflow.expense.api;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.cafteo.jdgflow.expense.api.dto.CategoryResponse;
import pl.cafteo.jdgflow.expense.service.ExpenseQueryService;

import java.util.List;

@RestController
@RequestMapping("/api/expense-categories")
@RequiredArgsConstructor
public class ExpenseCategoryController {

    private final ExpenseQueryService queryService;

    @GetMapping
    public List<CategoryResponse> list() {
        return queryService.listCategories().stream()
                .map(CategoryResponse::from)
                .toList();
    }
}
