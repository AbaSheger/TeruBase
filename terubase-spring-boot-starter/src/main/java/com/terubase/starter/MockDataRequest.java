package com.terubase.starter;

public record MockDataRequest(
        int count,
        String apiKey,
        String schema,
        String scenario,
        String dialect,
        boolean execute
) {
    public String safeScenario() {
        return scenario == null || scenario.isBlank()
                ? "Generate realistic local development seed data."
                : scenario.strip();
    }

    public String safeDialect() {
        return dialect == null || dialect.isBlank() ? "h2-postgresql-mode" : dialect.strip();
    }
}
