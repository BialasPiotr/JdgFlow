package pl.cafteo.jdgflow.ocr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import pl.cafteo.jdgflow.common.exception.BusinessException;
import pl.cafteo.jdgflow.expense.domain.ExpenseCategory;
import pl.cafteo.jdgflow.expense.domain.ExpenseCategoryRepository;
import pl.cafteo.jdgflow.ocr.domain.Receipt;
import pl.cafteo.jdgflow.ocr.domain.ReceiptRepository;
import pl.cafteo.jdgflow.ocr.domain.ReceiptStatus;
import pl.cafteo.jdgflow.ocr.service.ClaudeVisionClient;
import pl.cafteo.jdgflow.ocr.service.ParsedReceipt;
import pl.cafteo.jdgflow.ocr.service.ReceiptOcrService;
import pl.cafteo.jdgflow.ocr.service.ReceiptStorage;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ReceiptOcrService} with all I/O collaborators mocked. Verifies the
 * upload → persist → call Claude → mark status flow, and how each failure mode is mapped to
 * a status (FAILED with message vs. 503 BusinessException for storage outages).
 */
@ExtendWith(MockitoExtension.class)
class ReceiptOcrServiceTest {

    @Mock ReceiptStorage receiptStorage;
    @Mock ReceiptRepository receiptRepository;
    @Mock ClaudeVisionClient claudeVisionClient;
    @Mock ExpenseCategoryRepository categoryRepository;

    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    ReceiptOcrService service;

    private final UUID userId = UUID.randomUUID();
    private MultipartFile validFile;

    @BeforeEach
    void setUp() {
        service = new ReceiptOcrService(receiptStorage, receiptRepository,
                claudeVisionClient, categoryRepository, objectMapper);
        validFile = new MockMultipartFile("file", "paragon.jpg", "image/jpeg",
                new byte[]{1, 2, 3, 4});
    }

    @Test
    @DisplayName("happy path — upload + OCR → status PROCESSED, breakdown w bazie, sugerowana kategoria zmapowana")
    void happy_path_marks_processed_and_maps_category() {
        when(receiptStorage.upload(eq(userId), any(), anyString(), eq("image/jpeg")))
                .thenReturn("user/2026-05/uuid-paragon.jpg");
        when(receiptRepository.save(any(Receipt.class))).thenAnswer(inv -> inv.getArgument(0));

        ParsedReceipt parsed = new ParsedReceipt(
                new BigDecimal("99.00"), "PLN", new BigDecimal("18.51"), true,
                LocalDate.of(2026, 5, 1), "Anthropic", "Claude Pro",
                "SOFTWARE", 0.92);
        when(claudeVisionClient.extract(any(byte[].class), eq("image/jpeg"))).thenReturn(parsed);

        ExpenseCategory software = stubCategory("SOFTWARE");
        when(categoryRepository.findAll()).thenReturn(List.of(software));

        ReceiptOcrService.OcrResult result = service.processUpload(userId, validFile);

        assertThat(result.receipt().getStatus()).isEqualTo(ReceiptStatus.PROCESSED);
        assertThat(result.receipt().getOcrResult()).contains("Anthropic");
        assertThat(result.parsed()).isEqualTo(parsed);
        assertThat(result.suggestedCategoryId()).isEqualTo(software.getId());
    }

    @Test
    @DisplayName("OCR padło z BusinessException — receipt zostaje oznaczony FAILED, plik w MinIO nie znika")
    void ocr_failure_marks_failed_and_keeps_storage_object() {
        when(receiptStorage.upload(any(), any(), any(), any()))
                .thenReturn("user/2026-05/uuid-paragon.jpg");
        when(receiptRepository.save(any(Receipt.class))).thenAnswer(inv -> inv.getArgument(0));
        when(claudeVisionClient.extract(any(byte[].class), anyString()))
                .thenThrow(BusinessException.badRequest("OCR niedostępne — nieoczekiwana odpowiedź"));

        ReceiptOcrService.OcrResult result = service.processUpload(userId, validFile);

        assertThat(result.receipt().getStatus()).isEqualTo(ReceiptStatus.FAILED);
        assertThat(result.receipt().getErrorMessage()).contains("OCR niedostępne");
        assertThat(result.parsed()).isNull();
        assertThat(result.suggestedCategoryId()).isNull();
        verify(receiptStorage, never()).delete(anyString());
    }

    @Test
    @DisplayName("nieobsługiwany MIME — 400, nic nie wysyła do MinIO")
    void rejects_unsupported_mime() {
        MultipartFile gif = new MockMultipartFile("file", "p.gif", "image/gif", new byte[]{1});

        assertThatThrownBy(() -> service.processUpload(userId, gif))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Nieobsługiwany format");

        verify(receiptStorage, never()).upload(any(), any(), any(), any());
        verify(receiptRepository, never()).save(any());
    }

    @Test
    @DisplayName("MinIO niedostępne — 503 BusinessException (nie 500)")
    void storage_outage_translates_to_503() {
        when(receiptStorage.upload(any(), any(), any(), any()))
                .thenThrow(new ReceiptStorage.ReceiptStorageException(
                        "Upload do MinIO nie powiódł się: connection refused", new RuntimeException()));

        assertThatThrownBy(() -> service.processUpload(userId, validFile))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

        verify(receiptRepository, never()).save(any());
        verify(claudeVisionClient, never()).extract(any(byte[].class), anyString());
    }

    @Test
    @DisplayName("pusty plik — 400, nic nie idzie dalej")
    void rejects_empty_file() {
        MultipartFile empty = new MockMultipartFile("file", "p.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> service.processUpload(userId, empty))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Brak pliku");
        verify(receiptStorage, never()).upload(any(), any(), any(), any());
    }

    @Test
    @DisplayName("MIME z parametrami (np. \"image/jpeg; charset=utf-8\") jest normalizowany")
    void normalizes_mime_with_parameters() {
        MultipartFile file = new MockMultipartFile("file", "p.jpg",
                "image/jpeg; charset=utf-8", new byte[]{1, 2});
        when(receiptStorage.upload(any(), any(), any(), eq("image/jpeg")))
                .thenReturn("key");
        when(receiptRepository.save(any(Receipt.class))).thenAnswer(inv -> inv.getArgument(0));
        when(claudeVisionClient.extract(any(byte[].class), eq("image/jpeg")))
                .thenReturn(new ParsedReceipt(null, null, null, null, null, null, null, null, null));

        service.processUpload(userId, file);

        verify(receiptStorage).upload(any(), any(), any(), eq("image/jpeg"));
        verify(claudeVisionClient).extract(any(byte[].class), eq("image/jpeg"));
    }

    private static ExpenseCategory stubCategory(String code) {
        ExpenseCategory cat = newInstance(ExpenseCategory.class);
        setField(cat, "id", UUID.randomUUID());
        setField(cat, "code", code);
        setField(cat, "name", code);
        setField(cat, "deductible", true);
        return cat;
    }

    private static <T> T newInstance(Class<T> type) {
        try {
            var c = type.getDeclaredConstructor();
            c.setAccessible(true);
            return c.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
