package github.centralizedSwaggerDocumentation.configurations;

import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@RequiredArgsConstructor
public class SwaggerUrlsConfig {

    private final ApplicationConfig aConfig;

    @Value("${server.port}")
    private int serverPort;

    @Value("${app.local-addr}")
    private String localAddr;

    @Bean
    @Primary // Required, otherwise there is an error of 2 Beans in the context
    @SneakyThrows
    public SwaggerUiConfigProperties swaggerUiConfigProperties() {
        SwaggerUiConfigProperties properties = new SwaggerUiConfigProperties();

        var swaggerUrls = aConfig.apiConfig().getEndpoints().entrySet().stream()
            .map((entry) ->
                createSwaggerUrl( // Swagger is already here on the Gateway service, it indicates the addresses not of our microservices, but of its proxy routes
                    "%s/v3/api-docs".formatted( // Here we specify the addresses for microservices in the Swagger documentation, but at the same time we write links not to certain microservices, but to our proxy routes
                        "http://" + localAddr + ":" + serverPort + "/api" + entry.getValue().getServicePath()),
                    entry.getKey()
                )
            ).collect(Collectors.toSet());

        properties.setUrls(swaggerUrls);

        return properties;
    }

    private AbstractSwaggerUiConfigProperties.SwaggerUrl createSwaggerUrl(String url, String name) {
        AbstractSwaggerUiConfigProperties.SwaggerUrl swaggerUrl = new AbstractSwaggerUiConfigProperties.SwaggerUrl();
        swaggerUrl.setUrl(url);
        swaggerUrl.setName(name);
        return swaggerUrl;
    }
}