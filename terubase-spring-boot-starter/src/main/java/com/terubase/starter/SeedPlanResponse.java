package com.terubase.starter;

import java.util.List;
import java.util.Map;

public record SeedPlanResponse(
        String scenarioId,
        String scenarioTitle,
        String scenario,
        int count,
        String dialect,
        int entityCount,
        List<Map<String, Object>> entities,
        List<String> insertOrderHints,
        String schemaPrompt,
        Map<String, Object> recommendedMockRequest
) {
}
