package com.terubase.starter.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Mojo(
        name = "export-flyway",
        defaultPhase = LifecyclePhase.NONE,
        threadSafe = true
)
public class ExportFlywayMojo extends AbstractMojo {

    @Parameter(
            property = "terubase.inputFile",
            defaultValue = "${project.build.directory}/terubase/generated-seed.sql",
            required = true
    )
    private File inputFile;

    @Parameter(
            property = "terubase.outputFile",
            defaultValue = "${project.basedir}/src/main/resources/db/migration/V999__terubase_seed_data.sql",
            required = true
    )
    private File outputFile;

    @Override
    public void execute() throws MojoExecutionException {
        Path inputPath = inputFile.toPath();
        Path outputPath = outputFile.toPath();
        try {
            String sql = Files.readString(inputPath, StandardCharsets.UTF_8);
            InsertOnlySqlValidator.validate(sql);
            Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(outputPath, sql, StandardCharsets.UTF_8);
            getLog().info("Generated Flyway migration " + outputPath);
        } catch (IOException ex) {
            throw new MojoExecutionException("Could not export TeruBase SQL to Flyway migration.", ex);
        }
    }
}
