package com.terubase.starter;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Set;

final class TeruBaseLocalOnlyCondition implements Condition {

    private static final Set<String> PRODUCTION_PROFILES = Set.of("prod", "production");

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        boolean forceEnabled = context.getEnvironment()
                .getProperty("terubase.force-enable-in-production", Boolean.class, false);
        if (forceEnabled) {
            return true;
        }

        for (String profile : context.getEnvironment().getActiveProfiles()) {
            if (PRODUCTION_PROFILES.contains(profile)) {
                return false;
            }
        }
        return true;
    }
}
