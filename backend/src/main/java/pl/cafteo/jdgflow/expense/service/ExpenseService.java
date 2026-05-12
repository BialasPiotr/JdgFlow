package pl.cafteo.jdgflow.expense.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.cafteo.jdgflow.common.exception.BusinessException;
import pl.cafteo.jdgflow.expense.api.dto.CreateExpenseRequest;
import pl.cafteo.jdgflow.expense.api.dto.UpdateExpenseRequest;
import pl.cafteo.jdgflow.expense.domain.Expense;
import pl.cafteo.jdgflow.expense.domain.ExpenseCategory;
import pl.cafteo.jdgflow.expense.domain.ExpenseCategoryRepository;
import pl.cafteo.jdgflow.expense.domain.ExpenseRepository;
import pl.cafteo.jdgflow.ocr.domain.Receipt;
import pl.cafteo.jdgflow.ocr.domain.ReceiptRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseCategoryRepository categoryRepository;
    private final ReceiptRepository receiptRepository;

    @Transactional
    public Expense create(UUID userId, CreateExpenseRequest request) {
        ExpenseCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> BusinessException.badRequest("Unknown category"));

        Receipt receipt = resolveReceiptForLink(userId, request.receiptId());

        Expense expense = Expense.create(
                userId,
                category,
                request.amount(),
                normalizeCurrency(request.currency()),
                request.expenseDate(),
                request.description());
        expense.setVendor(request.vendor());
        expense.applyVat(request.vatDeductible(), request.vatAmount());
        if (receipt != null) {
            expense.attachReceipt(receipt.getId());
        }

        Expense saved = expenseRepository.save(expense);
        if (receipt != null) {
            receipt.linkExpense(saved.getId());
        }
        return saved;
    }

    private Receipt resolveReceiptForLink(UUID userId, UUID receiptId) {
        if (receiptId == null) return null;
        Receipt receipt = receiptRepository.findByIdAndUserId(receiptId, userId)
                .orElseThrow(() -> BusinessException.notFound("Paragon nie istnieje"));
        if (receipt.getExpenseId() != null) {
            throw BusinessException.conflict("Paragon jest już powiązany z innym wydatkiem");
        }
        return receipt;
    }

    @Transactional
    public Expense update(UUID userId, UUID expenseId, UpdateExpenseRequest request) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> BusinessException.notFound("Expense not found"));
        if (!expense.getUserId().equals(userId)) {

            throw BusinessException.notFound("Expense not found");
        }

        ExpenseCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> BusinessException.badRequest("Unknown category"));

        expense.changeCategory(category);
        expense.updateDetails(
                request.amount(),
                normalizeCurrency(request.currency()),
                request.expenseDate(),
                request.description(),
                request.vendor());
        expense.applyVat(request.vatDeductible(), request.vatAmount());

        return expense;
    }

    @Transactional
    public void delete(UUID userId, UUID expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> BusinessException.notFound("Expense not found"));
        if (!expense.getUserId().equals(userId)) {
            throw BusinessException.notFound("Expense not found");
        }
        expenseRepository.delete(expense);
    }

    private static String normalizeCurrency(String currency) {
        return (currency == null || currency.isBlank()) ? "PLN" : currency.toUpperCase();
    }
}
