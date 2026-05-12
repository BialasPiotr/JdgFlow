package pl.cafteo.jdgflow.expense.service;

import org.springframework.stereotype.Component;
import pl.cafteo.jdgflow.common.csv.CsvWriter;
import pl.cafteo.jdgflow.expense.domain.Expense;

import java.util.List;

@Component
public class ExpenseCsvExporter {

    public byte[] toCsv(List<Expense> expenses) {
        CsvWriter w = new CsvWriter()
                .row()
                .cell("Data")
                .cell("Kategoria (kod)")
                .cell("Kategoria")
                .cell("Opis")
                .cell("Sprzedawca")
                .cell("Kwota")
                .cell("Waluta")
                .cell("VAT odliczalny")
                .cell("Kwota VAT");

        for (Expense e : expenses) {
            w.row()
                    .cell(e.getExpenseDate())
                    .cell(e.getCategory().getCode())
                    .cell(e.getCategory().getName())
                    .cell(e.getDescription())
                    .cell(e.getVendor())
                    .cell(e.getAmount())
                    .cell(e.getCurrency())
                    .cell(e.isVatDeductible())
                    .cell(e.getVatAmount());
        }

        return w.toBytesWithBom();
    }
}
