package github.centralizedSwaggerDocumentation.configurations;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app", ignoreUnknownFields = false)
public record ApplicationConfig(
    ApiConfig apiConfig,
    String localAddr
) {

    @Data
    @Builder
    @RequiredArgsConstructor
    public static class ApiConfig {
        private final Map<String, Endpoint> endpoints;

        @Data
        @Builder
        @RequiredArgsConstructor
        public static class Endpoint {
            private final String gatewayPrefix;
            private final String serviceUrl;
            private final String servicePath;
        }
    }
}
