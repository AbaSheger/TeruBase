package com.terubase.starter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScenarioTemplateServiceTest {

    private final ScenarioTemplateService service = new ScenarioTemplateService();

    @Test
    void returnsAllBuiltInScenarios() {
        assertThat(service.findAll())
                .extracting(ScenarioTemplate::id)
                .containsExactly(
                        "ecommerce-demo",
                        "saas-billing-demo",
                        "crm-demo",
                        "banking-lite-demo",
                        "task-management-demo",
                        "qa-edge-cases",
                        "frontend-dashboard-demo"
                );
    }

    @Test
    void looksUpScenarioById() {
        assertThat(service.findById("saas-billing-demo"))
                .get()
                .extracting(ScenarioTemplate::title)
                .isEqualTo("SaaS Billing Demo");
    }

    @Test
    void eachScenarioHasRequiredContent() {
        assertThat(service.findAll()).allSatisfy(template -> {
            assertThat(template.id()).isNotBlank();
            assertThat(template.title()).isNotBlank();
            assertThat(template.description()).isNotBlank();
            assertThat(template.prompt()).isNotBlank();
        });
    }
}

