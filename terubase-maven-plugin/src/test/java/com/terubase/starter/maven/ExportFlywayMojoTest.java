package com.terubase.starter.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExportFlywayMojoTest {

    @Test
    void exportFlywayWritesInsertOnlySqlAndCreatesDirectories() throws Exception {
        Path directory = Files.createTempDirectory("terubase-export-flyway-test");
        Path inputFile = directory.resolve("target/terubase/generated-seed.sql");
        Path outputFile = directory.resolve("src/main/resources/db/migration/V999__terubase_seed_data.sql");
        Files.createDirectories(inputFile.getParent());
        String sql = """
                -- TeruBase seed data
                INSERT INTO customers (id, name) VALUES (1, 'Acme; Demo');
                INSERT INTO invoices (id, customer_id) VALUES (10, 1);
                """;
        Files.writeString(inputFile, sql);

        ExportFlywayMojo mojo = new ExportFlywayMojo();
        set(mojo, "inputFile", inputFile.toFile());
        set(mojo, "outputFile", outputFile.toFile());

        mojo.execute();

        assertThat(outputFile).exists();
        assertThat(Files.readString(outputFile)).isEqualTo(sql);
    }

    @Test
    void exportFlywayRejectsBlankSql() throws Exception {
        Path directory = Files.createTempDirectory("terubase-export-flyway-blank-test");
        Path inputFile = directory.resolve("target/terubase/generated-seed.sql");
        Path outputFile = directory.resolve("src/main/resources/db/migration/V999__terubase_seed_data.sql");
        Files.createDirectories(inputFile.getParent());
        Files.writeString(inputFile, "   ");

        ExportFlywayMojo mojo = new ExportFlywayMojo();
        set(mojo, "inputFile", inputFile.toFile());
        set(mojo, "outputFile", outputFile.toFile());

        assertThatThrownBy(mojo::execute)
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("blank");
        assertThat(outputFile).doesNotExist();
    }

    @Test
    void exportFlywayRejectsDestructiveSql() throws Exception {
        Path directory = Files.createTempDirectory("terubase-export-flyway-destructive-test");
        Path inputFile = directory.resolve("target/terubase/generated-seed.sql");
        Path outputFile = directory.resolve("src/main/resources/db/migration/V999__terubase_seed_data.sql");
        Files.createDirectories(inputFile.getParent());
        Files.writeString(inputFile, "INSERT INTO customers (id) VALUES (1); DELETE FROM customers;");

        ExportFlywayMojo mojo = new ExportFlywayMojo();
        set(mojo, "inputFile", inputFile.toFile());
        set(mojo, "outputFile", outputFile.toFile());

        assertThatThrownBy(mojo::execute)
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("only accepts INSERT");
        assertThat(outputFile).doesNotExist();
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
