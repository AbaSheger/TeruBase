# AI Coding Assistants and TeruBase

Codex, Copilot, Cursor, Claude, ChatGPT, and similar tools can draft seed SQL
from project context. TeruBase is designed to provide that context and manage
the repeatable steps around the draft.

The tools have different responsibilities: an AI assistant can author row
values, while TeruBase inspects the project, creates planning artifacts, checks
the reviewed SQL as `INSERT`-only, and copies it to a conventional output path.

## Using an Assistant Alone

```text
Generate seed INSERT statements for customers, invoices, and payments.
Use realistic demo data.
```

This can be enough for a one-off script, but the available context depends on
what the developer provides during that session. The prompt and result are not
automatically tied to a repeatable project artifact.

## TeruBase Workflow

```bash
mvn -B -ntp compile terubase:plan
```

TeruBase scans compiled JPA entities and writes:

```text
target/terubase/schema-context.json
target/terubase/seed-plan.md
```

It also prints the files an AI tool must be able to read, a short prompt, and the
expected SQL save path directly in the terminal.

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

TeruBase checks that the SQL contains only `INSERT` statements and writes
either:

```text
src/main/resources/data.sql
src/main/resources/db/migration/V999__terubase_seed_data.sql
```

## Division of Responsibilities

An AI coding assistant can:

- draft realistic row values and `INSERT` statements
- adapt a draft to a scenario or demo requirement
- revise SQL after developer feedback

TeruBase can:

- inspect selected field annotations from compiled JPA entities
- record basic relationship and insert-order hints
- record Java enums, IDs, generated values, explicit `@Table`/`@Column`
  metadata, and common relationship annotation types
- write repeatable plan artifacts under `target/terubase`
- check reviewed SQL as `INSERT`-only
- copy reviewed SQL to Spring Boot `data.sql` or a fixed Flyway migration path
- operate without an AI provider when SQL is supplied manually

The Maven plugin does not generate row values or call an AI provider. The
optional runtime starter includes a separate OpenAI-compatible generation
endpoint.

The plugin does not apply Hibernate naming strategies, inspect property-access
mappings, exclude `@Transient` fields, aggregate entities from child Maven
modules, build a full foreign-key graph, or validate SQL against a database. It
recognizes Jakarta Persistence annotations, not legacy `javax.persistence`
annotations.

## When to Use Each

Use an AI coding assistant alone when a one-off SQL draft is sufficient.

Use TeruBase with or without an assistant when you want reviewable project
artifacts and a repeatable seed-data workflow for a Spring Boot/JPA project.
