# TeruBase Invoice Demo

Small Spring Boot app showing TeruBase discovering real JPA entities:

- `Customer`
- `Invoice`
- `Payment`
- `InvoiceStatus`
- `PaymentStatus`

The application uses its own H2 datasource for the invoice app. TeruBase uses a
separate isolated H2 datasource for optional seed execution.

## Run

From the repository root, install the local starter version:

```bash
mvn -B -ntp clean install
```

Then run the demo app:

```bash
cd examples/invoice-demo
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
- `/terubase/api/mock` requires an OpenAI-compatible API key because it calls the
  configured AI provider.
- The example is intentionally small and does not define business APIs beyond
  the TeruBase endpoints.
