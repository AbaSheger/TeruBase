<h1 align="center">TeruBase</h1>

<p align="center">
  <img src="docs/assets/TeruBase.png" alt="TeruBase logo" width="220">
</p>

<p align="center">
  A repeatable seed-data workflow for Spring Boot/JPA projects.
</p>

<p align="center">
  <img alt="Java 21" src="https://img.shields.io/badge/Java-21-blue">
  <img alt="Spring Boot 3.4+" src="https://img.shields.io/badge/Spring%20Boot-3.4%2B-brightgreen">
  <img alt="Maven" src="https://img.shields.io/badge/Maven-build-orange">
  <img alt="MIT License" src="https://img.shields.io/badge/License-MIT-green">
  <a href="https://central.sonatype.com/artifact/io.github.abasheger/terubase-maven-plugin/0.1.1">
    <img alt="Maven Central" src="https://img.shields.io/badge/Maven%20Central-0.1.1-blue">
  </a>
  <a href="https://github.com/AbaSheger/TeruBase/actions/workflows/maven.yml">
    <img alt="Build status" src="https://github.com/AbaSheger/TeruBase/actions/workflows/maven.yml/badge.svg">
  </a>
</p>

TeruBase turns Spring Boot/JPA entity metadata into a repeatable seed-data
workflow: scan entities, create a seed plan, validate `INSERT`-only SQL, and
export Spring Boot `data.sql` or Flyway-ready files for local development,
demos, QA scenarios, and CI fixtures.

> From JPA entities to realistic runnable seed data in minutes.

The preferred path is the Maven plugin. It discovers JPA entities at build time
and writes reviewable artifacts without requiring an AI account:

```xml
<plugin>
  <groupId>io.github.abasheger</groupId>
  <artifactId>terubase-maven-plugin</artifactId>
  <version>0.1.1</version>
</plugin>
```

```bash
mvn -B -ntp compile terubase:plan
```

```text
target/terubase/schema-context.json
target/terubase/seed-plan.md
```

The command also prints a ready-made AI prompt, the exact path where the SQL
response should be saved, and the available export commands.

The plugin does not generate row values or call an AI provider. Create
`target/terubase/generated-seed.sql` yourself or with an AI assistant, review
it, then export it as Spring Boot's familiar `data.sql`:

```bash
mvn -B -ntp terubase:export-data-sql
```

```text
src/main/resources/data.sql
```

If Hibernate creates your schema with `spring.jpa.hibernate.ddl-auto`, also set
`spring.jpa.defer-datasource-initialization=true` so Spring runs `data.sql`
after the tables exist.

Projects using Flyway can instead run `mvn -B -ntp terubase:export-flyway`.

TeruBase is local-first tooling. It is not a generic fake-data generator, an H2
console clone, a production database API, or an enterprise test-data-management
platform.

The runtime starter remains available as an optional local playground. TeruBase
complements Flyway and Liquibase by preparing seed artifacts for review; it does
not replace migration tools.

## Why TeruBase?

- Build a safer, repeatable workflow around Spring Boot `data.sql`.
- Generate relationship-aware seed plans from real JPA entities.
- Keep seed SQL reviewable before it reaches Flyway or CI.
- Block destructive SQL and accept only `INSERT` statements.
- Make local apps and demos look realistic without production data.
- Complement Flyway and Liquibase instead of replacing them.

## Why Not Just ChatGPT?

ChatGPT, Claude, Cursor, and Copilot can write example `INSERT` statements.
That is useful, but it is not the whole workflow.

TeruBase adds the project-specific parts around generation:

- scans your compiled Spring Boot/JPA model
- captures tables, columns, enums, IDs, and common relationship types
- creates a reusable seed plan from that metadata
- validates reviewed SQL as `INSERT`-only
- exports seed data into `data.sql` or a Flyway migration
- keeps AI optional and export-first

Use an AI assistant if you want help drafting SQL. Use TeruBase when you want a
repeatable Spring/JPA workflow around that SQL.

See [ChatGPT vs TeruBase](docs/CHATGPT_VS_TERUBASE.md) for a linkable comparison.

## Try It in 5 Minutes

TeruBase requires Java 21 and Spring Boot 3.4+.

Add the Maven plugin to a Spring Boot/JPA project's `pom.xml`:

```xml
<build>
  <plugins>
    <plugin>
      <groupId>io.github.abasheger</groupId>
      <artifactId>terubase-maven-plugin</artifactId>
      <version>0.1.1</version>
      <configuration>
        <entityBasePackage>com.example.yourapp</entityBasePackage>
      </configuration>
    </plugin>
  </plugins>
</build>
```

Generate non-AI seed-plan artifacts from the project metadata:

```bash
mvn -B -ntp compile terubase:plan
```

