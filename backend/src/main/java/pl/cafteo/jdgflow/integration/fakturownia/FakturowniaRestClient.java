package pl.cafteo.jdgflow.integration.fakturownia;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import pl.cafteo.jdgflow.common.exception.BusinessException;
import pl.cafteo.jdgflow.invoice.domain.ExternalInvoice;
import pl.cafteo.jdgflow.invoice.domain.FakturowniaClient;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class FakturowniaRestClient implements FakturowniaClient {

    private final RestClient restClient;
    private final FakturowniaProperties properties;
    private final FakturowniaInvoiceMapper invoiceMapper;

    public FakturowniaRestClient(RestClient fakturowniaHttpClient,
                                 FakturowniaProperties properties,
                                 FakturowniaInvoiceMapper invoiceMapper) {
        this.restClient = fakturowniaHttpClient;
        this.properties = properties;
        this.invoiceMapper = invoiceMapper;
    }

    @Override
    public List<ExternalInvoice> fetchInvoices(int page, int perPage) {
        try {
            List<FakturowniaInvoiceDto> response = restClient.get()
                    .uri(uri -> uri.path("/invoices.json")
                            .queryParam("api_token", properties.apiToken())
                            .queryParam("page", page)
                            .queryParam("per_page", perPage)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null || response.isEmpty()) {
                return List.of();
            }
            return response.stream().map(invoiceMapper::toExternal).toList();
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().value() == 401) {
                throw BusinessException.badGateway(
                        "Fakturownia odrzuciła żądanie (401) — sprawdź FAKTUROWNIA_API_TOKEN w ustawieniach integracji");
            }
            throw ex;
        }
    }

    @Override
    public Optional<byte[]> fetchInvoicePdf(long fakturowniaId) {
        try {
            byte[] pdf = restClient.get()
                    .uri(uri -> uri.path("/invoices/{id}.pdf")
                            .queryParam("api_token", properties.apiToken())
                            .build(fakturowniaId))
                    .retrieve()
                    .body(byte[].class);
            return Optional.ofNullable(pdf);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().value() == 401) {
                throw BusinessException.badGateway(
                        "Fakturownia odrzuciła żądanie (401) — sprawdź FAKTUROWNIA_API_TOKEN w ustawieniach integracji");
            }
            if (ex.getStatusCode().equals(HttpStatusCode.valueOf(404))) {
                return Optional.empty();
            }
            log.warn("Failed to fetch PDF for Fakturownia invoice {}: {}", fakturowniaId, ex.getMessage());
            throw ex;
        }
    }
}
