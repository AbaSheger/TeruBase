# AGENTS.md

## Project

TeruBase provides a Maven workflow that inspects compiled, field-annotated JPA
entities, writes schema-context and seed-plan artifacts, checks reviewed SQL
for INSERT-only statements, and copies that SQL to Spring Boot `data.sql` or a
Flyway migration path. The optional Spring Boot starter provides richer
metadata inspection, OpenAI-compatible SQL generation, export responses, and
isolated H2 execution.

Core promise:

> From compiled JPA entities to reviewable planning artifacts and SQL exports.

## Stack

- Java 21
- Spring Boot 3.4+
- Maven
- Jakarta Persistence / JPA
- H2 for isolated local execution
- Jackson for JSON
- Java `HttpClient` for OpenAI-compatible API calls
- `java.util.logging.Logger`
- JUnit 5 and AssertJ

## Engineering Rules

- Write readable, modern, testable Java.
- Prefer small cohesive classes and narrowly scoped changes.
- Use Java records for DTOs where appropriate.
- Use constructor injection.
- Use try-with-resources for JDBC resources.
- Keep package names under `com.terubase.starter`.
- Keep REST endpoints under `/terubase/api`.
- Keep `@CrossOrigin(origins = "*")` on developer dashboard endpoints.
- Add focused tests for behavioral changes.
- Avoid huge files, unrelated refactors, placeholder code, pseudo-code, and
  `TODO` comments unless explicitly requested.

## Do Not Overbuild

- Do not introduce Lombok.
- Do not introduce unnecessary frameworks.
- Do not downgrade Java or Spring Boot.
- Do not replace Maven with Gradle.
- Do not add a frontend unless explicitly asked.
- Do not build enterprise test-data-management features yet.

## Safety Rules

- Never log API keys.
- Never store API keys.
- Do not send real production data in examples or AI requests.
- AI-generated SQL must default to export-only mode.
- Accept only `INSERT` statements from AI-generated output.
- Block destructive SQL from AI-generated output.
- Roll back the entire batch when execution fails.
- Block risky endpoints in `prod` and `production` profiles unless explicitly
  force-enabled.
- Keep TeruBase execution isolated from the host application's datasource.

## Current Priority

Make TeruBase worth trying by adding:

1. Scenario templates
2. An AI-ready seed-plan endpoint
3. Safe SQL export endpoints
4. Improved JPA metadata
5. A local-only profile guard
6. Tests
7. Clear README examples

## Verification

Run the full verification command after implementation work:

```bash
mvn -B -ntp clean verify
```

If it cannot run, explain exactly why.
