# TeruBase Implementation Plan

Implement each phase as a small, testable change. Run:

```bash
mvn -B -ntp clean verify
```

after implementation work. If verification cannot run, explain exactly why.

## Phase 1: Scenario Templates and `/terubase/api/scenarios`

### Goal

Provide a small built-in catalog of useful scenarios for demos, QA, and local
development.

### Files Likely to Change

- New scenario DTO and service classes under `com.terubase.starter`
- Existing or new controller under `com.terubase.starter`
- Controller and service tests
- `README.md`

### Acceptance Criteria

- `GET /terubase/api/scenarios` returns stable scenario IDs, names, and prompts.
- Templates are concise, useful, and do not include production data.
- Developer dashboard endpoint CORS behavior remains enabled.

### Tests to Add

- Scenario service catalog test
- Scenario controller response test

### What Not to Do

- Do not add persistence, user-defined templates, or a frontend.

## Phase 2: Seed-Plan Endpoint and AI-Ready Schema Prompt

### Goal

Expose a reviewable seed plan that combines scenario intent with schema metadata
before SQL generation.

### Files Likely to Change

- New seed-plan DTO and service classes
- Existing schema service
- New or existing controller
- Unit and controller tests

### Acceptance Criteria

- A `/terubase/api` seed-plan endpoint returns structured generation context.
- The response includes scenario intent and an AI-ready schema prompt.
- The endpoint does not call the AI provider or execute SQL.

### Tests to Add

- Prompt formatting test
- Seed-plan controller test
- Invalid scenario request test

### What Not to Do

- Do not mix planning, generation, and execution into one endpoint.

## Phase 3: Improve JPA Metadata Extraction

### Goal

Produce stable metadata that is sufficient for relationship-aware generation.

### Files Likely to Change

- `TeruBaseSchemaService`
- Schema DTOs if needed
- JPA fixture entities in tests
- Schema service tests

### Acceptance Criteria

- Metadata covers IDs, generated values, columns, nullability, enums, and common
  relationships.
- Owning and inverse relationship sides are distinguishable.
- Output ordering is deterministic.
- Unsupported mappings are visible rather than silently ignored.

### Tests to Add

- Entity field metadata tests
- Relationship metadata tests
- Deterministic output test

### What Not to Do

- Do not attempt to support every JPA edge case in one change.

## Phase 4: Safe SQL Export Endpoints

### Goal

Generate and export reviewable seed SQL with strict AI-output safety rules.

### Files Likely to Change

- `TeruBaseOpenAiClient`
- `TeruBaseSqlService`
- Mock-data controller and DTOs
- SQL validation helper
- Unit and controller tests

### Acceptance Criteria

- Export-only mode is the default.
- Only `INSERT` statements from AI output are accepted.
- Destructive or unsupported statements are rejected before execution.
- Export responses are usable as SQL files or response bodies.
- Executed batches roll back fully on failure.

### Tests to Add

- Valid `INSERT` export test
- Destructive SQL rejection tests
- Mixed-statement rejection test
- Transaction rollback test

### What Not to Do

- Do not permit generated `UPDATE`, `DELETE`, DDL, or arbitrary SQL.

## Phase 5: Local-Only Profile Safety Guard

### Goal

Prevent accidental exposure of risky TeruBase endpoints in production.

### Files Likely to Change

- `TeruBaseAutoConfiguration`
- `TeruBaseProperties`
- Configuration tests
- Example configuration
- `README.md`

### Acceptance Criteria

- Risky endpoints are blocked in `prod` and `production` profiles by default.
- An explicit force-enable property is required to override the guard.
- Default local development remains straightforward.

### Tests to Add

- Local profile enabled test
- `prod` profile blocked test
- `production` profile blocked test
- Explicit force-enable test

### What Not to Do

- Do not rely on README warnings as the only production safeguard.

## Phase 6: README and Examples

### Goal

Make the starter understandable and worth trying without reading source code.

### Files Likely to Change

- `README.md`
- Example configuration under `src/main/resources`

### Acceptance Criteria

- README shows setup, scenarios, seed planning, SQL export, and optional local
  execution.
- Examples use fictional data only.
- Safety defaults and production profile behavior are clear.

### Tests to Add

- No new automated tests required unless examples become executable.

### What Not to Do

- Do not add a frontend or cloud setup guide.

## Phase 7: Tests and Cleanup

### Goal

Close coverage gaps and simplify the implementation before broader product work.

### Files Likely to Change

- Tests under `src/test/java/com/terubase/starter`
- Small production-code fixes revealed by tests
- `README.md` if behavior clarification is needed

### Acceptance Criteria

- `mvn -B -ntp clean verify` passes.
- Scenario, seed-plan, metadata, SQL safety, rollback, and profile-guard behavior
  are covered.
- Logging does not expose API keys.
- No placeholder code, unrelated refactors, or unnecessary dependencies remain.

### Tests to Add

- Integration coverage across the primary local workflow
- Regression tests for any bugs found during cleanup

### What Not to Do

- Do not expand product scope while closing quality gaps.

## Known Limitations / Future Work

- JPA metadata extraction currently focuses on field annotations. Property-access
  mappings and less common JPA mapping patterns need dedicated coverage before
  claiming broader compatibility.
- `POST /terubase/api/mock` accepts an API key in the request body for a single
  provider call. TeruBase does not store or log it, but deployments should still
  avoid request-body logging. A future integration may support externally
  supplied secrets without expanding the local starter scope.
- Setting `"execute": true` on `POST /terubase/api/mock` explicitly runs the
  validated `INSERT` batch against TeruBase's isolated H2 database.
  `terubase.sql-execution-enabled` separately controls the direct SQL console
  endpoint, `POST /terubase/api/execute`.
