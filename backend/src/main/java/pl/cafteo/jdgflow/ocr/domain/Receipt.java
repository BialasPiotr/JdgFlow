package pl.cafteo.jdgflow.ocr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pl.cafteo.jdgflow.common.audit.AuditableEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Table(name = "receipts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Receipt extends AuditableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "s3_key", nullable = false, length = 512, unique = true)
    private String s3Key;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "mime_type", nullable = false, length = 64)
    private String mimeType;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ReceiptStatus status;

    @Column(name = "ocr_result", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String ocrResult;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "expense_id")
    private UUID expenseId;

    @Column(name = "processed_at")
    private Instant processedAt;

    public static Receipt pending(UUID userId, String s3Key, String originalFilename,
                                  String mimeType, long fileSizeBytes) {
        Receipt r = new Receipt();
        r.id = UUID.randomUUID();
        r.userId = userId;
        r.s3Key = s3Key;
        r.originalFilename = originalFilename;
        r.mimeType = mimeType;
        r.fileSizeBytes = fileSizeBytes;
        r.status = ReceiptStatus.PENDING;
        return r;
    }

    public void markProcessed(String ocrResultJson) {
        this.status = ReceiptStatus.PROCESSED;
        this.ocrResult = ocrResultJson;
        this.errorMessage = null;
        this.processedAt = Instant.now();
    }

    public void markFailed(String errorMessage) {
        this.status = ReceiptStatus.FAILED;
        this.errorMessage = errorMessage;
        this.processedAt = Instant.now();
    }

    public void linkExpense(UUID expenseId) {
        this.expenseId = expenseId;
    }

    public void unlinkExpense() {
        this.expenseId = null;
    }
}
