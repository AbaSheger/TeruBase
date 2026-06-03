package com.terubase.starter.maven;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Mojo(
        name = "plan",
        defaultPhase = LifecyclePhase.NONE,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        aggregator = true,
        threadSafe = true
)
public class PlanMojo extends AbstractMojo {

    private static final List<String> INSERT_ORDER_HINTS = List.of(
            "Insert parent and reference tables first.",
            "Insert child tables with foreign keys second.",
            "Insert join tables last.",
            "Populate every nullable=false field.",
            "Use valid enum values for enum fields."
    );

    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(property = "terubase.entityBasePackage")
    private String entityBasePackage;

    @Parameter(property = "terubase.scenario", defaultValue = "Generate realistic relationship-aware local development seed data.")
    private String scenario;

    @Parameter(property = "terubase.count", defaultValue = "20")
    private int count;

    @Parameter(property = "terubase.dialect", defaultValue = "h2-postgresql-mode")
    private String dialect;

    @Parameter(defaultValue = "${project.build.directory}/terubase", readonly = true, required = true)
    private File outputDirectory;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            Files.createDirectories(outputPath());
            List<Map<String, Object>> entities = scanEntities();
            writeSchemaContext(entities);
            writeSeedPlan(entities);
            getLog().info("Generated " + outputPath().resolve("schema-context.json"));
            getLog().info("Generated " + outputPath().resolve("seed-plan.md"));
        } catch (IOException | RuntimeException ex) {
            throw new MojoExecutionException("Could not generate TeruBase seed plan.", ex);
        }
    }

    List<Map<String, Object>> scanEntities() throws IOException {
        Path outputPath = Path.of(project.getBuild().getOutputDirectory());
        if (!Files.isDirectory(outputPath)) {
            getLog().warn("Project output directory does not exist yet: " + outputPath);
            return List.of();
        }
        try (URLClassLoader projectClassLoader = new URLClassLoader(projectClasspath(), getClass().getClassLoader())) {
            List<Map<String, Object>> entities = new ArrayList<>();
            try (var paths = Files.walk(outputPath)) {
                List<Path> classFiles = paths
                        .filter(path -> path.toString().endsWith(".class"))
                        .sorted()
                        .toList();
                for (Path classFile : classFiles) {
                    String className = className(outputPath, classFile);
                    if (matchesBasePackage(className)) {
                        describeEntity(projectClassLoader, className).ifPresent(entities::add);
                    }
                }
            }
            return entities.stream()
                    .sorted(Comparator.comparing(entity -> String.valueOf(entity.get("className"))))
                    .toList();
        }
    }

    private java.util.Optional<Map<String, Object>> describeEntity(ClassLoader classLoader, String className) {
        try {
            Class<?> type = Class.forName(className, false, classLoader);
            if (!hasAnnotation(type, "jakarta.persistence.Entity")) {
                return java.util.Optional.empty();
            }
            Map<String, Object> entity = new LinkedHashMap<>();
            entity.put("className", type.getName());
            entity.put("simpleName", type.getSimpleName());
            entity.put("tableName", tableName(type));
            List<Map<String, Object>> fields = fields(type);
            entity.put("fields", fields);
            entity.put("insertOrderHint", insertOrderHint(fields));
            return java.util.Optional.of(entity);
        } catch (LinkageError | ClassNotFoundException ex) {
            getLog().warn("Could not inspect class " + className + ": " + ex.getMessage());
            return java.util.Optional.empty();
        }
    }

    private String tableName(Class<?> type) {
        for (Annotation annotation : type.getAnnotations()) {
            if (annotation.annotationType().getName().equals("jakarta.persistence.Table")) {
                String name = annotationValue(annotation, "name");
                if (hasText(name)) {
                    return name;
                }
            }
        }
        return type.getSimpleName();
    }

    private static List<Map<String, Object>> fields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            fields.addAll(List.of(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields.stream()
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .sorted(Comparator.comparing(Field::getName))
                .map(PlanMojo::fieldMetadata)
                .toList();
    }

    private static Map<String, Object> fieldMetadata(Field field) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("name", field.getName());
        metadata.put("type", field.getType().getTypeName());
        metadata.put("id", hasAnnotation(field, "jakarta.persistence.Id"));
        metadata.put("generatedValue", hasAnnotation(field, "jakarta.persistence.GeneratedValue"));
        if (field.getType().isEnum()) {
            metadata.put("enum", true);
            metadata.put("enumValues", List.of(field.getType().getEnumConstants()).stream()
                    .map(value -> ((Enum<?>) value).name())
                    .toList());
        }
        addColumn(metadata, field);
        addRelationship(metadata, field);
        return metadata;
    }

    private static void addColumn(Map<String, Object> metadata, Field field) {
        Annotation column = annotation(field, "jakarta.persistence.Column");
        if (column == null) {
            return;
        }
        Map<String, Object> columnMetadata = new LinkedHashMap<>();
        String name = annotationValue(column, "name");
        columnMetadata.put("name", hasText(name) ? name : field.getName());
        columnMetadata.put("nullable", annotationBooleanValue(column, "nullable", true));
        columnMetadata.put("unique", annotationBooleanValue(column, "unique", false));
        metadata.put("column", columnMetadata);
    }

    private static void addRelationship(Map<String, Object> metadata, Field field) {
        for (String annotationName : List.of(
                "jakarta.persistence.ManyToOne",
                "jakarta.persistence.OneToMany",
                "jakarta.persistence.OneToOne",
                "jakarta.persistence.ManyToMany"
        )) {
            if (hasAnnotation(field, annotationName)) {
                metadata.put("relationship", annotationName.substring(annotationName.lastIndexOf('.') + 1));
                metadata.put("targetType", field.getGenericType().getTypeName());
                return;
            }
        }
    }

    private static boolean hasAnnotation(Class<?> type, String annotationName) {
        return annotation(type, annotationName) != null;
    }

    private static boolean hasAnnotation(Field field, String annotationName) {
        return annotation(field, annotationName) != null;
    }

    private static Annotation annotation(Class<?> type, String annotationName) {
        for (Annotation annotation : type.getAnnotations()) {
            if (annotation.annotationType().getName().equals(annotationName)) {
                return annotation;
            }
        }
        return null;
    }

    private static Annotation annotation(Field field, String annotationName) {
        for (Annotation annotation : field.getAnnotations()) {
            if (annotation.annotationType().getName().equals(annotationName)) {
                return annotation;
            }
        }
        return null;
    }

    private static String annotationValue(Annotation annotation, String name) {
        try {
            Object value = annotation.annotationType().getMethod(name).invoke(annotation);
            return value == null ? "" : String.valueOf(value);
        } catch (ReflectiveOperationException ex) {
            return "";
        }
    }

    private static boolean annotationBooleanValue(Annotation annotation, String name, boolean fallback) {
        try {
            Object value = annotation.annotationType().getMethod(name).invoke(annotation);
            return value instanceof Boolean booleanValue ? booleanValue : fallback;
        } catch (ReflectiveOperationException ex) {
            return fallback;
        }
    }

    private String className(Path outputPath, Path classFile) {
        String relative = outputPath.relativize(classFile).toString();
        return relative.substring(0, relative.length() - ".class".length())
                .replace('\\', '.')
                .replace('/', '.');
    }

    private boolean matchesBasePackage(String className) {
        String basePackage = resolveEntityBasePackage();
        return !hasText(basePackage) || className.equals(basePackage) || className.startsWith(basePackage + ".");
    }

    private String resolveEntityBasePackage() {
        if (hasText(entityBasePackage)) {
            return entityBasePackage.strip();
        }
        return hasText(project.getGroupId()) ? project.getGroupId() : "com";
    }

    private URL[] projectClasspath() throws MalformedURLException {
        List<URL> urls = new ArrayList<>();
        addPath(urls, project.getBuild().getOutputDirectory());
        for (Artifact artifact : project.getArtifacts()) {
            if (artifact.getFile() != null) {
                urls.add(artifact.getFile().toURI().toURL());
            }
        }
        return urls.toArray(URL[]::new);
    }

    private static void addPath(List<URL> urls, String path) throws MalformedURLException {
        if (hasText(path)) {
            urls.add(Path.of(path).toUri().toURL());
        }
    }

    private void writeSchemaContext(List<Map<String, Object>> entities) throws IOException {
        objectMapper.writeValue(outputPath().resolve("schema-context.json").toFile(), Map.of("entities", entities));
    }

    private void writeSeedPlan(List<Map<String, Object>> entities) throws IOException {
        Files.writeString(outputPath().resolve("seed-plan.md"), markdown(entities), StandardCharsets.UTF_8);
    }

    private Path outputPath() {
        return outputDirectory.toPath();
    }

    private String markdown(List<Map<String, Object>> entities) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# TeruBase Seed Plan\n\n");
        markdown.append("- Scenario: ").append(scenario).append('\n');
        markdown.append("- Target row count: ").append(count).append('\n');
        markdown.append("- SQL dialect: ").append(dialect).append('\n');
        markdown.append("- Discovered entities: ").append(entities.size()).append("\n\n");
        markdown.append("## Insert Order Hints\n\n");
        for (String hint : INSERT_ORDER_HINTS) {
            markdown.append("- ").append(hint).append('\n');
        }
        markdown.append("\n## Entities\n\n");
        for (Map<String, Object> entity : entities) {
            appendEntity(markdown, entity);
        }
        markdown.append("\n## AI Usage\n\n");
        markdown.append("No AI provider was called to generate this plan.\n");
        return markdown.toString();
    }

    @SuppressWarnings("unchecked")
    private static void appendEntity(StringBuilder markdown, Map<String, Object> entity) {
        markdown.append("### ").append(entity.get("simpleName")).append("\n\n");
        markdown.append("- Class: `").append(entity.get("className")).append("`\n");
        markdown.append("- Table: `").append(entity.get("tableName")).append("`\n");
        markdown.append("- Insert order hint: ").append(entity.get("insertOrderHint")).append("\n");
        List<Map<String, Object>> fields = (List<Map<String, Object>>) entity.get("fields");
        if (fields != null && !fields.isEmpty()) {
            markdown.append("- Fields: ");
            markdown.append(fields.stream()
                    .map(field -> "`" + field.get("name") + "`")
                    .reduce((left, right) -> left + ", " + right)
                    .orElse(""));
            markdown.append('\n');
        }
        markdown.append('\n');
    }

    private static String insertOrderHint(List<Map<String, Object>> fields) {
        boolean child = fields.stream().anyMatch(field ->
                Objects.equals("ManyToOne", field.get("relationship"))
                        || Objects.equals("OneToOne", field.get("relationship"))
        );
        return child
                ? "Child entity with foreign-key relationships; insert after referenced parent entities."
                : "Parent or reference entity; insert before dependent child entities.";
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
