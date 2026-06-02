package com.terubase.starter;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TeruBaseExportControllerTest {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new TeruBaseExportController(new TeruBaseExportService()))
            .build();

    @Test
    void exportsSqlWithoutExecutingIt() throws Exception {
        mockMvc.perform(post("/terubase/api/export/sql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "statements": [
                                    "insert into customer (id, name) values (1, 'Sara')"
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("terubase-seed.sql"))
                .andExpect(jsonPath("$.contentType").value("text/plain"))
                .andExpect(jsonPath("$.statementCount").value(1))
                .andExpect(jsonPath("$.content").value("insert into customer (id, name) values (1, 'Sara');"
                        + System.lineSeparator()));
    }

    @Test
    void exportsJsonWithoutExecutingIt() throws Exception {
        mockMvc.perform(post("/terubase/api/export/json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scenario": "SaaS billing demo",
                                  "filename": "billing-fixture",
                                  "statements": [
                                    "insert into customer (id, name) values (1, 'Sara')"
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("billing-fixture.json"))
                .andExpect(jsonPath("$.contentType").value("application/json"))
                .andExpect(jsonPath("$.scenario").value("SaaS billing demo"))
                .andExpect(jsonPath("$.statementCount").value(1))
                .andExpect(jsonPath("$.metadata.generatedBy").value("TeruBase"))
                .andExpect(jsonPath("$.metadata.safeForReview").value(true));
    }

    @Test
    void returnsControlledBadRequestForUnsafeSql() throws Exception {
        mockMvc.perform(post("/terubase/api/export/sql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "statements": [
                                    "delete from customer"
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_export_request"));
    }
}
