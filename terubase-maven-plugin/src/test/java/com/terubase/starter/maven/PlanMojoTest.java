package com.terubase.starter.maven;

import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlanMojoTest {

    @Test
    void planGoalWritesSchemaContextAndSeedPlanWithoutAi() throws Exception {
        Path outputDirectory = Files.createTempDirectory("terubase-plan-test");
        PlanMojo mojo = new PlanMojo();
        set(mojo, "project", project());
        set(mojo, "entityBasePackage", "com.terubase.starter.maven.fixture");
        set(mojo, "scenario", "Generate realistic relationship-aware local development seed data.");
        set(mojo, "count", 20);
        set(mojo, "dialect", "h2-postgresql-mode");
        set(mojo, "outputDirectory", outputDirectory.toFile());

        mojo.execute();

        Path schemaContext = outputDirectory.resolve("schema-context.json");
        Path seedPlan = outputDirectory.resolve("seed-plan.md");
        assertThat(schemaContext).exists();
        assertThat(seedPlan).exists();
        assertThat(Files.readString(schemaContext))
                .contains("InvoicePlanFixture")
                .contains("invoice_plan_fixture");
        assertThat(Files.readString(seedPlan))
                .contains("# TeruBase Seed Plan")
                .contains("No AI provider was called");
        assertThat(mojo.nextSteps())
                .contains("TeruBase AI prompt (copy and paste into your preferred AI)")
                .contains("Generate INSERT-only seed SQL")
                .contains("generated-seed.sql")
                .contains("terubase:export-data-sql")
                .contains("terubase:export-flyway");
    }

    @Test
    void planGoalFailsWhenNoEntitiesAreFound() throws Exception {
        Path outputDirectory = Files.createTempDirectory("terubase-empty-plan-test");
        PlanMojo mojo = new PlanMojo();
        set(mojo, "project", project());
        set(mojo, "entityBasePackage", "com.example.missing");
        set(mojo, "scenario", "Generate seed data.");
        set(mojo, "count", 20);
        set(mojo, "dialect", "h2-postgresql-mode");
        set(mojo, "outputDirectory", outputDirectory.toFile());

        assertThatThrownBy(mojo::execute)
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("No JPA entities were found");
        assertThat(outputDirectory.resolve("schema-context.json")).doesNotExist();
        assertThat(outputDirectory.resolve("seed-plan.md")).doesNotExist();
    }

    private static MavenProject project() {
        MavenProject project = new MavenProject();
        project.setGroupId("com.terubase.starter.maven.fixture");
        Build build = new Build();
        build.setOutputDirectory(Path.of("target", "test-classes").toAbsolutePath().toString());
        project.setBuild(build);
        project.setArtifacts(Set.of());
        return project;
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
