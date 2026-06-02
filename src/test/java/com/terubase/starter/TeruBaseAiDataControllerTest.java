package com.terubase.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TeruBaseAiDataControllerTest {

    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    @AfterEach
    void shutDownExecutor() {
        executorService.shutdownNow();
    }

    @Test
    void defaultsMockGenerationToExportOnly() throws Exception {
        mockMvc("[\"insert into customer (id) values (1)\"]")
                .perform(post("/terubase/api/mock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "count": 1,
                                  "apiKey": "request-only-key",
                                  "schema": "Customer(id)"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executed").value(false))
                .andExpect(jsonPath("$.generatedStatements").value(1))
                .andExpect(jsonPath("$.statements[0]").value("insert into customer (id) values (1)"));
    }

    @Test
    void rejectsUnsafeSqlReturnedByAiProvider() throws Exception {
        mockMvc("[\"delete from customer\"]")
                .perform(post("/terubase/api/mock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "count": 1,
                                  "apiKey": "request-only-key",
                                  "schema": "Customer(id)"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_mock_request"));
    }

    private MockMvc mockMvc(String modelOutput) {
        ObjectMapper objectMapper = new ObjectMapper();
        TeruBaseProperties properties = new TeruBaseProperties();
        TeruBaseOpenAiClient openAiClient = new StubOpenAiClient(objectMapper, properties, modelOutput);
        return MockMvcBuilders.standaloneSetup(new TeruBaseAiDataController(
                openAiClient,
                new TeruBaseSqlService(dataSource()),
                objectMapper,
                properties
        )).build();
    }

    private static JdbcDataSource dataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:terubase_ai_controller_test_db");
        return dataSource;
    }

    private class StubOpenAiClient extends TeruBaseOpenAiClient {

        private final String modelOutput;

        StubOpenAiClient(ObjectMapper objectMapper, TeruBaseProperties properties, String modelOutput) {
            super(objectMapper, properties, executorService);
            this.modelOutput = modelOutput;
        }

        @Override
        public String generateInsertSql(MockDataRequest request) {
            return modelOutput;
        }
    }
}
