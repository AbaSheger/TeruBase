package com.terubase.starter;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping("/terubase/api")
@CrossOrigin(origins = "*")
public class TeruBaseController {

    private static final Logger LOGGER = Logger.getLogger(TeruBaseController.class.getName());

    private final TeruBaseSqlService sqlService;
    private final TeruBaseProperties properties;

    public TeruBaseController(TeruBaseSqlService sqlService, TeruBaseProperties properties) {
        this.sqlService = sqlService;
        this.properties = properties;
    }

    @GetMapping("/status")
    @CrossOrigin(origins = "*")
    public ResponseEntity<?> status() {
        try {
            return ResponseEntity.ok(sqlService.status());
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "TeruBase status check failed", ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error("status_failed", ex));
        }
    }

    @PostMapping("/execute")
    @CrossOrigin(origins = "*")
    public ResponseEntity<?> execute(@RequestBody SqlRequest request) {
        if (!properties.isSqlExecutionEnabled()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "sql_execution_disabled"));
        }
        try {
            return ResponseEntity.ok(sqlService.execute(request.sql()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(error("invalid_sql_request", ex));
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "TeruBase SQL execution failed", ex);
            return ResponseEntity.badRequest().body(error("sql_execution_failed", ex));
        }
    }

    static Map<String, Object> error(String code, Exception ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", code);
        body.put("message", ex.getMessage());
        return body;
    }
}
