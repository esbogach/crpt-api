package org.example.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;


public class CrptApi {
    private final Semaphore rateLimiter;
    private final HttpClient httpClient;
    private final String apiUrl = "https://ismp.crpt.ru/api/v3/lk/documents/create?pg=";
    private final String authToken;
    private final ObjectMapper mapper = new ObjectMapper();
    private final List<String> requiredFields = List.of("productDocument", "documentFormat", "signature", "type");


    public CrptApi(TimeUnit timeUnit, int requestLimit, String authToken, HttpClient httpClient) {
        this.rateLimiter = new Semaphore(requestLimit);
        this.authToken = authToken;


        new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(timeUnit.toMillis(1));
                    rateLimiter.release(requestLimit - rateLimiter.availablePermits());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        this.httpClient = httpClient;
    }

    public ApiResponse createDocument(Document document, String signature) {
        try {
            rateLimiter.acquire();
            String requestBody = buildRequestBody(document, signature);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + document.getProductGroup()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + authToken)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return processResponse(response.body());

        } catch (Exception e) {
            rateLimiter.release();
            return ApiResponse.error(e.getMessage());
        }
    }

    private String buildRequestBody(Document document, String signature) throws JsonProcessingException {
        Map<String, String> map = mapper.convertValue(document, Map.class);
        map.put("signature", signature);
        map.values().removeIf(Objects::isNull);

        validationBody(map);

        String successJson = mapper.writeValueAsString(map);
        System.out.println(successJson);
        return successJson;
    }

    void validationBody(Map<String, String> map) {
        List<String> missingFields = requiredFields.stream()
            .filter(field -> !map.containsKey(field))
            .toList();

        if (!missingFields.isEmpty()) {
            throw new IllegalArgumentException("Отсутствуют необходимые поля: " + missingFields);
        }
    }

    ApiResponse processResponse(String response) throws JsonProcessingException {
        System.out.println("API Response: " + response);
        ResponseDocument document = mapper.readValue(response, ResponseDocument.class);
        return ApiResponse.success(document);
    }

    @Getter
    @RequiredArgsConstructor
    public static class Document {
        private final String productDocument;
        private final String documentFormat;
        private final String productGroup;
        private final String type;
    }

    @Getter
    public static class ResponseDocument {
        private final String value;       // Уникальный идентификатор (опциональный)
        private final String code;        // Код ошибки (опциональный)
        private final String errorMessage; // Сообщение об ошибке (опциональный)
        private final String description;  // Описание ошибки (опциональный)

        @JsonCreator
        public ResponseDocument(
            @JsonProperty("value") String value,
            @JsonProperty("code") String code,
            @JsonProperty("errorMessage") String errorMessage,
            @JsonProperty("description") String description) {
            this.value = value;
            this.code = code;
            this.errorMessage = errorMessage;
            this.description = description;
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class ApiResponse<T> {
        private final boolean success;
        private final ResponseDocument value;       // Уникальный идентификатор (опциональный)
        private final T error;

        // Статические методы для удобного создания ответов
        public static <T> ApiResponse<T> success(ResponseDocument value) {
            return new ApiResponse<>(true, value, null);
        }

        public static <T> ApiResponse<T> error(T errorMessage) {
            return new ApiResponse<>(false, null, errorMessage);
        }
    }


}
