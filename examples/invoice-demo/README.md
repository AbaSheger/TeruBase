# TeruBase Invoice Demo

Small Spring Boot app showing TeruBase's repeatable seed-data workflow on real
JPA entities:

- `Customer`
- `Invoice`
- `Payment`
- `InvoiceStatus`
- `PaymentStatus`

The application uses its own H2 datasource for the invoice app. TeruBase uses a
separate isolated H2 datasource for optional seed execution.

## Maven Plugin Workflow

TeruBase `0.1.1` is declared in the demo `pom.xml`. Generate non-AI seed-plan
artifacts from the invoice demo:

```bash
cd examples/invoice-demo
mvn -B -ntp compile terubase:plan
```

This writes:

```text
target/terubase/schema-context.json
target/terubase/seed-plan.md
```

The command also prints the files an AI tool must be able to read, a short
prompt, and the exact next commands. It does not create another prompt file or
call an AI provider.

Example `schema-context.json` excerpt:

```json
{
  "entities": [
    {
      "className": "com.terubase.starter.examples.invoice.Customer",
      "simpleName": "Customer",
      "tableName": "customers",
      "insertOrderHint": "Parent or reference entity; insert before dependent child entities."
    },
    {
      "className": "com.terubase.starter.examples.invoice.Invoice",
      "simpleName": "Invoice",
      "tableName": "invoices",
      "insertOrderHint": "Child entity with foreign-key relationships; insert after referenced parent entities."
    }
  ]
}
```

Example `seed-plan.md` excerpt:

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
```

Write the seed SQL yourself or draft it with an AI assistant using the generated
schema context and plan. After reviewing it, save it as
`target/terubase/generated-seed.sql`:

```sql
-- TeruBase seed data
INSERT INTO customers (id, name, email, billing_city, billing_country)
VALUES (1, 'Northstar Studio', 'billing@northstar.example', 'Stockholm', 'Sweden');

INSERT INTO invoices (id, invoice_number, customer_id, total_amount, currency, status)
VALUES (10, 'INV-2026-0001', 1, 1200.00, 'USD', 'SENT');
```

This SQL is a manually reviewed example. The Maven plugin does not discover the
`@JoinColumn(name = "customer_id")` value, so SQL authors must verify physical
column names against the application schema.

Then export it to Spring Boot's `data.sql`:

```bash
mvn -B -ntp terubase:export-data-sql
```

This writes:

```text
src/main/resources/data.sql
```

TeruBase validates the export as `INSERT`-only before writing the file. Projects
using Flyway can instead run:

```bash
mvn -B -ntp terubase:export-flyway
```

That writes
`src/main/resources/db/migration/V999__terubase_seed_data.sql`.

The demo sets `spring.jpa.defer-datasource-initialization=true` because Hibernate
creates its tables with `ddl-auto: create-drop`. This makes Spring load
`data.sql` after those tables exist.

## Optional Runtime Playground

Run the demo app:

```bash
mvn -B -ntp spring-boot:run
```

The app starts on `http://localhost:8080`.

## Inspect Entities

```bash
curl http://localhost:8080/terubase/api/entities
```

## List Scenarios

```bash
curl http://localhost:8080/terubase/api/scenarios
```

## Build a Seed Plan

```bash
curl "http://localhost:8080/terubase/api/seed-plan?scenarioId=saas-billing-demo&count=20"
```

The response includes `schemaPrompt` and `recommendedMockRequest`. Use the
schema prompt when calling `/terubase/api/mock`.

## Generate Mock SQL Without Executing

Replace `YOUR_API_KEY` and paste the `schemaPrompt` value from the seed-plan
response into `schema`.

```bash
curl -X POST http://localhost:8080/terubase/api/mock \
  -H "Content-Type: application/json" \
  -d '{
    "count": 20,
    "apiKey": "YOUR_API_KEY",
    "schema": "PASTE_SCHEMA_PROMPT_FROM_SEED_PLAN",
    "scenario": "SaaS billing demo with customers, invoices, and payments.",
    "dialect": "h2-postgresql-mode",
    "execute": false
  }'
```

With `execute=false`, TeruBase returns validated `INSERT` statements and export
SQL, but does not run them.

## Export SQL

Paste generated statements from `/terubase/api/mock` into `statements`.

```bash
curl -X POST http://localhost:8080/terubase/api/export/sql \
  -H "Content-Type: application/json" \
  -d '{
    "scenario": "saas-billing-demo invoice demo",
    "filename": "invoice-demo-seed.sql",
    "statements": [
      "INSERT INTO customers (id, name, email, billing_city, billing_country, created_at) VALUES (1, '\''Northstar Studio'\'', '\''billing@northstar.example'\'', '\''Stockholm'\'', '\''Sweden'\'', TIMESTAMP '\''2026-01-10T09:00:00'\'')",
      "INSERT INTO invoices (id, invoice_number, customer_id, issued_on, due_on, total_amount, currency, status) VALUES (1, '\''INV-2026-0001'\'', 1, DATE '\''2026-01-12'\'', DATE '\''2026-02-12'\'', 1200.00, '\''USD'\'', '\''SENT'\'')"
    ]
  }'
```

## Notes

- The direct SQL execution endpoint remains disabled by default with
  `terubase.sql-execution-enabled=false`.
- The Maven plugin workflow does not call an AI provider.
- `/terubase/api/mock` requires an OpenAI-compatible API key because it calls the
  configured AI provider.
- The example is intentionally small and does not define business APIs beyond
  the TeruBase endpoints.
