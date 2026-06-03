package com.terubase.starter;

import java.util.List;

public record ExportRequest(
        String scenario,
        List<String> statements,
        String filename
) {
}
