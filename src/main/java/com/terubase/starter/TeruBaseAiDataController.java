package com.terubase.starter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping("/terubase/api")
@CrossOrigin(origins = "*")
public class TeruBaseAiDataController {

    private static final Logger LOGGER = Logger.getLogger(TeruBaseAiDataController.class.getName());
    private static final TypeReference<List<String>> SQL_LIST_TYPE = new TypeReference<>() {};

    private final TeruBaseOpenAiClient openAiClient;
    private final TeruBaseSqlService sqlService;
    private final ObjectMapper objectMapper;
    private final TeruBaseProperties properties;

    public TeruBaseAiDataController(
            TeruBaseOpenAiClient openAiClient,
            TeruBaseSqlService sqlService,
            ObjectMapper objectMapper,
            TeruBaseProperties properties
    ) {
        this.openAiClient = openAiClient;
        this.sqlService = sqlService;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @PostMapping("/mock")
    @CrossOrigin(origins = "*")
    public ResponseEntity<?> mock(@RequestBody MockDataRequest request) {
        try {
            validateCount(request.count());
            String modelOutput = openAiClient.generateInsertSql(request);
            List<String> insertStatements = parseInsertStatements(modelOutput);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("requestedCount", request.count());
            body.put("scenario", request.safeScenario());
            body.put("dialect", request.safeDialect());
            body.put("generatedStatements", insertStatements.size());
            body.put("statements", insertStatements);
            body.put("exportSql", TeruBaseSqlService.toExportSql(insertStatements));
            body.put("executed", request.execute());

            if (request.execute()) {
                SqlExecutionResult result = sqlService.executeBatchTransactionally(insertStatements);
                body.put("execution", result);
            }

            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(TeruBaseController.error("invalid_mock_request", ex));
        } catch (JsonProcessingException ex) {
            LOGGER.log(Level.WARNING, "TeruBase failed to parse model JSON output", ex);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(TeruBaseController.error("model_json_parse_failed", ex));
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "TeruBase mock data batch failed and was rolled back", ex);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(TeruBaseController.error("mock_sql_batch_failed_rolled_back", ex));
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "TeruBase OpenAI-compatible API request failed", ex);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(TeruBaseController.error("ai_generation_failed", ex));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(TeruBaseController.error("ai_generation_interrupted", ex));
        }
    }

    private void validateCount(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("count must be greater than zero.");
        }
        if (count > properties.getMaxMockRows()) {
            throw new IllegalArgumentException("count must not exceed terubase.max-mock-rows=" + properties.getMaxMockRows());
        }
    }

    private List<String> parseInsertStatements(String modelOutput) throws JsonProcessingException {
        String json = stripCodeFence(modelOutput);
        List<String> statements = objectMapper.readValue(json, SQL_LIST_TYPE);
        return SqlSafetyValidator.normalizeInsertStatements(statements);
    }

    private static String stripCodeFence(String value) {
        String text = value.strip();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```(?:json)?", "").replaceFirst("```$", "").strip();
        }
        return text;
    }
}
