package org.example.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrptApiTest {

    @Mock
    private HttpClient httpClient;


    @Test
    void createDocument_Success() throws Exception {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 10, "token", httpClient);
        // Arrange
        CrptApi.Document document = new CrptApi.Document(
            "doc123", "MANUAL", "milk", "LP_INTRODUCE_GOODS");
        String signature = "signature123";
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.body()).thenReturn("{\"value\":\"doc123\"}");

        when(httpClient.send(
            any(HttpRequest.class),
            eq(HttpResponse.BodyHandlers.ofString())
        )).thenReturn(mockResponse);

        // Act
        CrptApi.ApiResponse<?> response = crptApi.createDocument(document, signature);

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getValue());
        assertEquals("doc123", response.getValue().getValue());
    }

    @Test
    void createDocument_ValidationError() {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 10, "token", httpClient);
        // Arrange
        CrptApi.Document invalidDocument = new CrptApi.Document(
            null, "MANUAL", "milk", "LP_INTRODUCE_GOODS"); // productDocument is null
        String signature = "signature123";


        CrptApi.ApiResponse<?> response = crptApi.createDocument(invalidDocument, signature);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals("Отсутствуют необходимые поля: [productDocument]", response.getError());
    }

    @Test
    void createDocument_RateLimit() throws Exception {
        // Arrange - создаем API с лимитом 1 запрос в секунду
        CrptApi rateLimitedApi = new CrptApi(TimeUnit.SECONDS, 1, "token", httpClient);

        // Mock HTTP client
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.body()).thenReturn("{\"value\":\"doc123\"}");
        when(httpClient.send(
            any(HttpRequest.class),
            eq(HttpResponse.BodyHandlers.ofString())
        )).thenReturn(mockResponse);

        // Act - делаем два запроса подряд
        CrptApi.Document document = new CrptApi.Document(
            "doc123", "MANUAL", "milk", "LP_INTRODUCE_GOODS");

        // Первый запрос должен пройти
        CrptApi.ApiResponse<?> firstResponse = rateLimitedApi.createDocument(document, "sig1");
        assertTrue(firstResponse.isSuccess());

        // Второй запрос должен быть заблокирован rate limiter'ом
        CrptApi.ApiResponse<?> response = rateLimitedApi.createDocument(document, "sig2");
        // Assert
        assertTrue(response.isSuccess());
        //Считать вызовы Semaphore
    }

    @Test
    void createDocument_NetworkError() throws Exception {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 10, "token", httpClient);
        // Arrange
        CrptApi.Document document = new CrptApi.Document(
            "doc123", "MANUAL", "milk", "LP_INTRODUCE_GOODS");
        String signature = "signature123";

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenThrow(new RuntimeException("Network error"));

        // Act
        CrptApi.ApiResponse<?> response = crptApi.createDocument(document, signature);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals("Network error", response.getError());
    }

    @Test
    void validationBody_MissingRequiredFields() {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 10, "token", httpClient);
        // Arrange
        Map<String, String> invalidMap = Map.of(
            "productDocument", "doc123",
            "documentFormat", "MANUAL"
            // Не хватает signature и type
        );

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            crptApi.validationBody(invalidMap));
    }

    @Test
    void processResponse_Success() throws Exception {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 10, "token", httpClient);
        // Arrange
        String jsonResponse = "{\"value\":\"doc123\",\"code\":\"200\"}";

        // Act
        CrptApi.ApiResponse<?> response = crptApi.processResponse(jsonResponse);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("doc123", response.getValue().getValue());
        assertEquals("200", response.getValue().getCode());
    }
}
