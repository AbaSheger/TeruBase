package com.terubase.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class TeruBaseSeedPlanControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SeedPlanService seedPlanService =
                new SeedPlanService(
                        new EmptySchemaService(),
                        new ScenarioTemplateService(),
                        new ObjectMapper(),
                        new TeruBaseProperties()
                );
        mockMvc = standaloneSetup(new TeruBaseSeedPlanController(seedPlanService)).build();
    }

    @Test
    void returnsSeedPlanWithScenarioTemplate() throws Exception {
        mockMvc.perform(get("/terubase/api/seed-plan")
                        .param("scenarioId", "saas-billing-demo")
                        .param("count", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scenarioId").value("saas-billing-demo"))
                .andExpect(jsonPath("$.scenarioTitle").value("SaaS Billing Demo"))
                .andExpect(jsonPath("$.count").value(30))
                .andExpect(jsonPath("$.recommendedMockRequest.execute").value(false));
    }

    @Test
    void returnsBadRequestForUnknownScenarioId() throws Exception {
        mockMvc.perform(get("/terubase/api/seed-plan").param("scenarioId", "missing"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_seed_plan_request"));
    }

    @Test
    void returnsBadRequestWhenCountExceedsMockLimit() throws Exception {
        mockMvc.perform(get("/terubase/api/seed-plan").param("count", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_seed_plan_request"));
    }

    private static class EmptySchemaService extends TeruBaseSchemaService {

        EmptySchemaService() {
            super(null, null);
        }

        @Override
        public List<Map<String, Object>> entities() {
            return List.of();
        }
    }
}
