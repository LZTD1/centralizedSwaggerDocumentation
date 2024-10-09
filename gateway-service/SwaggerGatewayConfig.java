package github.centralizedSwaggerDocumentation.configurations;

import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.Buildable;
import org.springframework.cloud.gateway.route.builder.PredicateSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
@RequiredArgsConstructor
public class SwaggerGatewayConfig {

    public static final String API_DOCS_POSTFIX = "/v3/api-docs"; // A constant for the JSON specification of the Swagger microservice
    private final ApplicationConfig applicationConfig; // Our config file

    @Value("${server.port}")
    private int serverPort;

    @Value("${app.local-addr}")
    private String localAddr;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        RouteLocatorBuilder.Builder routes = builder.routes();
        applicationConfig.apiConfig().getEndpoints().forEach((key, endpoint) -> { 
            routes.route(this.routeForJsonSpec(endpoint)); // Creating a router first for documentation
            routes.route(this.routeForDocumentation(endpoint)); // Then the router for endpoint
        }); // Order is important!
        return routes.build();
    }

    private Function<PredicateSpec, Buildable<Route>> routeForJsonSpec(
        ApplicationConfig.ApiConfig.Endpoint endpoint
    ) {
        String customUrl = "http://%s:%d/api".formatted(localAddr, serverPort); // We write the address of our Gateway, while it is worth noting that with the prefix / api

        // It is necessary to create a router through 
        // which you can contact and obtain documentation for a 
        // specific microservice

        return r -> r // Need route like http://inetAdress:8000/api/first/v3/api-docs -> http://inetAdress:microservicePort/v3/api-docs
            .path(endpoint.getGatewayPrefix() + endpoint.getServicePath() + API_DOCS_POSTFIX) // Here we get something like: /api/first/v3/api-docs
            .filters(f ->
                f.setPath(API_DOCS_POSTFIX) // Explicitly specifying the path /v3/api-docs, instead of /api/first ...
                    .modifyResponseBody(String.class, String.class, ((serverWebExchange, s) -> {
                        var newResponse = s.replace( // What it is is described in the README file, a little later
                            endpoint.getServiceUrl(),
                            customUrl
                        ).replace(
                            "Generated server url",
                            "Gateway server url"
                        );
                        return Mono.just(newResponse);
                    }))
            )
            .uri(endpoint.getServiceUrl()); // In the end, we set the path - from our YAML file to the microservice
    }

    private Function<PredicateSpec, Buildable<Route>> routeForDocumentation(
        ApplicationConfig.ApiConfig.Endpoint endpoint
    ) {
        return r -> r
            .path(endpoint.getGatewayPrefix() + endpoint.getServicePath() + "/**") // Any requests that follow the path, for example for the first service /api/first/ ...
            .filters(f ->
                f.stripPrefix(1) // We remove /api from our request in order to send a valid request to the microservice
            )
            .uri(endpoint.getServiceUrl()); // Just like in line 62 of the code
    }
    }

}
