package pl.cafteo.jdgflow.ocr.service;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.cafteo.jdgflow.ocr.config.MinioProperties;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptStorage {

    private final MinioClient minioClient;
    private final MinioProperties properties;

    public String upload(UUID userId, byte[] data, String originalFilename, String contentType) {
        String key = buildKey(userId, originalFilename);
        try (InputStream in = new ByteArrayInputStream(data)) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(key)
                    .stream(in, data.length, -1)
                    .contentType(contentType)
                    .build());
            log.info("Uploaded receipt to s3://{}/{} ({} bytes)", properties.bucket(), key, data.length);
            return key;
        } catch (Exception e) {
            throw new ReceiptStorageException("Upload do MinIO nie powiódł się: " + e.getMessage(), e);
        }
    }

    public byte[] download(String key) {
        try (InputStream in = minioClient.getObject(GetObjectArgs.builder()
                .bucket(properties.bucket())
                .object(key)
                .build())) {
            return in.readAllBytes();
        } catch (Exception e) {
            throw new ReceiptStorageException("Nie udało się pobrać obrazu z MinIO: " + e.getMessage(), e);
        }
    }

    public void delete(String key) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(key)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to delete s3://{}/{}: {}", properties.bucket(), key, e.getMessage());
        }
    }

    private static String buildKey(UUID userId, String filename) {
        String safeName = (filename == null || filename.isBlank())
                ? "receipt"
                : filename.replaceAll("[^A-Za-z0-9._-]", "_");
        String monthFolder = LocalDate.now().toString().substring(0, 7); // yyyy-MM
        return "%s/%s/%s-%s".formatted(userId, monthFolder, UUID.randomUUID(), safeName);
    }

    public static class ReceiptStorageException extends RuntimeException {
        public ReceiptStorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
