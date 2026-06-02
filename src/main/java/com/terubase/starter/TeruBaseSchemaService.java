package com.terubase.starter;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TeruBaseSchemaService {

    private static final Logger LOGGER = Logger.getLogger(TeruBaseSchemaService.class.getName());

    private final ApplicationContext applicationContext;
    private final TeruBaseProperties properties;

    public TeruBaseSchemaService(ApplicationContext applicationContext, TeruBaseProperties properties) {
        this.applicationContext = applicationContext;
        this.properties = properties;
    }

    public List<Map<String, Object>> entities() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));

        Set<String> entityClassNames = new LinkedHashSet<>();
        for (String basePackage : scanPackages()) {
            scanner.findCandidateComponents(basePackage).forEach(component -> entityClassNames.add(component.getBeanClassName()));
        }

        return entityClassNames.stream()
                .sorted()
                .map(this::describeEntity)
                .toList();
    }

    private Set<String> scanPackages() {
        Set<String> packages = new LinkedHashSet<>();
        if (StringUtils.hasText(properties.getEntityBasePackage())) {
            packages.add(properties.getEntityBasePackage());
            return packages;
        }
        for (String beanName : applicationContext.getBeanDefinitionNames()) {
            try {
                Class<?> type = applicationContext.getType(beanName);
                if (type != null && type.getPackageName() != null && !type.getPackageName().startsWith("org.springframework")) {
                    packages.add(type.getPackageName());
                }
            } catch (RuntimeException ex) {
                LOGGER.log(Level.FINE, "Could not inspect bean package for " + beanName, ex);
            }
        }
        if (packages.isEmpty()) {
            packages.add("com");
        }
        return packages;
    }

    private Map<String, Object> describeEntity(String className) {
        Map<String, Object> model = new LinkedHashMap<>();
        try {
            Class<?> entityClass = Class.forName(className);
            Entity entity = entityClass.getAnnotation(Entity.class);
            Table table = entityClass.getAnnotation(Table.class);
            model.put("className", entityClass.getName());
            model.put("simpleName", entityClass.getSimpleName());
            model.put("entityName", entity.name().isBlank() ? entityClass.getSimpleName() : entity.name());
            model.put("tableName", table == null || table.name().isBlank() ? entityClass.getSimpleName() : table.name());
            List<Map<String, Object>> fields = describeFields(entityClass);
            model.put("fields", fields);
            model.put("insertOrderHint", insertOrderHint(fields));
        } catch (ClassNotFoundException ex) {
            LOGGER.log(Level.WARNING, "Entity class could not be loaded: " + className, ex);
            model.put("className", className);
            model.put("error", ex.getMessage());
        }
        return model;
    }

    private static List<Map<String, Object>> describeFields(Class<?> entityClass) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = entityClass;
        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields.stream()
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .sorted(Comparator.comparing(Field::getName))
                .map(TeruBaseSchemaService::describeField)
                .toList();
    }

    private static Map<String, Object> describeField(Field field) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", field.getName());
        item.put("type", field.getType().getTypeName());
        item.put("id", field.isAnnotationPresent(Id.class));
        addGeneratedValue(item, field);
        addEnumMetadata(item, field);
        addTimestampMetadata(item, field);

        Column column = field.getAnnotation(Column.class);
        if (column != null) {
            item.put("column", describeColumn(field, column));
        }

        List<Map<String, Object>> relationships = new ArrayList<>();
        addRelationship(field, relationships, field.getAnnotation(ManyToOne.class));
        addRelationship(field, relationships, field.getAnnotation(OneToMany.class));
        addRelationship(field, relationships, field.getAnnotation(OneToOne.class));
        addRelationship(field, relationships, field.getAnnotation(ManyToMany.class));
        if (!relationships.isEmpty()) {
            item.put("relationships", relationships);
        }

        JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
        if (joinColumn != null) {
            item.put("joinColumn", describeJoinColumn(joinColumn));
        }

        JoinTable joinTable = field.getAnnotation(JoinTable.class);
        if (joinTable != null) {
            item.put("joinTable", describeJoinTable(joinTable));
        }
        return item;
    }

    private static void addGeneratedValue(Map<String, Object> item, Field field) {
        GeneratedValue generatedValue = field.getAnnotation(GeneratedValue.class);
        item.put("generatedValue", generatedValue != null);
        if (generatedValue != null) {
            item.put("generationStrategy", generatedValue.strategy().name());
        }
    }

    private static void addEnumMetadata(Map<String, Object> item, Field field) {
        if (field.getType().isEnum()) {
            item.put("enum", true);
            item.put("enumValues", Arrays.stream(field.getType().getEnumConstants())
                    .map(value -> ((Enum<?>) value).name())
                    .toList());
        }
        Enumerated enumerated = field.getAnnotation(Enumerated.class);
        if (enumerated != null) {
            item.put("enumerated", true);
            item.put("enumStorage", enumStorage(enumerated));
        }
    }

    private static String enumStorage(Enumerated enumerated) {
        EnumType value = enumerated.value();
        return value == null ? EnumType.ORDINAL.name() : value.name();
    }

    private static void addTimestampMetadata(Map<String, Object> item, Field field) {
        if (hasAnnotation(field, "org.hibernate.annotations.CreationTimestamp")) {
            item.put("creationTimestamp", true);
        }
        if (hasAnnotation(field, "org.hibernate.annotations.UpdateTimestamp")) {
            item.put("updateTimestamp", true);
        }
    }

    private static boolean hasAnnotation(Field field, String annotationClassName) {
        return Arrays.stream(field.getAnnotations())
                .map(Annotation::annotationType)
                .map(Class::getName)
                .anyMatch(annotationClassName::equals);
    }

    private static Map<String, Object> describeColumn(Field field, Column column) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("name", column.name().isBlank() ? field.getName() : column.name());
        metadata.put("nullable", column.nullable());
        metadata.put("unique", column.unique());
        metadata.put("length", column.length());
        metadata.put("precision", column.precision());
        metadata.put("scale", column.scale());
        metadata.put("insertable", column.insertable());
        metadata.put("updatable", column.updatable());
        return metadata;
    }

    private static void addRelationship(Field field, List<Map<String, Object>> relationships, ManyToOne annotation) {
        if (annotation != null) {
            relationships.add(describeRelationship(
                    field, ManyToOne.class.getSimpleName(), true, "", annotation.optional(),
                    annotation.fetch().name(), cascadeNames(annotation.cascade())
            ));
        }
    }

    private static void addRelationship(Field field, List<Map<String, Object>> relationships, OneToMany annotation) {
        if (annotation != null) {
            relationships.add(describeRelationship(
                    field, OneToMany.class.getSimpleName(), annotation.mappedBy().isBlank(),
                    annotation.mappedBy(), null, annotation.fetch().name(), cascadeNames(annotation.cascade())
            ));
        }
    }

    private static void addRelationship(Field field, List<Map<String, Object>> relationships, OneToOne annotation) {
        if (annotation != null) {
            relationships.add(describeRelationship(
                    field, OneToOne.class.getSimpleName(), annotation.mappedBy().isBlank(),
                    annotation.mappedBy(), annotation.optional(), annotation.fetch().name(),
                    cascadeNames(annotation.cascade())
            ));
        }
    }

    private static void addRelationship(Field field, List<Map<String, Object>> relationships, ManyToMany annotation) {
        if (annotation != null) {
            relationships.add(describeRelationship(
                    field, ManyToMany.class.getSimpleName(), annotation.mappedBy().isBlank(),
                    annotation.mappedBy(), null, annotation.fetch().name(), cascadeNames(annotation.cascade())
            ));
        }
    }

    private static Map<String, Object> describeRelationship(
            Field field,
            String type,
            boolean owningSide,
            String mappedBy,
            Boolean optional,
            String fetch,
            List<String> cascades
    ) {
        Map<String, Object> relationship = new LinkedHashMap<>();
        relationship.put("type", type);
        relationship.put("field", field.getName());
        relationship.put("targetType", field.getGenericType().getTypeName());
        relationship.put("owningSide", owningSide);
        if (!mappedBy.isBlank()) {
            relationship.put("mappedBy", mappedBy);
        }
        if (optional != null) {
            relationship.put("optional", optional);
        }
        relationship.put("fetch", fetch);
        relationship.put("cascade", cascades);
        return relationship;
    }

    private static List<String> cascadeNames(jakarta.persistence.CascadeType[] cascades) {
        return Arrays.stream(cascades)
                .map(Enum::name)
                .toList();
    }

    private static Map<String, Object> describeJoinColumn(JoinColumn joinColumn) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("name", joinColumn.name());
        metadata.put("referencedColumnName", joinColumn.referencedColumnName());
        metadata.put("nullable", joinColumn.nullable());
        metadata.put("unique", joinColumn.unique());
        metadata.put("insertable", joinColumn.insertable());
        metadata.put("updatable", joinColumn.updatable());
        return metadata;
    }

    private static Map<String, Object> describeJoinTable(JoinTable joinTable) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("name", joinTable.name());
        metadata.put("joinColumns", Arrays.stream(joinTable.joinColumns())
                .map(JoinColumn::name)
                .toList());
        metadata.put("inverseJoinColumns", Arrays.stream(joinTable.inverseJoinColumns())
                .map(JoinColumn::name)
                .toList());
        return metadata;
    }

    private static String insertOrderHint(List<Map<String, Object>> fields) {
        if (fields.stream().anyMatch(field -> field.containsKey("joinTable"))) {
            return "Contains join-table metadata; populate join tables last.";
        }
        if (fields.stream().anyMatch(TeruBaseSchemaService::hasForeignKeyRelationship)) {
            return "Child entity with foreign-key relationships; insert after referenced parent entities.";
        }
        return "Parent or reference entity; insert before dependent child entities.";
    }

    @SuppressWarnings("unchecked")
    private static boolean hasForeignKeyRelationship(Map<String, Object> field) {
        List<Map<String, Object>> relationships = (List<Map<String, Object>>) field.get("relationships");
        if (relationships == null) {
            return false;
        }
        return relationships.stream()
                .anyMatch(relationship -> List.of("ManyToOne", "OneToOne").contains(relationship.get("type"))
                        && Boolean.TRUE.equals(relationship.get("owningSide")));
    }

}
