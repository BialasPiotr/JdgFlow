package pl.cafteo.jdgflow.invoice;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import pl.cafteo.jdgflow.AbstractIntegrationTest;
import pl.cafteo.jdgflow.auth.api.dto.AuthResponse;
import pl.cafteo.jdgflow.auth.api.dto.RegisterRequest;
import pl.cafteo.jdgflow.auth.domain.UserRepository;
import pl.cafteo.jdgflow.invoice.domain.InvoiceKind;
import pl.cafteo.jdgflow.invoice.domain.InvoiceRepository;
import pl.cafteo.jdgflow.invoice.domain.InvoiceStatus;
import pl.cafteo.jdgflow.invoice.service.InvoiceSyncService;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

class FakturowniaSyncIntegrationTest extends AbstractIntegrationTest {

    static WireMockServer wireMock;

    @Autowired TestRestTemplate rest;
    @Autowired InvoiceRepository invoiceRepository;
    @Autowired UserRepository userRepository;

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMock != null) wireMock.stop();
    }

    @DynamicPropertySource
    static void overrideFakturownia(DynamicPropertyRegistry registry) {
        registry.add("jdgflow.integration.fakturownia.base-url", () -> wireMock.baseUrl());
        registry.add("jdgflow.integration.fakturownia.api-token", () -> "test-token-abc");
        registry.add("jdgflow.integration.fakturownia.default-page-size", () -> "50");
    }

    @BeforeEach
    void cleanState() {
        invoiceRepository.deleteAll();
        userRepository.deleteAll();
        wireMock.resetAll();
    }

    @Test
    void syncs_invoices_from_fakturownia_and_persists_them() {
        stubInvoicesPage(1, """
                [
                  {
                    "id": 12345,
                    "number": "FV/2026/04/01",
                    "kind": "vat",
                    "status": "paid",
                    "price_net": "1000,00",
                    "price_gross": "1230,00",
                    "total_price_gross": "1230,00",
                    "currency": "PLN",
                    "issue_date": "2026-04-15",
                    "sell_date": "2026-04-15",
                    "payment_date": "2026-04-29",
                    "paid_date": "2026-04-22",
                    "buyer_name": "Acme Sp. z o.o.",
                    "buyer_tax_no": "5252111111",
                    "buyer_email": "billing@acme.test"
                  },
                  {
                    "id": 12346,
                    "number": "FV/2026/04/02",
                    "kind": "vat",
                    "status": "sent",
                    "price_net": "500.00",
                    "price_gross": "615.00",
                    "total_price_gross": "615.00",
                    "currency": "PLN",
                    "issue_date": "2026-04-20",
                    "sell_date": "2026-04-20",
                    "payment_date": "2026-05-04",
                    "paid_date": null,
                    "buyer_name": "Foo LLC",
                    "buyer_tax_no": null,
                    "buyer_email": "ar@foo.test"
                  }
                ]
                """);
        stubInvoicesPage(2, "[]");

        String token = registerAndGetToken();

        ResponseEntity<InvoiceSyncService.SyncResult> syncResponse = rest.exchange(
                "/api/invoices/sync", HttpMethod.POST, authorized(token), InvoiceSyncService.SyncResult.class);

        assertThat(syncResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(syncResponse.getBody().created()).isEqualTo(2);
        assertThat(syncResponse.getBody().updated()).isEqualTo(0);

        var invoices = invoiceRepository.findAll();
        assertThat(invoices).hasSize(2);

        var paid = invoices.stream().filter(i -> i.getFakturowniaId() == 12345L).findFirst().orElseThrow();
        assertThat(paid.getInvoiceNumber()).isEqualTo("FV/2026/04/01");
        assertThat(paid.getKind()).isEqualTo(InvoiceKind.VAT);
        assertThat(paid.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(paid.getNetAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(paid.getVatAmount()).isEqualByComparingTo(new BigDecimal("230.00"));
        assertThat(paid.getGrossAmount()).isEqualByComparingTo(new BigDecimal("1230.00"));
        assertThat(paid.getCurrency()).isEqualTo("PLN");
        assertThat(paid.getIssueDate()).isEqualTo(LocalDate.of(2026, 4, 15));
        assertThat(paid.getPaidDate()).isEqualTo(LocalDate.of(2026, 4, 22));
        assertThat(paid.getBuyerName()).isEqualTo("Acme Sp. z o.o.");
        assertThat(paid.getBuyerNip()).isEqualTo("5252111111");
    }

    @Test
    void second_sync_updates_existing_invoices_without_duplicating_them() {
        stubInvoicesPage(1, """
                [
                  {
                    "id": 99,
                    "number": "FV/2026/01/01",
                    "kind": "vat",
                    "status": "sent",
                    "price_net": "200.00",
                    "total_price_gross": "246.00",
                    "currency": "PLN",
                    "issue_date": "2026-01-10",
                    "sell_date": "2026-01-10"
                  }
                ]
                """);
        stubInvoicesPage(2, "[]");

        String token = registerAndGetToken();

        rest.exchange("/api/invoices/sync", HttpMethod.POST, authorized(token), InvoiceSyncService.SyncResult.class);

        wireMock.resetAll();
        stubInvoicesPage(1, """
                [
                  {
                    "id": 99,
                    "number": "FV/2026/01/01",
                    "kind": "vat",
                    "status": "paid",
                    "price_net": "200.00",
                    "total_price_gross": "246.00",
                    "currency": "PLN",
                    "issue_date": "2026-01-10",
                    "sell_date": "2026-01-10",
                    "payment_date": "2026-01-24",
                    "paid_date": "2026-01-22"
                  }
                ]
                """);
        stubInvoicesPage(2, "[]");

        ResponseEntity<InvoiceSyncService.SyncResult> resync = rest.exchange(
                "/api/invoices/sync", HttpMethod.POST, authorized(token), InvoiceSyncService.SyncResult.class);

        assertThat(resync.getBody().created()).isEqualTo(0);
        assertThat(resync.getBody().updated()).isEqualTo(1);

        var stored = invoiceRepository.findAll();
        assertThat(stored).hasSize(1);
        assertThat(stored.get(0).getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(stored.get(0).getPaidDate()).isEqualTo(LocalDate.of(2026, 1, 22));
    }

    @Test
    void list_endpoint_returns_synced_invoices_with_pagination_metadata() {
        stubInvoicesPage(1, """
                [
                  {"id":1,"number":"A","kind":"vat","status":"paid","price_net":"100","total_price_gross":"123","currency":"PLN","issue_date":"2026-03-01","sell_date":"2026-03-01"},
                  {"id":2,"number":"B","kind":"vat","status":"sent","price_net":"200","total_price_gross":"246","currency":"PLN","issue_date":"2026-03-15","sell_date":"2026-03-15"}
                ]
                """);
        stubInvoicesPage(2, "[]");

        String token = registerAndGetToken();
        rest.exchange("/api/invoices/sync", HttpMethod.POST, authorized(token), InvoiceSyncService.SyncResult.class);

        ResponseEntity<String> listResponse = rest.exchange(
                "/api/invoices?status=PAID", HttpMethod.GET, authorized(token), String.class);

        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody()).contains("\"totalElements\":1");
        assertThat(listResponse.getBody()).contains("\"number\":\"A\"");
        assertThat(listResponse.getBody()).doesNotContain("\"number\":\"B\"");
    }

    private void stubInvoicesPage(int page, String body) {
        wireMock.stubFor(get(urlPathEqualTo("/invoices.json"))
                .withQueryParam("api_token", equalTo("test-token-abc"))
                .withQueryParam("page", equalTo(String.valueOf(page)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));
    }

    private String registerAndGetToken() {
        ResponseEntity<AuthResponse> response = rest.postForEntity(
                "/api/auth/register",
                new RegisterRequest("piotr@example.com", "test-password-1234", "Piotr Białas"),
                AuthResponse.class);
        return response.getBody().token();
    }

    private HttpEntity<Void> authorized(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(headers);
    }
}
