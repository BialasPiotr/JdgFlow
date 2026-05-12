package pl.cafteo.jdgflow.invoice.service;

import org.springframework.stereotype.Component;
import pl.cafteo.jdgflow.common.csv.CsvWriter;
import pl.cafteo.jdgflow.invoice.domain.Invoice;

import java.util.List;

@Component
public class InvoiceCsvExporter {

    public byte[] toCsv(List<Invoice> invoices) {
        CsvWriter w = new CsvWriter()
                .row()
                .cell("Numer")
                .cell("Rodzaj")
                .cell("Status")
                .cell("Data wystawienia")
                .cell("Data sprzedaży")
                .cell("Termin płatności")
                .cell("Data zapłaty")
                .cell("Nabywca")
                .cell("NIP nabywcy")
                .cell("Netto")
                .cell("VAT")
                .cell("Brutto")
                .cell("Waluta");

        for (Invoice inv : invoices) {
            w.row()
                    .cell(inv.getInvoiceNumber())
                    .cell(inv.getKind())
                    .cell(inv.getStatus())
                    .cell(inv.getIssueDate())
                    .cell(inv.getSaleDate())
                    .cell(inv.getPaymentDate())
                    .cell(inv.getPaidDate())
                    .cell(inv.getBuyerName())
                    .cell(inv.getBuyerNip())
                    .cell(inv.getNetAmount())
                    .cell(inv.getVatAmount())
                    .cell(inv.getGrossAmount())
                    .cell(inv.getCurrency());
        }

        return w.toBytesWithBom();
    }
}
