package pl.cafteo.jdgflow.expense;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.cafteo.jdgflow.common.exception.BusinessException;
import pl.cafteo.jdgflow.expense.api.dto.CreateExpenseRequest;
import pl.cafteo.jdgflow.expense.api.dto.UpdateExpenseRequest;
import pl.cafteo.jdgflow.expense.domain.Expense;
import pl.cafteo.jdgflow.expense.domain.ExpenseCategory;
import pl.cafteo.jdgflow.expense.domain.ExpenseCategoryRepository;
import pl.cafteo.jdgflow.expense.domain.ExpenseRepository;
import pl.cafteo.jdgflow.expense.service.ExpenseService;
import pl.cafteo.jdgflow.ocr.domain.Receipt;
import pl.cafteo.jdgflow.ocr.domain.ReceiptRepository;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ExpenseService} with all collaborators mocked. Verifies the receipt
 * link rules (1:1, single-use) and ownership checks that the integration tests don't cover
 * directly.
 */
@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock ExpenseRepository expenseRepository;
    @Mock ExpenseCategoryRepository categoryRepository;
    @Mock ReceiptRepository receiptRepository;

    @InjectMocks ExpenseService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID otherUserId = UUID.randomUUID();

    @Test
    @DisplayName("create — bez paragonu zapisuje wydatek bez wywoływania ReceiptRepository")
    void create_without_receipt_saves_expense() {
        ExpenseCategory category = stubCategory();
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));

        Expense saved = service.create(userId, new CreateExpenseRequest(
                category.getId(), new BigDecimal("199.00"), "PLN",
                LocalDate.of(2026, 4, 15), "Subskrypcja", "Acme",
                false, null, null));

        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getReceiptId()).isNull();
        verify(receiptRepository, never()).findByIdAndUserId(any(), any());
    }

    @Test
    @DisplayName("create — z paragonem ustawia receipt_id i wywołuje linkExpense (dwustronny link)")
    void create_with_receipt_links_both_sides() {
        ExpenseCategory category = stubCategory();
        Receipt receipt = stubReceipt(userId, /* expenseId */ null);

        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(receiptRepository.findByIdAndUserId(receipt.getId(), userId)).thenReturn(Optional.of(receipt));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));

        Expense saved = service.create(userId, new CreateExpenseRequest(
                category.getId(), new BigDecimal("50.00"), "PLN",
                LocalDate.now(), "Paliwo", "Orlen",
                false, null, receipt.getId()));

        assertThat(saved.getReceiptId()).isEqualTo(receipt.getId());
        assertThat(receipt.getExpenseId()).isEqualTo(saved.getId());
    }

    @Test
    @DisplayName("create — paragon już powiązany z innym wydatkiem rzuca 409")
    void create_rejects_receipt_already_linked_to_another_expense() {
        ExpenseCategory category = stubCategory();
        Receipt receipt = stubReceipt(userId, /* already linked */ UUID.randomUUID());

        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(receiptRepository.findByIdAndUserId(receipt.getId(), userId)).thenReturn(Optional.of(receipt));

        assertThatThrownBy(() -> service.create(userId, new CreateExpenseRequest(
                category.getId(), new BigDecimal("10"), "PLN",
                LocalDate.now(), "X", null, false, null, receipt.getId())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("już powiązany");

        verify(expenseRepository, never()).save(any());
    }

    @Test
    @DisplayName("create — nieznana kategoria → 400")
    void create_rejects_unknown_category() {
        UUID missing = UUID.randomUUID();
        when(categoryRepository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(userId, new CreateExpenseRequest(
                missing, new BigDecimal("1"), "PLN",
                LocalDate.now(), "x", null, false, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Unknown category");
    }

    @Test
    @DisplayName("update — zmienia kategorię i pola")
    void update_replaces_fields_and_category() {
        ExpenseCategory oldCat = stubCategory();
        ExpenseCategory newCat = stubCategory();
        Expense expense = Expense.create(userId, oldCat,
                new BigDecimal("100"), "PLN", LocalDate.now(), "Old");

        when(expenseRepository.findById(expense.getId())).thenReturn(Optional.of(expense));
        when(categoryRepository.findById(newCat.getId())).thenReturn(Optional.of(newCat));

        Expense updated = service.update(userId, expense.getId(), new UpdateExpenseRequest(
                newCat.getId(), new BigDecimal("250"), "EUR",
                LocalDate.of(2026, 1, 5), "New", "Vendor",
                true, new BigDecimal("46.75")));

        assertThat(updated.getCategory()).isSameAs(newCat);
        assertThat(updated.getAmount()).isEqualByComparingTo("250");
        assertThat(updated.getCurrency()).isEqualTo("EUR");
        assertThat(updated.isVatDeductible()).isTrue();
        assertThat(updated.getVatAmount()).isEqualByComparingTo("46.75");
    }

    @Test
    @DisplayName("update — cudzy wydatek udaje 404 (nie wycieka istnienia)")
    void update_other_users_expense_returns_404() {
        ExpenseCategory cat = stubCategory();
        Expense expense = Expense.create(otherUserId, cat,
                new BigDecimal("1"), "PLN", LocalDate.now(), "x");

        when(expenseRepository.findById(expense.getId())).thenReturn(Optional.of(expense));

        assertThatThrownBy(() -> service.update(userId, expense.getId(), new UpdateExpenseRequest(
                cat.getId(), new BigDecimal("2"), "PLN",
                LocalDate.now(), "y", null, false, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("delete — cudzy wydatek udaje 404")
    void delete_other_users_expense_returns_404() {
        ExpenseCategory cat = stubCategory();
        Expense expense = Expense.create(otherUserId, cat,
                new BigDecimal("1"), "PLN", LocalDate.now(), "x");
        when(expenseRepository.findById(expense.getId())).thenReturn(Optional.of(expense));

        assertThatThrownBy(() -> service.delete(userId, expense.getId()))
                .isInstanceOf(BusinessException.class);
        verify(expenseRepository, never()).delete(any(Expense.class));
    }

    @Test
    @DisplayName("create — pusta waluta normalizowana do PLN")
    void create_normalizes_blank_currency_to_PLN() {
        ExpenseCategory cat = stubCategory();
        when(categoryRepository.findById(cat.getId())).thenReturn(Optional.of(cat));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));

        Expense saved = service.create(userId, new CreateExpenseRequest(
                cat.getId(), new BigDecimal("5"), null,
                LocalDate.now(), "x", null, false, null, null));

        assertThat(saved.getCurrency()).isEqualTo("PLN");
    }

    private static ExpenseCategory stubCategory() {
        ExpenseCategory cat = newInstance(ExpenseCategory.class);
        setField(cat, "id", UUID.randomUUID());
        setField(cat, "code", "OTHER");
        setField(cat, "name", "Inne");
        setField(cat, "deductible", true);
        return cat;
    }

    private static Receipt stubReceipt(UUID owner, UUID expenseId) {
        Receipt r = Receipt.pending(owner, "key/" + UUID.randomUUID(), "p.jpg",
                "image/jpeg", 1024L);
        if (expenseId != null) r.linkExpense(expenseId);
        return r;
    }

    private static <T> T newInstance(Class<T> type) {
        try {
            var c = type.getDeclaredConstructor();
            c.setAccessible(true);
            return c.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
