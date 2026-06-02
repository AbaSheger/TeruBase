package com.terubase.starter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SqlSafetyValidator {

    private SqlSafetyValidator() {
    }

    public static List<String> normalizeInsertStatements(List<String> statements) {
        if (statements == null || statements.isEmpty()) {
            throw new IllegalArgumentException("At least one SQL statement is required.");
        }

        List<String> normalized = new ArrayList<>();
        for (String statement : statements) {
            normalized.add(normalizeInsertStatement(statement));
        }
        return List.copyOf(normalized);
    }

    private static String normalizeInsertStatement(String rawStatement) {
        if (rawStatement == null || rawStatement.isBlank()) {
            throw new IllegalArgumentException("SQL statements must not be blank.");
        }

        String statement = rawStatement.strip();
        if (!startsWithInsert(statement)) {
            throw new IllegalArgumentException("Only INSERT statements are accepted.");
        }

        int terminator = findUnquotedSemicolon(statement);
        if (terminator >= 0) {
            String remaining = statement.substring(terminator + 1).strip();
            if (!remaining.isEmpty()) {
                throw new IllegalArgumentException("Each item must contain exactly one INSERT statement.");
            }
            statement = statement.substring(0, terminator).strip();
        }

        return statement;
    }

    private static boolean startsWithInsert(String statement) {
        String lowerCase = statement.toLowerCase(Locale.ROOT);
        return lowerCase.startsWith("insert")
                && (statement.length() == "insert".length()
                || Character.isWhitespace(statement.charAt("insert".length())));
    }

    private static int findUnquotedSemicolon(String statement) {
        boolean quoted = false;
        for (int index = 0; index < statement.length(); index++) {
            char current = statement.charAt(index);
            if (current == '\'') {
                if (quoted && index + 1 < statement.length() && statement.charAt(index + 1) == '\'') {
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == ';' && !quoted) {
                return index;
            }
        }
        return -1;
    }
}
