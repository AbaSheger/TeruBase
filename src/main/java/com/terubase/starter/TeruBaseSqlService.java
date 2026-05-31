package com.terubase.starter;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TeruBaseSqlService {

    private static final Logger LOGGER = Logger.getLogger(TeruBaseSqlService.class.getName());
    private final DataSource dataSource;

    public TeruBaseSqlService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Map<String, Object> status() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            Map<String, Object> status = new LinkedHashMap<>();
            status.put("name", "TeruBase");
            status.put("healthy", connection.isValid(2));
            status.put("timestamp", Instant.now().toString());
            status.put("databaseProductName", metaData.getDatabaseProductName());
            status.put("databaseProductVersion", metaData.getDatabaseProductVersion());
            status.put("driverName", metaData.getDriverName());
            status.put("driverVersion", metaData.getDriverVersion());
            status.put("url", metaData.getURL());
            status.put("transactionIsolation", connection.getTransactionIsolation());
            status.put("autoCommit", connection.getAutoCommit());
            return status;
        }
    }

    public SqlExecutionResult execute(String rawSql) throws SQLException {
        String sql = requireSql(rawSql);
        LOGGER.info(() -> "Executing TeruBase SQL block with length " + sql.length());
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            boolean hasResultSet = statement.execute(sql);
            if (hasResultSet) {
                try (ResultSet resultSet = statement.getResultSet()) {
                    return SqlExecutionResult.query(toRows(resultSet));
                }
            }
            int updateCount = statement.getUpdateCount();
            SQLWarning warning = statement.getWarnings();
            if (warning != null) {
                LOGGER.warning(() -> "TeruBase SQL warning: " + warning.getMessage());
            }
            return SqlExecutionResult.update(updateCount);
        }
    }

    public SqlExecutionResult executeBatchTransactionally(List<String> sqlStatements) throws SQLException {
        if (sqlStatements == null || sqlStatements.isEmpty()) {
            throw new IllegalArgumentException("At least one SQL statement is required.");
        }
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                for (String sql : sqlStatements) {
                    statement.addBatch(requireSql(sql));
                }
                int[] updateCounts = statement.executeBatch();
                connection.commit();
                return SqlExecutionResult.batch(updateCounts);
            } catch (SQLException | RuntimeException ex) {
                rollbackQuietly(connection, ex);
                throw ex;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    public static List<Map<String, Object>> toRows(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        List<Map<String, Object>> rows = new ArrayList<>();
        while (resultSet.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int index = 1; index <= columnCount; index++) {
                row.put(metaData.getColumnLabel(index), resultSet.getObject(index));
            }
            rows.add(row);
        }
        return rows;
    }

    public static String toExportSql(List<String> statements) {
        StringBuilder builder = new StringBuilder();
        for (String statement : statements) {
            String normalized = requireSql(statement);
            builder.append(normalized);
            if (!normalized.endsWith(";")) {
                builder.append(';');
            }
            builder.append(System.lineSeparator());
        }
        return builder.toString();
    }

    private static void rollbackQuietly(Connection connection, Exception rootCause) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            rootCause.addSuppressed(rollbackException);
            LOGGER.log(Level.SEVERE, "Rollback failed after TeruBase batch error", rollbackException);
        }
    }

    private static String requireSql(String rawSql) {
        if (rawSql == null || rawSql.isBlank()) {
            throw new IllegalArgumentException("SQL must not be blank.");
        }
        return rawSql.strip();
    }
}