This writes:

```text
target/terubase/schema-context.json
target/terubase/seed-plan.md
```

The terminal then prints a ready-to-copy prompt similar to:

```text
Generate INSERT-only seed SQL using target/terubase/schema-context.json and target/terubase/seed-plan.md. Follow the relationships, constraints, row count, and SQL dialect in those files. Return SQL only.
```

To copy a reviewed `target/terubase/generated-seed.sql` file into Spring Boot's
`data.sql`:

```bash
mvn -B -ntp terubase:export-data-sql
```

This writes:

```text
src/main/resources/data.sql
```

For applications where Hibernate creates the schema, configure:

```yaml
spring:
  jpa:
    defer-datasource-initialization: true
```

For a Flyway migration instead:

```bash
mvn -B -ntp terubase:export-flyway
```

AI generation is not built into the Maven plugin. You can use any AI assistant
to draft SQL from the generated artifacts, or use the optional runtime starter's
OpenAI-compatible endpoint.

### Example Output

A compact `schema-context.json` looks like this:

```json
{
  "entities": [
    {
      "className": "com.example.invoice.Customer",
      "simpleName": "Customer",
      "tableName": "customers",
      "fields": [
        {
          "name": "id",
          "type": "java.lang.Long",
          "id": true,
          "generatedValue": true
        },
        {
          "name": "email",
          "type": "java.lang.String",
          "column": {
            "name": "email",
            "nullable": false,
            "unique": true
          }
        }
      ],
      "insertOrderHint": "Parent or reference entity; insert before dependent child entities."
    }
  ]
}
```

The matching `seed-plan.md` summarizes the workflow:

```markdown
# TeruBase Seed Plan

- Scenario: Generate realistic relationship-aware local development seed data.
- Target row count: 20
- SQL dialect: h2-postgresql-mode
- Discovered entities: 3

## Insert Order Hints

- Insert parent and reference tables first.
- Insert child tables with foreign keys second.
- Insert join tables last.
- Populate every nullable=false field.
```

After review, `terubase:export-data-sql` copies validated SQL into `data.sql`:

```sql
-- TeruBase seed data
INSERT INTO customers (id, name, email) VALUES (1, 'Northstar Studio', 'billing@northstar.example');
INSERT INTO invoices (id, customer_id, invoice_number, total_amount) VALUES (10, 1, 'INV-2026-0001', 1200.00);
```

### Optional Starter Playground

Add the starter to a Spring Boot application when you want the local runtime
playground endpoints:


```xml
<dependency>
    <groupId>io.github.abasheger</groupId>
    <artifactId>terubase-spring-boot-starter</artifactId>
    <version>0.1.1</version>
</dependency>
```

Add local-only configuration to `application-local.yml`:

```yaml
terubase:
  enabled: true
  entity-base-package: com.example.store.domain
  sql-execution-enabled: false
```

Start the application with the `local` profile, then inspect the built-in
scenarios:

```bash
curl http://localhost:8080/terubase/api/scenarios
```

Generate a seed plan from your discovered JPA entities:

```bash
curl "http://localhost:8080/terubase/api/seed-plan?scenarioId=saas-billing-demo&count=30"
```

The seed-plan response is metadata-only. It does not call AI or execute SQL.
Copy its `recommendedMockRequest` into `POST /terubase/api/mock` when you want
AI-generated SQL. The generated request keeps `execute=false`.

## Try the Invoice Demo

Generate Maven plugin artifacts from the invoice demo:

```bash
cd examples/invoice-demo
mvn -B -ntp compile terubase:plan
```

This writes:

```text
examples/invoice-demo/target/terubase/schema-context.json
examples/invoice-demo/target/terubase/seed-plan.md
```

To export reviewed SQL to `data.sql`, place reviewed `INSERT` statements in
`examples/invoice-demo/target/terubase/generated-seed.sql`, then run:

```bash
mvn -B -ntp terubase:export-data-sql
```

This writes:

```text
examples/invoice-demo/src/main/resources/data.sql
```

Use `mvn -B -ntp terubase:export-flyway` instead when the project uses Flyway.

You can also run the example Spring Boot app and use the local runtime
playground endpoints:

```bash
mvn -B -ntp spring-boot:run
```

See [`examples/invoice-demo/README.md`](examples/invoice-demo/README.md) for
plugin output, curl examples, and details.

## Workflow

```mermaid
flowchart LR
    A[JPA Entities] --> B[GET /terubase/api/entities]
    B --> C[GET /terubase/api/seed-plan]
    C --> D[POST /terubase/api/mock]
    D --> E[POST /terubase/api/export/sql]
    D --> F[POST /terubase/api/export/json]
    E --> G[local/demo/CI seed data]
    F --> G
```

