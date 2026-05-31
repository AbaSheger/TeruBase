package com.terubase.starter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

public class TeruBaseOpenAiClient {

    private static final Logger LOGGER = Logger.getLogger(TeruBaseOpenAiClient.class.getName());

    private final ObjectMapper objectMapper;
    private final TeruBaseProperties properties;
    private final HttpClient httpClient;

    public TeruBaseOpenAiClient(ObjectMapper objectMapper, TeruBaseProperties properties, ExecutorService executorService) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .executor(executorService)
                .build();
    }

    public String generateInsertSql(MockDataRequest request) throws IOException, InterruptedException {
        if (request.apiKey() == null || request.apiKey().isBlank()) {
            throw new IllegalArgumentException("apiKey is required for AI mock-data generation.");
        }
        if (request.schema() == null || request.schema().isBlank()) {
            throw new IllegalArgumentException("schema is required for AI mock-data generation.");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", properties.getOpenAiModel());
        payload.put("temperature", 0.2);
        payload.put("messages", List.of(
                Map.of(
                        "role", "system",
                        "content", "You generate safe local-development seed SQL only. Return a JSON array of SQL INSERT statements as strings. No markdown, no prose, no deletes, no drops, no updates. Respect foreign-key order and realistic relationships."
                ),
                Map.of(
                        "role", "user",
                        "content", buildPrompt(request)
                )
        ));

        String body = objectMapper.writeValueAsString(payload);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(properties.getOpenAiChatCompletionsUrl()))
                .timeout(Duration.ofSeconds(90))
                .header("Authorization", "Bearer " + request.apiKey().strip())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        LOGGER.info(() -> "Requesting TeruBase AI seed data using model " + properties.getOpenAiModel());
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("OpenAI-compatible API returned HTTP " + response.statusCode() + ": " + response.body());
        }
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || content.asText().isBlank()) {
            throw new IOException("OpenAI-compatible API response did not contain message content.");
        }
        return content.asText().strip();
    }

    private static String buildPrompt(MockDataRequest request) {
        return "Generate exactly " + request.count() + " realistic seed-data INSERT statements.\n"
                + "Dialect: " + request.safeDialect() + "\n"
                + "Scenario: " + request.safeScenario() + "\n"
                + "Schema/JPA metadata:\n" + request.schema().strip() + "\n\n"
                + "Rules:\n"
                + "- Return only a JSON array of SQL strings.\n"
                + "- Respect primary keys, nullable fields, unique fields, and foreign-key insert order.\n"
                + "- Generate coherent relationship-aware data, not random placeholders.\n"
                + "- Use safe fictional data only.\n"
                + "- Do not include markdown code fences.\n";
    }
}
