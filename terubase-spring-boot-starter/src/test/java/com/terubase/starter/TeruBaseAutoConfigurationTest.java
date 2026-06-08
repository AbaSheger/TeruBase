package com.terubase.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.sql.DataSource;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class TeruBaseAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TeruBaseAutoConfiguration.class))
            .withBean(ObjectMapper.class, ObjectMapper::new);

    private final WebApplicationContextRunner webContextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TeruBaseAutoConfiguration.class))
            .withUserConfiguration(TestWebConfiguration.class);

    @Test
    void enablesTeruBaseForLocalProfile() {
        contextRunner.withPropertyValues(
                        "spring.profiles.active=local",
                        "terubase.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(TeruBaseController.class);
                    assertThat(context).hasSingleBean(TeruBaseSchemaController.class);
                    assertThat(context).hasSingleBean(TeruBaseScenarioController.class);
                    assertThat(context).hasSingleBean(TeruBaseSeedPlanController.class);
                    assertThat(context).hasSingleBean(TeruBaseExportController.class);
                    assertThat(context).hasSingleBean(TeruBaseAiDataController.class);
                });
    }

    @Test
    void remainsDisabledUntilExplicitlyEnabled() {
        contextRunner.withPropertyValues("spring.profiles.active=local")
                .run(context -> assertThat(context).doesNotHaveBean(TeruBaseController.class));
    }

    @Test
    void blocksTeruBaseForProdProfile() {
        contextRunner.withPropertyValues(
                        "spring.profiles.active=prod",
                        "terubase.enabled=true"
                )
                .run(context -> assertThat(context).doesNotHaveBean(TeruBaseScenarioController.class));
    }

    @Test
    void blocksTeruBaseForProductionProfile() {
        contextRunner.withPropertyValues(
                        "spring.profiles.active=production",
                        "terubase.enabled=true"
                )
                .run(context -> assertThat(context).doesNotHaveBean(TeruBaseScenarioController.class));
    }

    @Test
    void forceEnableOverridesProductionProfileGuard() {
        contextRunner.withPropertyValues(
                        "spring.profiles.active=production",
                        "terubase.enabled=true",
                        "terubase.force-enable-in-production=true"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(TeruBaseScenarioController.class);
                    assertThat(context).hasSingleBean(TeruBaseProperties.class);
                    assertThat(context.getBean(TeruBaseProperties.class).isForceEnableInProduction()).isTrue();
                });
    }

    @Test
    void defaultsDirectSqlExecutionToDisabled() {
        contextRunner.withPropertyValues("terubase.enabled=true").run(context ->
                assertThat(context.getBean(TeruBaseProperties.class).isSqlExecutionEnabled()).isFalse());
    }

    @Test
    void keepsIsolatedDatasourceInternalWhenHostApplicationProvidesDatasource() {
        JdbcDataSource hostDataSource = new JdbcDataSource();
        hostDataSource.setURL("jdbc:h2:mem:host_application_db");

        contextRunner.withPropertyValues("terubase.enabled=true")
                .withBean("hostDataSource", DataSource.class, () -> hostDataSource)
                .run(context -> {
                    assertThat(context.getBeansOfType(DataSource.class)).containsOnlyKeys("hostDataSource");
                    assertThatCode(() -> assertThat(context.getBean(TeruBaseSqlService.class).status())
                            .containsEntry("url", "jdbc:h2:mem:terubase_isolated_db"))
                            .doesNotThrowAnyException();
                });
    }

    @Test
    void registersEveryDocumentedEndpointForConsumingApplications() {
        webContextRunner.withPropertyValues("terubase.enabled=true").run(context -> {
            RequestMappingHandlerMapping mappings = context.getBean(RequestMappingHandlerMapping.class);
            Set<String> paths = mappings.getHandlerMethods().keySet().stream()
                    .flatMap(mapping -> mapping.getPatternValues().stream())
                    .collect(Collectors.toSet());

            assertThat(paths).contains(
                    "/terubase/api/status",
                    "/terubase/api/execute",
                    "/terubase/api/entities",
                    "/terubase/api/scenarios",
                    "/terubase/api/scenarios/{id}",
                    "/terubase/api/seed-plan",
                    "/terubase/api/mock",
                    "/terubase/api/export/sql",
                    "/terubase/api/export/json"
            );
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    static class TestWebConfiguration {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
