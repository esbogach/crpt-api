# Crpt-Api

# ЧестныйЗнак API Клиент (CrptApi)

Thread-safe Java клиент для работы с API Честного знака с поддержкой ограничения запросов

## Описание

Реализация thread-safe клиента для работы с API Честного знака (система маркировки товаров) с поддержкой ограничения
количества запросов в заданный интервал времени.

## Особенности

- Поддержка ограничения запросов (rate limiting)
- Thread-safe реализация
- Простое расширение функционала
- Готовность к работе в высоконагруженных системах
- Поддержка Java 11+

## Использование

### Инициализация клиента

```java
// Создание клиента с ограничением 10 запросов в минуту
CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 10, "token", httpClient);

// Создание объекта документа
CrptApi.Document document = new CrptApi.Document(
    /* заполнение полей документа */
);

// Подпись документа
String signature = "digital_signature_here";

// Отправка документа
crptApi.createDocument(document, signature);
```

