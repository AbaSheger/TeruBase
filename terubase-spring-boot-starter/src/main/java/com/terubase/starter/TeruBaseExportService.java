package com.terubase.starter;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TeruBaseExportService {

    private static final String DEFAULT_SQL_FILENAME = "terubase-seed.sql";
    private static final String DEFAULT_JSON_FILENAME = "terubase-seed.json";

    private final Clock clock;

    public TeruBaseExportService() {
        this(Clock.systemUTC());
    }

    TeruBaseExportService(Clock clock) {
        this.clock = clock;
    }

    public SqlExportResponse exportSql(ExportRequest request) {
        List<String> statements = validatedStatements(request);
        return new SqlExportResponse(
                filename(request.filename(), DEFAULT_SQL_FILENAME, ".sql"),
                "text/plain",
                TeruBaseSqlService.toExportSql(statements),
                statements.size()
        );
    }

    public JsonExportResponse exportJson(ExportRequest request) {
        List<String> statements = validatedStatements(request);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("generatedBy", "TeruBase");
        metadata.put("exportedAt", Instant.now(clock).toString());
        metadata.put("safeForReview", true);

        return new JsonExportResponse(
                filename(request.filename(), DEFAULT_JSON_FILENAME, ".json"),
                "application/json",
                request.scenario(),
                statements.size(),
                statements,
                metadata
        );
    }

    private static List<String> validatedStatements(ExportRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Export request is required.");
        }
        return SqlSafetyValidator.normalizeInsertStatements(request.statements());
    }

    private static String filename(String requestedFilename, String defaultFilename, String extension) {
        if (requestedFilename == null || requestedFilename.isBlank()) {
            return defaultFilename;
        }
        String filename = requestedFilename.strip();
        return filename.toLowerCase(Locale.ROOT).endsWith(extension) ? filename : filename + extension;
    }
}
