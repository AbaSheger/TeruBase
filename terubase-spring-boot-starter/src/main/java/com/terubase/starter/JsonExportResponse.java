package com.terubase.starter;

import java.util.List;
import java.util.Map;

public record JsonExportResponse(
        String filename,
        String contentType,
        String scenario,
        int statementCount,
        List<String> statements,
        Map<String, Object> metadata
) {
}
