package pl.cafteo.jdgflow.ocr.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(ClaudeApiProperties.class)
public class ClaudeApiConfig {

    @Bean
    RestClient claudeHttpClient(ClaudeApiProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(properties.timeout() != null ? properties.timeout() : Duration.ofSeconds(60));

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader("x-api-key", properties.apiKey())
                .defaultHeader("User-Agent", "JdgFlow/0.1")
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
