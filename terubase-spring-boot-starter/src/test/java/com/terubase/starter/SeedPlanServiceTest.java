package com.terubase.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SeedPlanServiceTest {

    @Test
    void generatesDefaultSeedPlan() {
        SeedPlanResponse response = service(List.of()).generate(null, null, 20, null);

        assertThat(response.scenarioId()).isNull();
        assertThat(response.scenarioTitle()).isNull();
        assertThat(response.scenario())
                .isEqualTo("Generate realistic relationship-aware local development seed data.");
        assertThat(response.count()).isEqualTo(20);
        assertThat(response.dialect()).isEqualTo("h2-postgresql-mode");
    }

    @Test
    void usesMatchingScenarioTemplate() {
        SeedPlanResponse response = service(List.of())
                .generate("saas-billing-demo", "ignored", 30, "postgresql");

        assertThat(response.scenarioId()).isEqualTo("saas-billing-demo");
        assertThat(response.scenarioTitle()).isEqualTo("SaaS Billing Demo");
        assertThat(response.scenario()).contains("SaaS billing demo");
    }

    @Test
    void usesCustomScenarioWhenScenarioIdIsNotProvided() {
        SeedPlanResponse response = service(List.of()).generate(null, "Generate overdue invoices.", 20, null);

        assertThat(response.scenario()).isEqualTo("Generate overdue invoices.");
    }

    @Test
    void rejectsUnknownScenarioId() {
        assertThatThrownBy(() -> service(List.of()).generate("missing", null, 20, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown scenarioId");
    }

    @Test
    void schemaPromptContainsDiscoveredEntityAndTableNames() {
        SeedPlanResponse response = service(List.of(Map.of(
                "entityName", "Invoice",
                "tableName", "billing_invoice"
        ))).generate(null, null, 20, null);

        assertThat(response.entityCount()).isEqualTo(1);
        assertThat(response.schemaPrompt()).contains("Invoice", "billing_invoice");
    }

    @Test
    void schemaPromptContainsEnumValuesAndRelationshipMetadata() {
        SeedPlanResponse response = service(List.of(Map.of(
                "fields", List.of(
                        Map.of("name", "status", "enumValues", List.of("ACTIVE", "SUSPENDED")),
                        Map.of("name", "account", "relationships", List.of(Map.of(
                                "type", "ManyToOne",
                                "optional", false
                        )))
                )
        ))).generate(null, null, 20, null);

        assertThat(response.schemaPrompt()).contains("ACTIVE", "SUSPENDED", "ManyToOne", "optional");
    }

    @Test
    void recommendsExportOnlyMockRequest() {
        SeedPlanResponse response = service(List.of()).generate(null, null, 20, null);

        assertThat(response.recommendedMockRequest()).containsEntry("execute", false);
    }

    private static SeedPlanService service(List<Map<String, Object>> entities) {
        return new SeedPlanService(
                new TestSchemaService(entities),
                new ScenarioTemplateService(),
                new ObjectMapper()
        );
    }

    private static class TestSchemaService extends TeruBaseSchemaService {

        private final List<Map<String, Object>> entities;

        TestSchemaService(List<Map<String, Object>> entities) {
            super(null, null);
            this.entities = entities;
        }

        @Override
        public List<Map<String, Object>> entities() {
            return entities;
        }
    }
}
