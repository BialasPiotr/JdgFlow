package pl.cafteo.jdgflow.expense;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import pl.cafteo.jdgflow.AbstractIntegrationTest;
import pl.cafteo.jdgflow.auth.api.dto.AuthResponse;
import pl.cafteo.jdgflow.auth.api.dto.RegisterRequest;
import pl.cafteo.jdgflow.auth.domain.UserRepository;
import pl.cafteo.jdgflow.expense.api.dto.CategoryResponse;
import pl.cafteo.jdgflow.expense.api.dto.CreateExpenseRequest;
import pl.cafteo.jdgflow.expense.api.dto.ExpenseResponse;
import pl.cafteo.jdgflow.expense.api.dto.ExpenseSummaryResponse;
import pl.cafteo.jdgflow.expense.api.dto.UpdateExpenseRequest;
import pl.cafteo.jdgflow.expense.domain.ExpenseRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExpenseFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired TestRestTemplate rest;
    @Autowired UserRepository userRepository;
    @Autowired ExpenseRepository expenseRepository;

    private String token;
    private UUID softwareCategoryId;
    private UUID hardwareCategoryId;

    @BeforeEach
    void setUp() {
        expenseRepository.deleteAll();
        userRepository.deleteAll();

        ResponseEntity<AuthResponse> auth = rest.postForEntity(
                "/api/auth/register",
                new RegisterRequest("expenses-test@example.com", "secret-password-1234", "Tester"),
                AuthResponse.class);
        token = auth.getBody().token();

        ResponseEntity<List<CategoryResponse>> cats = rest.exchange(
                "/api/expense-categories",
                HttpMethod.GET,
                authorized(),
                new ParameterizedTypeReference<>() {});
        softwareCategoryId = cats.getBody().stream().filter(c -> c.code().equals("SOFTWARE")).findFirst().orElseThrow().id();
        hardwareCategoryId = cats.getBody().stream().filter(c -> c.code().equals("HARDWARE")).findFirst().orElseThrow().id();
    }

    @Test
    void seeded_categories_are_available() {
        ResponseEntity<List<CategoryResponse>> response = rest.exchange(
                "/api/expense-categories", HttpMethod.GET, authorized(),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).extracting(CategoryResponse::code)
                .contains("SOFTWARE", "HARDWARE", "OFFICE", "FUEL", "OTHER");
    }

    @Test
    void creates_expense_and_lists_it() {
        CreateExpenseRequest request = new CreateExpenseRequest(
                softwareCategoryId,
                new BigDecimal("99.00"),
                "PLN",
                LocalDate.of(2026, 4, 15),
                "Claude Pro subscription",
                "Anthropic",
                true,
                new BigDecimal("18.51"),
                null);

        ResponseEntity<ExpenseResponse> created = rest.exchange(
                "/api/expenses", HttpMethod.POST, authorizedJson(request), ExpenseResponse.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().description()).isEqualTo("Claude Pro subscription");
        assertThat(created.getBody().vatDeductible()).isTrue();
        assertThat(created.getBody().vatAmount()).isEqualByComparingTo("18.51");

        ResponseEntity<JsonNode> list = rest.exchange(
                "/api/expenses", HttpMethod.GET, authorized(), JsonNode.class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(list.getBody().get("totalElements").asInt()).isEqualTo(1);
    }

    @Test
    void rejects_non_positive_amount() {
        CreateExpenseRequest request = new CreateExpenseRequest(
                softwareCategoryId,
                new BigDecimal("0.00"),
                "PLN",
                LocalDate.of(2026, 4, 15),
                "Test",
                null, false, null, null);

        ResponseEntity<JsonNode> response = rest.exchange(
                "/api/expenses", HttpMethod.POST, authorizedJson(request), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejects_future_expense_date() {
        CreateExpenseRequest request = new CreateExpenseRequest(
                softwareCategoryId,
                new BigDecimal("100"),
                "PLN",
                LocalDate.now().plusDays(5),
                "Test",
                null, false, null, null);

        ResponseEntity<JsonNode> response = rest.exchange(
                "/api/expenses", HttpMethod.POST, authorizedJson(request), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updates_existing_expense() {
        ExpenseResponse created = createExpense(softwareCategoryId, "100.00", LocalDate.of(2026, 3, 10), "X", null);

        UpdateExpenseRequest update = new UpdateExpenseRequest(
                hardwareCategoryId, new BigDecimal("250.00"), "PLN",
                LocalDate.of(2026, 3, 12), "Klawiatura mechaniczna", "x-kom",
                true, new BigDecimal("46.75"));

        ResponseEntity<ExpenseResponse> response = rest.exchange(
                "/api/expenses/" + created.id(), HttpMethod.PUT,
                authorizedJson(update), ExpenseResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().amount()).isEqualByComparingTo("250.00");
        assertThat(response.getBody().description()).isEqualTo("Klawiatura mechaniczna");
        assertThat(response.getBody().category().code()).isEqualTo("HARDWARE");
    }

    @Test
    void deletes_expense() {
        ExpenseResponse created = createExpense(softwareCategoryId, "10.00", LocalDate.of(2026, 1, 1), "X", null);

        ResponseEntity<Void> response = rest.exchange(
                "/api/expenses/" + created.id(), HttpMethod.DELETE, authorized(), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(expenseRepository.count()).isZero();
    }

    @Test
    void filters_by_category() {
        createExpense(softwareCategoryId, "10.00", LocalDate.of(2026, 1, 5), "Cloud", null);
        createExpense(hardwareCategoryId, "1500.00", LocalDate.of(2026, 1, 10), "Monitor", null);

        ResponseEntity<JsonNode> response = rest.exchange(
                "/api/expenses?categoryId=" + softwareCategoryId, HttpMethod.GET,
                authorized(), JsonNode.class);

        assertThat(response.getBody().get("totalElements").asInt()).isEqualTo(1);
        assertThat(response.getBody().get("content").get(0).get("description").asText()).isEqualTo("Cloud");
    }

    @Test
    void summary_aggregates_by_category() {
        createExpense(softwareCategoryId, "100.00", LocalDate.of(2026, 4, 5), "Subscription", null);
        createExpense(softwareCategoryId, "50.00", LocalDate.of(2026, 4, 10), "Plugin", null);
        createExpense(hardwareCategoryId, "2000.00", LocalDate.of(2026, 4, 15), "Monitor", null);
        createExpense(hardwareCategoryId, "999.00", LocalDate.of(2026, 5, 20), "Keyboard", null);

        ResponseEntity<ExpenseSummaryResponse> response = rest.exchange(
                "/api/expenses/summary?from=2026-04-01&to=2026-04-30", HttpMethod.GET,
                authorized(), ExpenseSummaryResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var body = response.getBody();
        assertThat(body.total()).isEqualByComparingTo("2150.00");
        assertThat(body.count()).isEqualTo(3);
        assertThat(body.byCategory()).hasSize(2);
        // Sorted by total DESC — Hardware first
        assertThat(body.byCategory().get(0).category().code()).isEqualTo("HARDWARE");
        assertThat(body.byCategory().get(0).total()).isEqualByComparingTo("2000.00");
        assertThat(body.byCategory().get(1).category().code()).isEqualTo("SOFTWARE");
        assertThat(body.byCategory().get(1).total()).isEqualByComparingTo("150.00");
    }

    private ExpenseResponse createExpense(UUID categoryId, String amount, LocalDate date,
                                          String description, String vendor) {
        CreateExpenseRequest req = new CreateExpenseRequest(
                categoryId, new BigDecimal(amount), "PLN", date, description, vendor, false, null, null);
        return rest.exchange("/api/expenses", HttpMethod.POST,
                authorizedJson(req), ExpenseResponse.class).getBody();
    }

    private HttpEntity<Void> authorized() {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        return new HttpEntity<>(h);
    }

    private <T> HttpEntity<T> authorizedJson(T body) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, h);
    }
}
