package com.terubase.starter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TeruBaseSchemaServiceTest {

    private List<Map<String, Object>> entities;

    @BeforeEach
    void setUp() {
        TeruBaseProperties properties = new TeruBaseProperties();
        properties.setEntityBasePackage("com.terubase.starter.fixture");
        entities = new TeruBaseSchemaService(new StaticApplicationContext(), properties).entities();
    }

    @Test
    void extractsEnumColumnIdAndTimestampMetadata() {
        Map<String, Object> account = entity("FixtureAccount");

        assertThat(field(account, "status"))
                .containsEntry("enum", true)
                .containsEntry("enumValues", List.of("ACTIVE", "SUSPENDED"))
                .containsEntry("enumerated", true)
                .containsEntry("enumStorage", "STRING");
        assertThat(column(account, "balance"))
                .containsEntry("precision", 12)
                .containsEntry("scale", 2)
                .containsEntry("insertable", false)
                .containsEntry("updatable", false);
        assertThat(field(account, "id"))
                .containsEntry("id", true)
                .containsEntry("generatedValue", true)
                .containsEntry("generationStrategy", "IDENTITY");
        assertThat(field(account, "createdAt")).containsEntry("creationTimestamp", true);
        assertThat(field(account, "updatedAt")).containsEntry("updateTimestamp", true);
    }

    @Test
    void extractsRelationshipAndJoinMetadata() {
        Map<String, Object> account = entity("FixtureAccount");
        Map<String, Object> purchase = entity("FixturePurchase");

        assertThat(relationship(account, "purchases"))
                .containsEntry("type", "OneToMany")
                .containsEntry("owningSide", false)
                .containsEntry("mappedBy", "account")
                .containsEntry("fetch", "LAZY")
                .containsEntry("cascade", List.of("ALL"));
        assertThat(relationship(purchase, "account"))
                .containsEntry("type", "ManyToOne")
                .containsEntry("owningSide", true)
                .containsEntry("optional", false)
                .containsEntry("fetch", "LAZY")
                .containsEntry("cascade", List.of("PERSIST"));
        assertThat(joinColumn(purchase, "account"))
                .containsEntry("name", "account_id")
                .containsEntry("insertable", false)
                .containsEntry("updatable", false);
        assertThat(joinTable(purchase, "labels"))
                .containsEntry("name", "fixture_purchase_label")
                .containsEntry("joinColumns", List.of("purchase_id"))
                .containsEntry("inverseJoinColumns", List.of("label_id"));
    }

    @Test
    void addsDeterministicInsertOrderHints() {
        assertThat(entity("FixtureAccount").get("insertOrderHint"))
                .isEqualTo("Parent or reference entity; insert before dependent child entities.");
        assertThat(entity("FixturePurchase").get("insertOrderHint"))
                .isEqualTo("Contains join-table metadata; populate join tables last.");
    }

    private Map<String, Object> entity(String simpleName) {
        return entities.stream()
                .filter(entity -> simpleName.equals(entity.get("simpleName")))
                .findFirst()
                .orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> field(Map<String, Object> entity, String name) {
        return ((List<Map<String, Object>>) entity.get("fields")).stream()
                .filter(field -> name.equals(field.get("name")))
                .findFirst()
                .orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> relationship(Map<String, Object> entity, String fieldName) {
        return ((List<Map<String, Object>>) field(entity, fieldName).get("relationships")).getFirst();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> column(Map<String, Object> entity, String fieldName) {
        return (Map<String, Object>) field(entity, fieldName).get("column");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> joinColumn(Map<String, Object> entity, String fieldName) {
        return (Map<String, Object>) field(entity, fieldName).get("joinColumn");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> joinTable(Map<String, Object> entity, String fieldName) {
        return (Map<String, Object>) field(entity, fieldName).get("joinTable");
    }
}
