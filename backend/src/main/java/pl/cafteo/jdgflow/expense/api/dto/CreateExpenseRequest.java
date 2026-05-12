package pl.cafteo.jdgflow.expense.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateExpenseRequest(
        @NotNull UUID categoryId,

        @NotNull
        @DecimalMin(value = "0.01", message = "Kwota musi być większa od 0")
        @Digits(integer = 13, fraction = 2)
        BigDecimal amount,

        @Pattern(regexp = "^[A-Z]{3}$", message = "Waluta to 3-literowy kod ISO (np. PLN)")
        String currency,

        @NotNull @PastOrPresent LocalDate expenseDate,

        @NotBlank @Size(max = 500) String description,

        @Size(max = 255) String vendor,

        boolean vatDeductible,

        @DecimalMin(value = "0.00")
        @Digits(integer = 13, fraction = 2)
        BigDecimal vatAmount,

        UUID receiptId
) {}
