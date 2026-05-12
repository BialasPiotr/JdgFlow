package pl.cafteo.jdgflow.expense.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.cafteo.jdgflow.common.csv.CsvResponse;
import pl.cafteo.jdgflow.common.security.AuthenticatedUser;
import pl.cafteo.jdgflow.expense.api.dto.CreateExpenseRequest;
import pl.cafteo.jdgflow.expense.api.dto.ExpenseResponse;
import pl.cafteo.jdgflow.expense.api.dto.ExpenseSummaryResponse;
import pl.cafteo.jdgflow.expense.api.dto.UpdateExpenseRequest;
import pl.cafteo.jdgflow.expense.domain.Expense;
import pl.cafteo.jdgflow.expense.service.ExpenseCsvExporter;
import pl.cafteo.jdgflow.expense.service.ExpenseQueryService;
import pl.cafteo.jdgflow.expense.service.ExpenseService;
import pl.cafteo.jdgflow.invoice.api.dto.PageResponse;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;
    private final ExpenseQueryService queryService;
    private final ExpenseCsvExporter csvExporter;

    @GetMapping
    public PageResponse<ExpenseResponse> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Boolean vatDeductible,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by(Sort.Direction.DESC, "expenseDate").and(Sort.by(Sort.Direction.DESC, "createdAt")));
        Page<Expense> result = queryService.list(user.id(), categoryId, from, to, vatDeductible, pageable);
        return PageResponse.from(result, ExpenseResponse::from);
    }

    @GetMapping("/{id}")
    public ExpenseResponse get(@AuthenticationPrincipal AuthenticatedUser user,
                               @PathVariable UUID id) {
        return ExpenseResponse.from(queryService.byId(user.id(), id));
    }

    @GetMapping(path = "/export.csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Boolean vatDeductible) {

        var expenses = queryService.listAll(user.id(), categoryId, from, to, vatDeductible);
        String filename = "wydatki-" + LocalDate.now() + ".csv";
        return CsvResponse.attachment(filename, csvExporter.toCsv(expenses));
    }

    @GetMapping("/summary")
    public ExpenseSummaryResponse summary(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return queryService.summary(user.id(), from, to);
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> create(@AuthenticationPrincipal AuthenticatedUser user,
                                                  @Valid @RequestBody CreateExpenseRequest request) {
        Expense expense = expenseService.create(user.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ExpenseResponse.from(expense));
    }

    @PutMapping("/{id}")
    public ExpenseResponse update(@AuthenticationPrincipal AuthenticatedUser user,
                                  @PathVariable UUID id,
                                  @Valid @RequestBody UpdateExpenseRequest request) {
        return ExpenseResponse.from(expenseService.update(user.id(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser user,
                                       @PathVariable UUID id) {
        expenseService.delete(user.id(), id);
        return ResponseEntity.noContent().build();
    }
}
