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

It also prints a ready-to-copy AI prompt and the expected SQL save path directly
in the terminal.

Write the SQL yourself or use any AI assistant to draft it from those
artifacts. After reviewing it, save it as
`target/terubase/generated-seed.sql` and export it to Spring Boot `data.sql`:

```bash
mvn -B -ntp terubase:export-data-sql
```

For a Flyway migration instead, run:

```bash
mvn -B -ntp terubase:export-flyway
```

TeruBase validates the SQL as `INSERT`-only and writes either:

```text
src/main/resources/data.sql
src/main/resources/db/migration/V999__terubase_seed_data.sql
```

## What TeruBase Adds

- Project metadata from compiled JPA entities
- Relationship and insert-order hints
- Enum, ID, column, and common relationship metadata
- Repeatable plan artifacts in `target/terubase`
- `INSERT`-only SQL validation
- Spring Boot `data.sql` and Flyway-ready export
- Local-first workflow that does not require production data
- Optional AI usage instead of an AI-only workflow

The Maven plugin does not generate row values or call an AI provider. The
optional runtime starter includes a separate OpenAI-compatible generation
endpoint.

## Positioning

Use plain AI when you need a quick draft.

Use TeruBase when you want a repeatable seed-data workflow for a Spring Boot/JPA
project.
