package com.terubase.starter;

public record SqlExportResponse(
        String filename,
        String contentType,
        String content,
        int statementCount
) {
}
