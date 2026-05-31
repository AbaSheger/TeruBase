package com.terubase.starter;

import jakarta.persistence.Column;
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
            model.put("fields", describeFields(entityClass));
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
        item.put("generatedValue", field.isAnnotationPresent(GeneratedValue.class));

        Column column = field.getAnnotation(Column.class);
        if (column != null) {
            Map<String, Object> columnMeta = new LinkedHashMap<>();
            columnMeta.put("name", column.name().isBlank() ? field.getName() : column.name());
            columnMeta.put("nullable", column.nullable());
            columnMeta.put("unique", column.unique());
            columnMeta.put("length", column.length());
            item.put("column", columnMeta);
        }

        List<Map<String, Object>> relationships = new ArrayList<>();
        addRelationship(field, relationships, ManyToOne.class.getSimpleName(), field.getAnnotation(ManyToOne.class));
        addRelationship(field, relationships, OneToMany.class.getSimpleName(), field.getAnnotation(OneToMany.class));
        addRelationship(field, relationships, OneToOne.class.getSimpleName(), field.getAnnotation(OneToOne.class));
        addRelationship(field, relationships, ManyToMany.class.getSimpleName(), field.getAnnotation(ManyToMany.class));
        if (!relationships.isEmpty()) {
            item.put("relationships", relationships);
        }

        JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
        if (joinColumn != null) {
            item.put("joinColumn", Map.of(
                    "name", joinColumn.name(),
                    "referencedColumnName", joinColumn.referencedColumnName(),
                    "nullable", joinColumn.nullable(),
                    "unique", joinColumn.unique()
            ));
        }

        JoinTable joinTable = field.getAnnotation(JoinTable.class);
        if (joinTable != null) {
            item.put("joinTable", Map.of("name", joinTable.name()));
        }
        return item;
    }

    private static void addRelationship(Field field, List<Map<String, Object>> relationships, String type, Object annotation) {
        if (annotation == null) {
            return;
        }
        Map<String, Object> relationship = new LinkedHashMap<>();
        relationship.put("type", type);
        relationship.put("field", field.getName());
        relationship.put("targetType", field.getGenericType().getTypeName());
        relationships.add(relationship);
    }
}
