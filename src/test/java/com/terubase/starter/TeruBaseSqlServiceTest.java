package com.terubase.starter;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class TeruBaseSqlServiceTest {

    @Test
    void executesQueryAndPreservesColumnOrder() throws SQLException {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:terubase_test_db;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        TeruBaseSqlService service = new TeruBaseSqlService(dataSource);

        SqlExecutionResult result = service.execute("select 1 as id, 'TeruBase' as name");

        assertThat(result.query()).isTrue();
        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().getFirst().keySet()).containsExactly("ID", "NAME");
        assertThat(result.rows().getFirst()).containsEntry("ID", 1).containsEntry("NAME", "TeruBase");
    }

    @Test
    void executesBatchTransactionally() throws SQLException {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:terubase_batch_test_db;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        TeruBaseSqlService service = new TeruBaseSqlService(dataSource);
        service.execute("create table customer (id int primary key, name varchar(255))");

        SqlExecutionResult result = service.executeBatchTransactionally(java.util.List.of(
                "insert into customer (id, name) values (1, 'Sara')",
                "insert into customer (id, name) values (2, 'Adam')"
        ));

        assertThat(result.batchUpdateCounts()).containsExactly(1, 1);
        assertThat(service.execute("select * from customer order by id").rows()).hasSize(2);
    }
}
