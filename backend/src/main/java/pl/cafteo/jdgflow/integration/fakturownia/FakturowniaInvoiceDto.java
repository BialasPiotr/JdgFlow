package pl.cafteo.jdgflow.integration.fakturownia;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FakturowniaInvoiceDto(
        Long id,
        String number,
        String kind,
        String status,
        @JsonProperty("price_net") String priceNet,
        @JsonProperty("price_gross") String priceGross,
        @JsonProperty("total_price_gross") String totalPriceGross,
        String currency,
        @JsonProperty("exchange_currency_rate") String exchangeRate,
        @JsonProperty("issue_date") String issueDate,
        @JsonProperty("sell_date") String sellDate,
        @JsonProperty("payment_date") String paymentDate,
        @JsonProperty("paid_date") String paidDate,
        @JsonProperty("buyer_name") String buyerName,
        @JsonProperty("buyer_tax_no") String buyerTaxNo,
        @JsonProperty("buyer_email") String buyerEmail
) {}
