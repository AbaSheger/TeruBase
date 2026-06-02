package com.terubase.starter;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TeruBaseExportServiceTest {

    private final TeruBaseExportService service = new TeruBaseExportService(
            Clock.fixed(Instant.parse("2026-06-02T12:30:00Z"), ZoneOffset.UTC)
    );

    @Test
    void exportsNormalizedSemicolonTerminatedSqlWithDefaultFilename() {
        SqlExportResponse response = service.exportSql(request(
                "insert into customer (id, name) values (1, 'Sara')",
                " insert into invoice (id, amount) values (1, 499.00); "
        ));

        assertThat(response.filename()).isEqualTo("terubase-seed.sql");
        assertThat(response.contentType()).isEqualTo("text/plain");
        assertThat(response.content()).isEqualTo(String.join(
                System.lineSeparator(),
                "insert into customer (id, name) values (1, 'Sara');",
                "insert into invoice (id, amount) values (1, 499.00);",
                ""
        ));
        assertThat(response.statementCount()).isEqualTo(2);
    }

    @Test
    void appendsSqlExtensionToCustomFilename() {
        SqlExportResponse response = service.exportSql(new ExportRequest(
                null,
                List.of("insert into customer (id) values (1)"),
                "demo-seed"
        ));

        assertThat(response.filename()).isEqualTo("demo-seed.sql");
    }

    @Test
    void exportsJsonWithDefaultFilenameAndReviewMetadata() {
        JsonExportResponse response = service.exportJson(new ExportRequest(
                "SaaS billing demo",
                List.of("insert into customer (id, name) values (1, 'Sara')"),
                null
        ));

        assertThat(response.filename()).isEqualTo("terubase-seed.json");
        assertThat(response.contentType()).isEqualTo("application/json");
        assertThat(response.scenario()).isEqualTo("SaaS billing demo");
        assertThat(response.statementCount()).isEqualTo(1);
        assertThat(response.statements()).containsExactly("insert into customer (id, name) values (1, 'Sara')");
        assertThat(response.metadata())
                .containsEntry("generatedBy", "TeruBase")
                .containsEntry("exportedAt", "2026-06-02T12:30:00Z")
                .containsEntry("safeForReview", true);
    }

    @Test
    void appendsJsonExtensionToCustomFilename() {
        JsonExportResponse response = service.exportJson(new ExportRequest(
                null,
                List.of("insert into customer (id) values (1)"),
                "demo-seed"
        ));

        assertThat(response.filename()).isEqualTo("demo-seed.json");
    }

    @Test
    void rejectsBlankStatements() {
        assertThatThrownBy(() -> service.exportSql(request("  ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("SQL statements must not be blank.");
    }

    @Test
    void rejectsUpdateStatements() {
        assertRejected("update customer set name = 'Adam' where id = 1");
    }

    @Test
    void rejectsDeleteStatements() {
        assertRejected("delete from customer where id = 1");
    }

    @Test
    void rejectsDropStatements() {
        assertRejected("drop table customer");
    }

    @Test
    void rejectsAdditionalCommandAfterInsert() {
        assertThatThrownBy(() -> service.exportSql(request(
                "insert into customer (id) values (1); delete from customer"
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Each item must contain exactly one INSERT statement.");
    }

    @Test
    void acceptsMixedCaseInsertStatementsAndSemicolonsInsideValues() {
        JsonExportResponse response = service.exportJson(request(
                "InSeRt into note (id, text) values (1, 'Review; then approve')"
        ));

        assertThat(response.statements()).containsExactly(
                "InSeRt into note (id, text) values (1, 'Review; then approve')"
        );
    }

    private void assertRejected(String statement) {
        assertThatThrownBy(() -> service.exportSql(request(statement)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only INSERT statements are accepted.");
    }

    private static ExportRequest request(String... statements) {
        return new ExportRequest(null, List.of(statements), null);
    }
}
