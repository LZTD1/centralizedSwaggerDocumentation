# Централизованная (единая) Swagger страница для всех ваших микросервисов
> Данный материал не претендует на высокую степень качетсва, он создан только для таких же соискателей, как и я, которые в начале своего пути задаются вопросом централизованной Swagger страницы, be happy :)
###### 1. Изначальное состояние
![before image](https://raw.githubusercontent.com/LZTD1/centralizedSwaggerDocumentation/refs/heads/main/assets/before.png "Изначальная струкрута проекта")
В самом начале у меня было такое Spring Boot приложение, 6 раздельных микросервисов, работающих на едином домене (монорепозиторий), но разных портах, у каждого из них была такая зависимость:
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```
Сами же Swagger документации, генерировались по адресу, /swagger-url, прописанному в yaml файле
```yaml
springdoc:
  swagger-ui:
    path: /swagger-ui
```
Возникала острая необходимость централизовать все свои страницы, на едином Swagger документе, условно на порту, условно - 8000. Хорошей практикой я посчитал отдельный микросервис, который агрегирует страницы Swagger
###### 2. Описание Gateway микросервиса
Подняв дополнительный сервис, на порту 8000, стояло 2 задачи:
1. Единая страница для всех Swagger документов
2. Единая точка входа для получения доступа ко **всем** endpoint`ам
Для этой задачи нужно было проксировать весь трафик, для чего на новом микросервисе был необходим данная зависимость:
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
    <version>4.1.5</version>
</dependency>
```
А так же, поскольку Spring Cloud работает на реактивной технологии, **реактивный** движок SpringDoc, так же **реактивный** стартер веба
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
В самом же сервисе, необходимо было указать информацию, об имеющихся endpoint`ах, и адресах и лучше, способа как завести yaml файл с конфигурацией, и описать его в аннотацией Spring @ConfigurationProperties, я не нашел.
1. Java файл конфигурации приложения - [./gateway-service/ApplicationConfig.java](https://github.com/LZTD1/centralizedSwaggerDocumentation/blob/main/gateway-service/ApplicationConfig.java "Нажми, что бы посмотреть")
2. YAML файл описывающий сервисы - [./gateway-service/application.yaml](https://github.com/LZTD1/centralizedSwaggerDocumentation/blob/main/gateway-service/application.yaml "Нажми, что бы посмотреть")

Структура YAML файла состоит из
```yaml
firstService: # Название вашего сервиса - так же он будет называться в единой странице
    gateway-prefix: /api # Префикс Gateway сервиса, для маршрутизации всех ./api запросов на микросервисы 
    service-path: /first # Префикс, который используется в самом микросервисе
    service-url: http://${LOCAL_ADDR}:8001 # Путь, до микросервиса
```
<details>
  <summary>Подробное обьяснение service-path: /first</summary>

  В каждом микросервисе, контроллеры должны быть настроены следующим образом:

  ```java
// ... Other code
@GetMapping("/first/mySuperControllerFunc")
public Suggestions mySuperControllerFunc(){
// Other code ...
  ```
  т.е. что бы префикс всех маппингов начинался едино, и описывая суть микросервиса, в данном случае /first, что бы можно было в gateway сервисе потом валидно, роутинг произвести, используя как раз этот префикс
</details>

###### 3. Написание @Bean, для агрегации роутинга по единому адресу
Сейчас нам необходимо сделать что бы все запросы на 
http://inetAddress:8000/** -> валидно проксировались на свои микросервисы, для этого напишем новый конфигурационный файл для Spring Cloud

1. Java файл, описывающий конфигурацию - [./gateway-service/SwaggerGatewayConfig.java](https://github.com/LZTD1/centralizedSwaggerDocumentation/blob/main/gateway-service/SwaggerGatewayConfig.java "Нажми, что бы посмотреть")

Дальше, смотри коментарии в файле
<details>

 <summary>Что же за модификация тела ответа присутствует в роутинге? </summary>

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
Поскольку Swagger, хоть и будет открыт с единой точки входа на порту 8000, его внутренняя спецификация, полученная с микросервиса, все еще будет указывать на адрес микросервиса, и при попытке отправить запрос с swagger 8000 - запросы будут отправляться все равно на 800* порт

! Заметка: обговоренное выше, является проблемой только в UI swagger, напрямую по http все работает

![problem image](https://raw.githubusercontent.com/LZTD1/centralizedSwaggerDocumentation/refs/heads/main/assets/problem.png "В чем заключается проблема")
Поэтому, мы **явно** с помощью проксирования (фильтра modifyResponseBody), находим в json ответе адрес сервера, и меняем его на свой адрес gateway сервиса
</details>

###### 4. Написание @Bean, для агрегации всех документов на единой сваггер странице
И самый последний шаг, собрать все в едином месте Swagger страницей

1. Java файл, описывающий конфигурацию Open API - [./gateway-service/SwaggerUrlsConfig.java](https://github.com/LZTD1/centralizedSwaggerDocumentation/blob/main/gateway-service/SwaggerUrlsConfig.java "Нажми, что бы посмотреть")

###### 5. Заключение
Таким образом, мы получили микросервис, который агрегирует + проксирует все запросы.
Остается только настроить CORS на gateway сервисе, для того что бы Frontend мог взаимодействовать с нашим приложением и все, так же привел свой вариант реализации 

1. Java файл, описывающий CORS - [./gateway-service/CorsConfig.java](https://github.com/LZTD1/centralizedSwaggerDocumentation/blob/main/gateway-service/CorsConfig.java "Нажми, что бы посмотреть")

Для удобства, я еще добавил, следующую конфигурацию в YAML файл gateway api, для доступа без специфичного path к Swagger API
```yaml
springdoc:
  swagger-ui:
    path: /
```
**Спасибо** что прочитали, если у вас есть предложения и идеи по улучшению, всегда готов выслушать :)