package pl.cafteo.jdgflow.ocr.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {

    @Bean
    public MinioClient minioClient(MinioProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.endpoint())
                .credentials(properties.accessKey(), properties.secretKey())
                .build();
    }

    @Bean
    public ApplicationListener<ApplicationReadyEvent> minioBucketInitializer(
            MinioClient minioClient, MinioProperties properties) {
        return event -> {
            try {
                boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                        .bucket(properties.bucket()).build());
                if (!exists) {
                    minioClient.makeBucket(MakeBucketArgs.builder()
                            .bucket(properties.bucket()).build());
                    log.info("Created MinIO bucket '{}'", properties.bucket());
                } else {
                    log.debug("MinIO bucket '{}' already exists", properties.bucket());
                }
            } catch (Exception e) {
                log.warn("Could not verify MinIO bucket '{}' on startup: {}",
                        properties.bucket(), e.getMessage());
            }
        };
    }
}
