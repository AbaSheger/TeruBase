package com.terubase.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class TeruBaseAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TeruBaseAutoConfiguration.class))
            .withBean(ObjectMapper.class, ObjectMapper::new);

    @Test
    void enablesTeruBaseForLocalProfile() {
        contextRunner.withPropertyValues("spring.profiles.active=local")
                .run(context -> assertThat(context).hasSingleBean(TeruBaseScenarioController.class));
    }

    @Test
    void blocksTeruBaseForProdProfile() {
        contextRunner.withPropertyValues("spring.profiles.active=prod")
                .run(context -> assertThat(context).doesNotHaveBean(TeruBaseScenarioController.class));
    }

    @Test
    void blocksTeruBaseForProductionProfile() {
        contextRunner.withPropertyValues("spring.profiles.active=production")
                .run(context -> assertThat(context).doesNotHaveBean(TeruBaseScenarioController.class));
    }

    @Test
    void forceEnableOverridesProductionProfileGuard() {
        contextRunner.withPropertyValues(
                        "spring.profiles.active=production",
                        "terubase.force-enable-in-production=true"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(TeruBaseScenarioController.class);
                    assertThat(context).hasSingleBean(TeruBaseProperties.class);
                    assertThat(context.getBean(TeruBaseProperties.class).isForceEnableInProduction()).isTrue();
                });
    }
}