The plugin workflow mirrors the planning and export parts at build time: JPA
entities become schema context and seed plans, then reviewed SQL becomes a
validated `data.sql` or Flyway file. The plugin does not call an AI provider.

## Endpoints

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/terubase/api/entities` | Discover JPA entity metadata. |
| `GET` | `/terubase/api/scenarios` | List built-in scenario templates. |
| `GET` | `/terubase/api/seed-plan` | Build an AI-ready seed plan. |
| `POST` | `/terubase/api/mock` | Generate export-first AI seed SQL. |
| `POST` | `/terubase/api/export/sql` | Export reviewed `INSERT` statements as SQL. |
| `POST` | `/terubase/api/export/json` | Export reviewed `INSERT` statements as JSON. |
| `GET` | `/terubase/api/status` | Check isolated SQL service status. |
| `POST` | `/terubase/api/execute` | Execute SQL when explicitly enabled. |

## Safety Defaults

> Keep TeruBase local, export-first, and separate from production data.

- Use TeruBase only with `local`, `dev`, or `test` profiles.
- Never expose `/terubase/api/**` publicly.
- Never use real production records in prompts or examples.
- Keep AI generation export-first with `"execute": false`.
- Review generated SQL before optional isolated execution.
- Keep `sql-execution-enabled` disabled unless direct local SQL access is needed.
- Keep `force-enable-in-production` disabled.

## Configuration

The complete example is
[`application-terubase-example.yml`](terubase-spring-boot-starter/src/main/resources/application-terubase-example.yml).
All settings are flat `terubase.*` properties:

```yaml
terubase:
  enabled: true
  jdbc-url: jdbc:h2:mem:terubase_isolated_db;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH
  username: sa
  password: ""
  entity-base-package: com.example.store.domain
  open-ai-chat-completions-url: https://api.openai.com/v1/chat/completions
  open-ai-model: gpt-4o
  max-mock-rows: 100
  sql-execution-enabled: false
  force-enable-in-production: false
```

TeruBase auto-configuration is blocked when the `prod` or `production` Spring
profile is active. An intentional override requires:

```yaml
terubase:
  force-enable-in-production: true
```

## Main Workflow

### Discover Entities

```http
GET /terubase/api/entities
```

TeruBase scans `terubase.entity-base-package` and returns JPA metadata for
columns, IDs, generated values, enums, relationships, join columns, join
tables, and deterministic insert-order hints. The hints guide seed generation;
they are not a full database dependency planner.

### Choose a Scenario

```http
GET /terubase/api/scenarios
GET /terubase/api/scenarios/{id}
```

Built-in IDs:

- `ecommerce-demo`
- `saas-billing-demo`
- `crm-demo`
- `banking-lite-demo`
- `task-management-demo`
- `inventory-management-demo`
- `learning-management-demo`
- `event-registration-demo`
- `qa-edge-cases`
- `frontend-dashboard-demo`

### Build an AI-Ready Seed Plan

```http
GET /terubase/api/seed-plan?scenarioId=saas-billing-demo&count=30
```

The response combines scenario intent with discovered metadata and returns a
`schemaPrompt` plus a `recommendedMockRequest`. This endpoint does not require
an API key and does not call AI.

### Generate Export-First Seed SQL

```http
POST /terubase/api/mock
Content-Type: application/json

{
  "count": 20,
  "apiKey": "your-local-api-key",
  "schema": "<schemaPrompt from /terubase/api/seed-plan>",
  "scenario": "Generate a fictional SaaS billing demo with overdue invoices",
  "dialect": "h2-postgresql-mode",
  "execute": false
}
```

`execute=false` returns SQL for review without running it. TeruBase accepts only
`INSERT` statements from AI output. It blocks unsupported or destructive SQL.
API keys are request-only: never commit, store, or log them.

### Export Generated Data

Export reviewed statements as SQL:

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

Or as JSON:

```http
POST /terubase/api/export/json
Content-Type: application/json

{
  "scenario": "Fictional SaaS billing demo",
  "statements": [
    "insert into customer (id, name) values (1, 'Sara')"
  ]
}
```

Export endpoints do not execute SQL or write files to disk. They accept only
`INSERT` statements and return content for review, local seed files, CI
fixtures, and demos.

### Optional Local Execution

AI-generated SQL can run against TeruBase's isolated H2 database by setting
`"execute": true` in `POST /terubase/api/mock`. Batch execution is transactional
and rolls back on failure.

For direct local SQL access, explicitly enable:

```yaml
terubase:
  sql-execution-enabled: true
```

Then use:

```http
GET /terubase/api/status

POST /terubase/api/execute
Content-Type: application/json

{
  "sql": "select 1 as value"
}
```

## Build

```bash
mvn -B -ntp clean verify
```

## License

MIT License.
