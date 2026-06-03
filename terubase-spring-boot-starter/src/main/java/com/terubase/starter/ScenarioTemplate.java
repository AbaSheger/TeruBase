package com.terubase.starter;

import java.util.List;

public record ScenarioTemplate(
        String id,
        String title,
        String description,
        String prompt,
        List<String> bestFor,
        List<String> exampleEntities
) {
}

