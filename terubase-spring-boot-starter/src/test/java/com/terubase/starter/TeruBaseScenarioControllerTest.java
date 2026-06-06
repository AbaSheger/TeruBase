package com.terubase.starter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class TeruBaseScenarioControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new TeruBaseScenarioController(new ScenarioTemplateService())).build();
    }

    @Test
    void returnsAllScenarios() throws Exception {
        mockMvc.perform(get("/terubase/api/scenarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[0].id").value("ecommerce-demo"))
                .andExpect(jsonPath("$[9].id").value("frontend-dashboard-demo"));
    }

    @Test
    void returnsScenarioById() throws Exception {
        mockMvc.perform(get("/terubase/api/scenarios/crm-demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("crm-demo"))
                .andExpect(jsonPath("$.title").value("CRM Demo"));
    }

    @Test
    void returnsNotFoundForUnknownScenario() throws Exception {
        mockMvc.perform(get("/terubase/api/scenarios/not-a-scenario"))
                .andExpect(status().isNotFound());
    }
}

