# ChatGPT vs TeruBase

ChatGPT, Claude, Cursor, and Copilot are useful for drafting seed SQL. If you
paste a schema and ask for example rows, they can often produce reasonable
`INSERT` statements.

TeruBase is not trying to replace that. It adds the repeatable Spring Boot/JPA
workflow around seed data.

## Plain AI Prompt

```text
Generate seed INSERT statements for customers, invoices, and payments.
Use realistic demo data.
```

This can be enough for a one-off script, but the result depends on what you
remember to paste into the prompt. It may miss entity relationships, enum
values, generated IDs, nullability, unique fields, or migration workflow details.

## TeruBase Workflow

```bash
mvn -B -ntp compile terubase:plan
```

TeruBase scans compiled JPA entities and writes:

```text
target/terubase/schema-context.json
target/terubase/seed-plan.md
```

After seed SQL is reviewed, export it:

```bash
mvn -B -ntp terubase:export-flyway
```

TeruBase validates the SQL as `INSERT`-only and writes:

```text
src/main/resources/db/migration/V999__terubase_seed_data.sql
```

## What TeruBase Adds

- Project metadata from compiled JPA entities
- Relationship and insert-order hints
- Enum, ID, column, and join metadata
- Repeatable plan artifacts in `target/terubase`
- `INSERT`-only SQL validation
- Flyway-ready export
- Local-first workflow that does not require production data
- Optional AI usage instead of an AI-only workflow

## Positioning

Use plain AI when you need a quick draft.

Use TeruBase when you want a repeatable seed-data workflow for a Spring Boot/JPA
project.
