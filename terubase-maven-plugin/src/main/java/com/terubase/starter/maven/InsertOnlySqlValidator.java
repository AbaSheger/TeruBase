package com.terubase.starter.maven;

import org.apache.maven.plugin.MojoExecutionException;

import java.util.ArrayList;
import java.util.List;

final class InsertOnlySqlValidator {

    private InsertOnlySqlValidator() {
    }

    static void validate(String sql) throws MojoExecutionException {
        if (sql == null || sql.isBlank()) {
            throw new MojoExecutionException("TeruBase SQL export is blank.");
        }

        List<String> statements = statementsWithoutComments(sql);
        if (statements.isEmpty()) {
            throw new MojoExecutionException("TeruBase SQL export does not contain any INSERT statements.");
        }

        for (String statement : statements) {
            String normalized = statement.stripLeading().toLowerCase();
            if (!normalized.startsWith("insert ")) {
                throw new MojoExecutionException("TeruBase SQL export only accepts INSERT statements.");
            }
        }
    }

    private static List<String> statementsWithoutComments(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;

        for (int i = 0; i < sql.length(); i++) {
            char value = sql.charAt(i);
            char next = i + 1 < sql.length() ? sql.charAt(i + 1) : '\0';

            if (inLineComment) {
                if (value == '\n' || value == '\r') {
                    inLineComment = false;
                    current.append('\n');
                }
                continue;
            }

            if (inBlockComment) {
                if (value == '*' && next == '/') {
                    inBlockComment = false;
                    i++;
                }
                continue;
            }

            if (!inSingleQuote && value == '-' && next == '-') {
                inLineComment = true;
                i++;
                continue;
            }

            if (!inSingleQuote && value == '/' && next == '*') {
                inBlockComment = true;
                i++;
                continue;
            }

            if (value == '\'') {
                current.append(value);
                if (inSingleQuote && next == '\'') {
                    current.append(next);
                    i++;
                } else {
                    inSingleQuote = !inSingleQuote;
                }
                continue;
            }

            if (!inSingleQuote && value == ';') {
                addStatement(statements, current);
                current.setLength(0);
                continue;
            }

            current.append(value);
        }

        addStatement(statements, current);
        return statements;
    }

    private static void addStatement(List<String> statements, StringBuilder current) {
        String statement = current.toString().strip();
        if (!statement.isEmpty()) {
            statements.add(statement);
        }
    }
}
