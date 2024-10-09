! NOTE: Initially, the manual was written in Russian, translated into English with the help of a translator, I apologize in advance for inaccuracies in the translation
! RU manual - [./README_ru.md](https://github.com/LZTD1/centralizedSwaggerDocumentation/blob/main/README_ru.md "Click to see")
# Centralized (single) Swagger page for all your microservices
> This material does not claim a high degree of quality, it was created only for the same applicants as me, who at the beginning of their journey are wondering about a centralized Swagger page, be happy :)
###### 1. The original state
![before image](https://raw.githubusercontent.com/LZTD1/centralizedSwaggerDocumentation/refs/heads/main/assets/before.png "The original state project")
At the very beginning, I had such a Spring Boot application, 6 separate microservices running on a single domain (monorepository), but different ports, each of them had such a dependency:
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```
The Swagger documentation itself was generated at the address /swagger-url specified in the yaml file
```yaml
springdoc:
  swagger-ui:
    path: /swagger-ui
```
There was an urgent need to centralize all your pages on a single Swagger document, on the port, conditionally - 8000. I considered a separate microservice that aggregates Swagger pages to be a good practice.
###### 2. Description of the Gateway microservice
Having raised the additional service, on port 8000, there were 2 tasks:
1. A single page for all Swagger documents
2. A single entry point for accessing **all** endpoints
For this task, it was necessary to proxy all traffic, for which this dependency was necessary for the new micro service:
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
    <version>4.1.5</version>
</dependency>
```
And also, since Spring Cloud runs on reactive technology, the **reactive** engine SpringDoc, and **reactive** web starter
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webflux-ui</artifactId>
    <version>2.3.0</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```
In the service itself, it was necessary to specify information about the available endpoints, and addresses, and better, I did not find a way to create a yaml configuration file and describe it in the Spring @ConfigurationProperties annotation.
1. Java file configuration application - [./gateway-service/ApplicationConfig.java](https://github.com/LZTD1/centralizedSwaggerDocumentation/blob/main/gateway-service/ApplicationConfig.java "Click to see")
2. YAML file describing service - [./gateway-service/application.yaml](https://github.com/LZTD1/centralizedSwaggerDocumentation/blob/main/gateway-service/application.yaml "Click to see")

The structure of the YAML file consists of
```yaml
firstService: # The name of your service - it will also be called in a single page
    gateway-prefix: /api # The prefix of the Gateway service, for routing all ./microservices request api
    service-path: /first # The prefix that is used in the microservice itself
    service-url: http://${LOCAL_ADDR}:8001 # The path to the microservice
```
<details>
  <summary>Detailed explanation service-path: /first</summary>

In each micro service, the controllers must be configured as follows:

  ```java
// ... Other code
@GetMapping("/first/mySuperControllerFunc")
public Suggestions mySuperControllerFunc(){
// Other code ...
  ```
that is, so that the prefix of all mappings starts as one, and describing the essence of the microservice, in this case /first, so that it would be possible to perform routing in the gateway service later, using just this prefix
</details>

###### 3. Writing @Bean, for aggregation of routing to a single address
Now we need to make sure that all requests for
http://inetAddress:8000/** -> we have been validly proxied to our microservices, for this we will write a new configuration file for Spring Cloud

1. Java file describing the configuration - [./gateway-service/SwaggerGatewayConfig.java](https://github.com/LZTD1/centralizedSwaggerDocumentation/blob/main/gateway-service/SwaggerGatewayConfig.java "Click to see")

Next, see the comments in the file

<details>

 <summary>What kind of modification of the response body is present in the routing?</summary>

```java
.modifyResponseBody(String.class, String.class, ((serverWebExchange, s) -> {
    var newResponse = s.replace(
        endpoint.getServiceUrl(),
        customUrl
    ).replace(
        "Generated server url",
        "Gateway server url"
    );
    return Mono.just(newResponse);
}))
```
Since Swagger, although it will be opened from a single entry point on port 8000, its internal specification received from the microservice will still point to the microservice address, and when trying to send a request from swagger 8000, requests will still be sent to the 800* port 

! Note: the above is a problem only in the swagger UI, everything works directly over http

![problem image](https://raw.githubusercontent.com/LZTD1/centralizedSwaggerDocumentation/refs/heads/main/assets/problem.png "problem")

Therefore, we **explicitly** using proxying (modifyResponseBody filter), find the server address in the json response, and change it to our gateway service address
</details>

###### 4. Writing @Bean, to aggregate all documents on a single swagger page

And the very last step is to collect everything in a single place by Swagger page

1. Java file describing the configuration of the Open API - [./gateway-service/SwaggerUrlsConfig.java](https://github.com/LZTD1/centralizedSwaggerDocumentation/blob/main/gateway-service/SwaggerUrlsConfig.java "Click to see")

###### 5. Conclusion
Thus, we got a microservice that aggregates + proxies all requests.
It remains only to configure CORS on the gateway service, so that Frontend can interact with our application and that's it, he also brought his own implementation option

1. Java file describing CORS - [./gateway-service/CorsConfig.java](https://github.com/LZTD1/centralizedSwaggerDocumentation/blob/main/gateway-service/CorsConfig.java "Click to see")

For convenience, I also added the following configuration to the gateway api YAML file, for access without a specific path to the Swagger API
```yaml
springdoc:
  swagger-ui:
    path: /
```
**Thank you** for reading, if you have suggestions and ideas for improvement, I am always ready to listen :)