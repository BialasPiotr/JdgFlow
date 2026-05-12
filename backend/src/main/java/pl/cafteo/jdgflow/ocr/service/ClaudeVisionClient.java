package pl.cafteo.jdgflow.ocr.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pl.cafteo.jdgflow.common.exception.BusinessException;
import pl.cafteo.jdgflow.ocr.config.ClaudeApiProperties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ClaudeVisionClient {

    private static final String TOOL_NAME = "extract_expense";

    private static final List<String> CATEGORY_CODES = List.of(
            "SOFTWARE", "HARDWARE", "OFFICE", "TELECOM", "FUEL",
            "TRAVEL", "TRAINING", "HOSTING", "OTHER"
    );

    private static final String SYSTEM_PROMPT = """
            Jesteś asystentem księgowym jednoosobowej działalności gospodarczej w Polsce.
            Otrzymasz zdjęcie lub PDF paragonu albo faktury kosztowej. Twoim zadaniem jest
            wyodrębnić strukturalne pola wydatku i wywołać narzędzie `extract_expense`.

            Reguły:
            - `amount` to kwota brutto (do zapłaty) w walucie z dokumentu.
            - `currency` to kod ISO 4217, domyślnie PLN gdy nie ma jawnego oznaczenia.
            - `vatAmount` to suma VAT z dokumentu (NIE doliczaj sam — tylko jeśli widoczne).
            - `vatDeductible` ustaw na true gdy paragon/faktura ma NIP nabywcy oraz wyodrębniony VAT.
              W innym wypadku false. Jeśli nie potrafisz tego ocenić — pomiń pole.
            - `expenseDate` to data wystawienia/sprzedaży (ISO yyyy-MM-dd).
            - `vendor` to nazwa sprzedawcy (firma na górze paragonu/faktury).
            - `description` krótki opis (1 zdanie po polsku) co to za wydatek.
            - `suggestedCategoryCode` MUSI być jednym z: SOFTWARE, HARDWARE, OFFICE, TELECOM,
              FUEL, TRAVEL, TRAINING, HOSTING, OTHER. Wybierz najbardziej prawdopodobną.
            - `confidence` 0.0–1.0 — Twoja pewność co do całości ekstrakcji.

            Jeśli któreś pole jest nieczytelne lub niepewne — pomiń je. Lepiej zostawić puste
            niż zgadywać. Użytkownik uzupełni resztę w formularzu.
            """;

    private final RestClient restClient;
    private final ClaudeApiProperties properties;
    private final ObjectMapper objectMapper;

    public ClaudeVisionClient(RestClient claudeHttpClient,
                              ClaudeApiProperties properties,
                              ObjectMapper objectMapper) {
        this.restClient = claudeHttpClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public ParsedReceipt extract(byte[] fileBytes, String mimeType) {
        if (!properties.isConfigured()) {
            throw BusinessException.badRequest("OCR niedostępne — skonfiguruj zmienną środowiskową ANTHROPIC_API_KEY");
        }
        if (fileBytes == null || fileBytes.length == 0) {
            throw BusinessException.badRequest("Pusty plik — nie ma czego analizować");
        }

        String base64 = Base64.getEncoder().encodeToString(fileBytes);
        Map<String, Object> body = buildRequestBody(base64, mimeType);

        JsonNode response;
        try {
            response = restClient.post()
                    .uri("/v1/messages")
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException ex) {
            log.warn("Claude API call failed: {}", ex.getMessage());
            throw BusinessException.badRequest("OCR niedostępne — błąd komunikacji z API: " + ex.getMessage());
        }

        if (response == null) {
            throw BusinessException.badRequest("OCR niedostępne — pusta odpowiedź z API");
        }
        return parseToolResult(response);
    }

    private Map<String, Object> buildRequestBody(String base64, String mimeType) {
        Map<String, Object> source = Map.of(
                "type", "base64",
                "media_type", mimeType,
                "data", base64
        );

        boolean isPdf = "application/pdf".equalsIgnoreCase(mimeType);
        Map<String, Object> mediaBlock = Map.of(
                "type", isPdf ? "document" : "image",
                "source", source
        );

        Map<String, Object> textBlock = Map.of(
                "type", "text",
                "text", "Wyodrębnij dane wydatku z tego dokumentu i zwróć je przez narzędzie extract_expense."
        );

        Map<String, Object> userMessage = Map.of(
                "role", "user",
                "content", List.of(mediaBlock, textBlock)
        );

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.model());
        body.put("max_tokens", 1024);
        body.put("system", SYSTEM_PROMPT);
        body.put("tools", List.of(toolDefinition()));
        body.put("tool_choice", Map.of("type", "tool", "name", TOOL_NAME));
        body.put("messages", List.of(userMessage));
        return body;
    }

    private Map<String, Object> toolDefinition() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("amount", Map.of(
                "type", "number",
                "description", "Kwota brutto wydatku w walucie dokumentu"
        ));
        properties.put("currency", Map.of(
                "type", "string",
                "description", "Kod waluty ISO 4217 (np. PLN, EUR, USD)"
        ));
        properties.put("vatAmount", Map.of(
                "type", "number",
                "description", "Wyodrębniona kwota VAT — pomiń jeśli niewidoczna"
        ));
        properties.put("vatDeductible", Map.of(
                "type", "boolean",
                "description", "Czy VAT jest odliczalny (paragon/faktura z NIP nabywcy)"
        ));
        properties.put("expenseDate", Map.of(
                "type", "string",
                "format", "date",
                "description", "Data wystawienia/sprzedaży w formacie yyyy-MM-dd"
        ));
        properties.put("vendor", Map.of(
                "type", "string",
                "description", "Nazwa sprzedawcy"
        ));
        properties.put("description", Map.of(
                "type", "string",
                "description", "Krótki opis wydatku po polsku (1 zdanie)"
        ));
        properties.put("suggestedCategoryCode", Map.of(
                "type", "string",
                "enum", CATEGORY_CODES,
                "description", "Sugerowana kategoria wydatku"
        ));
        properties.put("confidence", Map.of(
                "type", "number",
                "minimum", 0,
                "maximum", 1,
                "description", "Pewność ekstrakcji 0.0–1.0"
        ));

        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", properties,
                "required", List.of()
        );

        return Map.of(
                "name", TOOL_NAME,
                "description", "Zapisuje strukturalne dane wydatku wyodrębnione z paragonu lub faktury",
                "input_schema", schema
        );
    }

    private ParsedReceipt parseToolResult(JsonNode response) {
        JsonNode contentArray = response.path("content");
        if (!contentArray.isArray()) {
            log.warn("Claude response has no content array: {}", response);
            throw BusinessException.badRequest("OCR niedostępne — nieoczekiwana odpowiedź z API");
        }

        for (JsonNode block : contentArray) {
            if ("tool_use".equals(block.path("type").asText())
                    && TOOL_NAME.equals(block.path("name").asText())) {
                JsonNode input = block.path("input");
                return mapInput(input);
            }
        }

        log.warn("Claude response did not contain tool_use for {}: {}", TOOL_NAME, response);
        throw BusinessException.badRequest("OCR niedostępne — model nie zwrócił wymaganego narzędzia");
    }

    private ParsedReceipt mapInput(JsonNode input) {
        return new ParsedReceipt(
                bigDecimalOrNull(input, "amount"),
                stringOrNull(input, "currency"),
                bigDecimalOrNull(input, "vatAmount"),
                booleanOrNull(input, "vatDeductible"),
                localDateOrNull(input, "expenseDate"),
                stringOrNull(input, "vendor"),
                stringOrNull(input, "description"),
                stringOrNull(input, "suggestedCategoryCode"),
                doubleOrNull(input, "confidence")
        );
    }

    private static String stringOrNull(JsonNode node, String field) {
        JsonNode v = node.get(field);
        if (v == null || v.isNull() || !v.isTextual()) return null;
        String s = v.asText().trim();
        return s.isEmpty() ? null : s;
    }

    private static BigDecimal bigDecimalOrNull(JsonNode node, String field) {
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) return null;
        if (v.isNumber()) return v.decimalValue();
        if (v.isTextual()) {
            try {
                return new BigDecimal(v.asText().trim().replace(",", "."));
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    private static Boolean booleanOrNull(JsonNode node, String field) {
        JsonNode v = node.get(field);
        if (v == null || v.isNull() || !v.isBoolean()) return null;
        return v.asBoolean();
    }

    private static Double doubleOrNull(JsonNode node, String field) {
        JsonNode v = node.get(field);
        if (v == null || v.isNull() || !v.isNumber()) return null;
        return v.asDouble();
    }

    private static LocalDate localDateOrNull(JsonNode node, String field) {
        String raw = stringOrNull(node, field);
        if (raw == null) return null;
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
