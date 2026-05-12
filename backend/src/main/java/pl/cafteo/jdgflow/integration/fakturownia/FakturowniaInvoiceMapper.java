package pl.cafteo.jdgflow.integration.fakturownia;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.cafteo.jdgflow.invoice.domain.ExternalInvoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FakturowniaInvoiceMapper {

    private final FakturowniaStatusMapper statusMapper;
    private final FakturowniaKindMapper kindMapper;

    public ExternalInvoice toExternal(FakturowniaInvoiceDto dto) {
        BigDecimal net = parseAmount(dto.priceNet());
        BigDecimal gross = parseAmount(firstNonBlank(dto.totalPriceGross(), dto.priceGross()));
        BigDecimal vat = gross.subtract(net);

        return new ExternalInvoice(
                dto.id(),
                dto.number(),
                kindMapper.toDomain(dto.kind()),
                statusMapper.toDomain(dto.status()),
                net,
                vat,
                gross,
                Optional.ofNullable(dto.currency()).orElse("PLN").toUpperCase(),
                parseNullableAmount(dto.exchangeRate()),
                parseDate(dto.issueDate()),
                Optional.ofNullable(parseDate(dto.sellDate())).orElseGet(() -> parseDate(dto.issueDate())),
                parseDate(dto.paymentDate()),
                parseDate(dto.paidDate()),
                dto.buyerName(),
                dto.buyerTaxNo(),
                dto.buyerEmail()
        );
    }

    private static BigDecimal parseAmount(String raw) {
        if (raw == null || raw.isBlank()) return BigDecimal.ZERO;
        return new BigDecimal(raw.replace(",", ".").replace(" ", ""));
    }

    private static BigDecimal parseNullableAmount(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return parseAmount(raw);
    }

    private static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return LocalDate.parse(raw);
    }

    private static String firstNonBlank(String a, String b) {
        return (a != null && !a.isBlank()) ? a : b;
    }
}
