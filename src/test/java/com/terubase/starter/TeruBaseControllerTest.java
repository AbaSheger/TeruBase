package com.terubase.starter;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TeruBaseControllerTest {

    @Test
    void blocksDirectSqlExecutionByDefault() throws Exception {
        TeruBaseProperties properties = new TeruBaseProperties();
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new TeruBaseController(new TeruBaseSqlService(dataSource()), properties)
        ).build();

        mockMvc.perform(post("/terubase/api/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sql": "select 1"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("sql_execution_disabled"));
    }

    private static JdbcDataSource dataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:terubase_controller_test_db");
        return dataSource;
    }
}
