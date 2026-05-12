package pl.cafteo.jdgflow.ocr.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pl.cafteo.jdgflow.common.exception.BusinessException;
import pl.cafteo.jdgflow.expense.domain.ExpenseCategory;
import pl.cafteo.jdgflow.expense.domain.ExpenseCategoryRepository;
import pl.cafteo.jdgflow.ocr.domain.Receipt;
import pl.cafteo.jdgflow.ocr.domain.ReceiptRepository;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptOcrService {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "application/pdf"
    );

    private final ReceiptStorage receiptStorage;
    private final ReceiptRepository receiptRepository;
    private final ClaudeVisionClient claudeVisionClient;
    private final ExpenseCategoryRepository categoryRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public OcrResult processUpload(UUID userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("Brak pliku do uploadu");
        }
        String mimeType = normalizeMime(file.getContentType());
        if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw BusinessException.badRequest(
                    "Nieobsługiwany format pliku — dozwolone: JPEG, PNG, PDF");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw BusinessException.badRequest("Nie udało się odczytać pliku: " + e.getMessage());
        }

        String s3Key;
        try {
            s3Key = receiptStorage.upload(userId, bytes, file.getOriginalFilename(), mimeType);
        } catch (ReceiptStorage.ReceiptStorageException ex) {
            log.warn("Receipt storage upload failed for user {}: {}", userId, ex.getMessage());
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Storage paragonów niedostępny — sprawdź czy MinIO działa (docker compose up -d)");
        }

        Receipt receipt = Receipt.pending(userId, s3Key, file.getOriginalFilename(),
                mimeType, bytes.length);
        receiptRepository.save(receipt);

        ParsedReceipt parsed = null;
        try {
            parsed = claudeVisionClient.extract(bytes, mimeType);
            String json = objectMapper.writeValueAsString(parsed);
            receipt.markProcessed(json);
            log.info("OCR processed receipt {} (confidence={})", receipt.getId(), parsed.confidence());
        } catch (BusinessException ex) {
            receipt.markFailed(ex.getMessage());
            log.warn("OCR failed for receipt {}: {}", receipt.getId(), ex.getMessage());
        } catch (JsonProcessingException ex) {
            receipt.markFailed("Nie udało się zserializować wyniku OCR: " + ex.getMessage());
            log.warn("OCR result serialization failed for receipt {}: {}", receipt.getId(), ex.getMessage());
            parsed = null;
        } catch (Exception ex) {
            receipt.markFailed("Nieoczekiwany błąd OCR: " + ex.getMessage());
            log.warn("Unexpected OCR error for receipt {}: {}", receipt.getId(), ex.getMessage(), ex);
            parsed = null;
        }

        UUID suggestedCategoryId = parsed == null
                ? null
                : resolveCategoryId(parsed.suggestedCategoryCode());

        return new OcrResult(receipt, parsed, suggestedCategoryId);
    }

    private UUID resolveCategoryId(String code) {
        if (code == null || code.isBlank()) return null;
        return categoryRepository.findAll().stream()
                .filter(c -> code.equalsIgnoreCase(c.getCode()))
                .map(ExpenseCategory::getId)
                .findFirst()
                .orElse(null);
    }

    private static String normalizeMime(String contentType) {
        if (contentType == null) return "";
        int semi = contentType.indexOf(';');
        return (semi >= 0 ? contentType.substring(0, semi) : contentType).trim().toLowerCase();
    }

    public Receipt getOwnedReceipt(UUID userId, UUID receiptId) {
        return receiptRepository.findByIdAndUserId(receiptId, userId)
                .orElseThrow(() -> BusinessException.notFound("Paragon nie istnieje"));
    }

    public byte[] downloadFile(Receipt receipt) {
        return receiptStorage.download(receipt.getS3Key());
    }

    public record OcrResult(Receipt receipt, ParsedReceipt parsed, UUID suggestedCategoryId) {

        public Optional<ParsedReceipt> parsedOpt() {
            return Optional.ofNullable(parsed);
        }
    }
}
