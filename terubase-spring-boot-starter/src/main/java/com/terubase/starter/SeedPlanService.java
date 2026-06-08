package com.terubase.starter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SeedPlanService {

    private static final String DEFAULT_SCENARIO =
            "Generate realistic relationship-aware local development seed data.";

    private static final List<String> INSERT_ORDER_HINTS = List.of(
            "Insert parent and reference tables first.",
            "Insert child tables with foreign keys second.",
            "Insert join tables last.",
            "Do not manually insert generated IDs unless the developer chooses that intentionally.",
            "Populate every nullable=false field.",
            "Keep values unique for fields marked unique.",
            "Use valid enum values for enum fields."
    );

    private final TeruBaseSchemaService schemaService;
    private final ScenarioTemplateService scenarioTemplateService;
    private final ObjectMapper objectMapper;
    private final TeruBaseProperties properties;

    public SeedPlanService(
            TeruBaseSchemaService schemaService,
            ScenarioTemplateService scenarioTemplateService,
            ObjectMapper objectMapper,
            TeruBaseProperties properties
    ) {
        this.schemaService = schemaService;
        this.scenarioTemplateService = scenarioTemplateService;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public SeedPlanResponse generate(String scenarioId, String customScenario, int count, String dialect) {
        validateCount(count);
        String safeDialect = StringUtils.hasText(dialect) ? dialect.strip() : "h2-postgresql-mode";
        ScenarioSelection scenario = selectScenario(scenarioId, customScenario);
        List<Map<String, Object>> entities = schemaService.entities();
        String schemaPrompt = buildSchemaPrompt(entities, scenario.text(), count, safeDialect);

        Map<String, Object> recommendedMockRequest = new LinkedHashMap<>();
        recommendedMockRequest.put("count", count);
        recommendedMockRequest.put("schema", schemaPrompt);
        recommendedMockRequest.put("scenario", scenario.text());
        recommendedMockRequest.put("dialect", safeDialect);
        recommendedMockRequest.put("execute", false);

        return new SeedPlanResponse(
                scenario.id(),
                scenario.title(),
                scenario.text(),
                count,
                safeDialect,
                entities.size(),
                entities,
                INSERT_ORDER_HINTS,
                schemaPrompt,
                recommendedMockRequest
        );
    }

    private ScenarioSelection selectScenario(String scenarioId, String customScenario) {
        if (StringUtils.hasText(scenarioId)) {
            ScenarioTemplate template = scenarioTemplateService.findById(scenarioId.strip())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown scenarioId: " + scenarioId));
            return new ScenarioSelection(template.id(), template.title(), template.prompt());
        }
        String scenario = StringUtils.hasText(customScenario) ? customScenario.strip() : DEFAULT_SCENARIO;
        return new ScenarioSelection(null, null, scenario);
    }

    private String buildSchemaPrompt(
            List<Map<String, Object>> entities,
            String scenario,
            int count,
            String dialect
    ) {
        try {
            return """
                    Generate realistic, relationship-aware seed data using the discovered JPA metadata below.
                    Scenario: %s
                    Target row count: %d
                    SQL dialect: %s

                    Respect these insert-order and data-integrity hints:
                    - %s

                    Discovered JPA entities:
                    %s
                    """.formatted(
                    scenario,
                    count,
                    dialect,
                    String.join("\n- ", INSERT_ORDER_HINTS),
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(entities)
            );
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize discovered JPA metadata.", ex);
        }
    }

    private void validateCount(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("count must be greater than zero.");
        }
        if (count > properties.getMaxMockRows()) {
            throw new IllegalArgumentException(
                    "count must not exceed terubase.max-mock-rows=" + properties.getMaxMockRows()
            );
        }
    }

    private record ScenarioSelection(String id, String title, String text) {
    }
}
