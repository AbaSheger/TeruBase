# TeruBase

TeruBase is a custom Spring Boot starter for local development workflows that need a safe, fast, disposable database playground.

It auto-configures an isolated H2 in-memory database, exposes local developer control endpoints, discovers JPA entity models from the host application, and can ask an OpenAI-compatible chat completion model to generate mock SQL seed data for the sandbox database.

> TeruBase is intended for local development and internal developer tooling only. Do not expose its endpoints publicly.

## Why TeruBase exists

When building Spring Boot applications, developers often need to:

- test SQL ideas without touching the real application database
- inspect JPA entity structure quickly
- seed realistic local mock data
- connect a small dashboard to a disposable database engine
- reset and experiment without breaking the actual app state

TeruBase gives you that workflow as a Spring Boot starter.

## Features

- Spring Boot 3.4+ auto-configuration
- Java 21 baseline
- isolated H2 in-memory database
- PostgreSQL compatibility mode for H2
- local REST API for status checks and SQL execution
- JPA entity discovery through classpath scanning
- relationship-aware entity metadata extraction
- OpenAI-compatible mock SQL generation
- transactional batch execution with rollback
- export-only SQL mode for CI, demos, and seed files
- CORS-enabled endpoints for standalone developer dashboards
- Java Util Logging throughout

## Positioning

TeruBase is not trying to replace Flyway, Liquibase, Testcontainers, H2 Console, or enterprise test-data-management platforms.

It is a Spring Boot-native developer experience layer for making local applications feel alive quickly.

The long-term direction is:

> Generate realistic, relationship-aware seed data from JPA entities for local development, frontend demos, onboarding, tests, and CI pipelines.

## Install locally

Clone the repository and install it into your local Maven cache:

```bash
mvn clean install
```

Then add it to another Spring Boot application:

```xml
<dependency>
    <groupId>com.terubase</groupId>
    <artifactId>terubase-spring-boot-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Configuration

TeruBase is enabled by default for straightforward local development, but you
should explicitly scope it to local/dev profiles.

```yaml
terubase:
  enabled: true
  base-package: com.example
  openai:
    endpoint: https://api.openai.com/v1/chat/completions
    model: gpt-4o
```

Disable it in production:

```yaml
terubase:
  enabled: false
```

TeruBase also blocks its auto-configuration automatically when either the
`prod` or `production` Spring profile is active. This prevents its local
developer endpoints from being exposed accidentally. An intentional override
requires the explicit property:

```yaml
terubase:
  force-enable-in-production: true
```

The isolated datasource uses:

```text
jdbc:h2:mem:terubase_isolated_db;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
```

## REST API

All endpoints are rooted under:

```text
/terubase/api
```

### Status

```http
GET /terubase/api/status
```

Returns metadata about the isolated database.

### Execute SQL

```http
POST /terubase/api/execute
Content-Type: application/json

{
  "sql": "select 1 as value"
}
```

For SELECT statements, TeruBase returns rows as ordered maps to preserve column sequence.

For mutation statements, TeruBase returns affected row counts.

### Discover JPA entities

```http
GET /terubase/api/entities
```

Returns discovered JPA entities, columns, IDs, generated values, and relationships such as:

- `@ManyToOne`
- `@OneToMany`
- `@OneToOne`
- `@ManyToMany`
- `@JoinColumn`
- `@JoinTable`

The metadata also includes column constraints, enum values, join-column and
join-table details, and deterministic insert-order hints for parent entities,
child entities with foreign keys, and join tables. The hints guide seed
generation; they are not a full database dependency planner.

### Scenario templates

```http
GET /terubase/api/scenarios
```

Returns built-in scenario templates that can guide realistic seed-data
generation. Fetch one template with `GET /terubase/api/scenarios/{id}`.

Available scenario IDs:

- `ecommerce-demo`
- `saas-billing-demo`
- `crm-demo`
- `banking-lite-demo`
- `task-management-demo`
- `qa-edge-cases`
- `frontend-dashboard-demo`

### Seed plan

```http
GET /terubase/api/seed-plan?scenarioId=saas-billing-demo&count=30
```

This metadata-only endpoint does not call AI. It creates an AI-ready
`schemaPrompt` from discovered JPA metadata and returns a
`recommendedMockRequest` that can be used with `POST /terubase/api/mock`.
`execute=false` remains the safe default.

### Export generated seed data safely

```http
POST /terubase/api/export/sql
Content-Type: application/json

{
  "statements": [
    "insert into customer (id, name) values (1, 'Sara')"
  ],
  "filename": "demo-seed.sql"
}
```

```http
POST /terubase/api/export/json
Content-Type: application/json

{
  "scenario": "SaaS billing demo",
  "statements": [
    "insert into customer (id, name) values (1, 'Sara')"
  ]
}
```

These endpoints return content for review, local seed files, CI fixtures, and
demos. They do not execute SQL or write files to disk. Only `INSERT` statements
are accepted; destructive SQL is blocked.

### Generate mock seed SQL

```http
POST /terubase/api/mock
Content-Type: application/json

{
  "count": 20,
  "apiKey": "sk-...",
  "schema": "Customer(id, name, email), Order(id, customer_id, total)",
  "scenario": "Generate a SaaS billing demo with overdue invoices and failed payments",
  "dialect": "postgresql",
  "execute": false
}
```

If `execute` is false, TeruBase returns export-ready SQL only.

If `execute` is true, TeruBase executes the generated SQL inside a transaction. If any statement fails, the transaction is rolled back.

## Example response

```json
{
  "scenario": "Generate a SaaS billing demo",
  "dialect": "postgresql",
  "executed": false,
  "statements": [
    "insert into customer (id, name, email) values (1, 'Sara Lind', 'sara@example.test')"
  ],
  "exportSql": "insert into customer ...;"
}
```

## Safety notes

TeruBase exposes powerful local developer endpoints. Treat it as local-only tooling.

Recommended safeguards:

- enable only under `local`, `dev`, or `test` profiles
- never expose `/terubase/api/**` publicly
- do not pass real production data to the AI endpoint
- prefer export-only mode when reviewing generated SQL
- keep the default production-profile guard enabled; do not set
  `terubase.force-enable-in-production=true` unless the exposure is intentional

## Open-source wedge and future cloud path

The open-source starter should focus on trust, adoption, and real developer workflow value.

Possible future hosted features:

- GitHub repository scanning
- team-shared demo data scenarios
- schema history and seed-data versioning
- CI/CD API
- GDPR-friendly anonymization workflows
- private scenario templates
- audit logs and workspace controls

## Build

```bash
mvn -B -ntp clean verify
```

## License

MIT License.
