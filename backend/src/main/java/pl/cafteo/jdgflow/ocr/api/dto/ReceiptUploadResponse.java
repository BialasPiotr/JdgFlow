package pl.cafteo.jdgflow.ocr.api.dto;

import pl.cafteo.jdgflow.ocr.domain.ReceiptStatus;
import pl.cafteo.jdgflow.ocr.service.ReceiptOcrService;

import java.util.UUID;

public record ReceiptUploadResponse(
        UUID id,
        String s3Key,
        String originalFilename,
        String mimeType,
        long fileSizeBytes,
        ReceiptStatus status,
        String errorMessage,
        SuggestedExpense suggested
) {
    public static ReceiptUploadResponse from(ReceiptOcrService.OcrResult result) {
        var receipt = result.receipt();
        return new ReceiptUploadResponse(
                receipt.getId(),
                receipt.getS3Key(),
                receipt.getOriginalFilename(),
                receipt.getMimeType(),
                receipt.getFileSizeBytes(),
                receipt.getStatus(),
                receipt.getErrorMessage(),
                SuggestedExpense.from(result.parsed(), result.suggestedCategoryId())
        );
    }
}
