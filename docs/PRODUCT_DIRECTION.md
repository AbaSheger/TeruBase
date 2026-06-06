# TeruBase Product Direction

## Problem

Spring Boot applications often have empty or unrealistic local databases.
Hand-written seed scripts become stale as JPA models change. Copying production
data creates privacy and security risks. Developers need realistic data quickly
without building and maintaining fixtures by hand.

## Target Users

- Java and Spring Boot developers
- Frontend developers working against local backend APIs
- QA engineers preparing repeatable scenarios
- Teams building demos, onboarding flows, and CI pipelines

## Main Use Cases

- Prepare relationship-aware metadata and seed plans from JPA entities.
- Create realistic demo scenarios without production records.
- Export reviewable seed artifacts for Flyway, Liquibase, `data.sql`,
  Testcontainers, local use, and CI pipelines.
- Inspect entity metadata before generating data.
- Optionally use the runtime starter as a local playground.

## Product Promise

> From JPA entities to realistic runnable seed data in minutes.

## What TeruBase Is

TeruBase is a Spring Boot-native seed-data copilot. It discovers JPA entities
and relationships, prepares schema context, builds relationship-aware seed
plans, and exports reviewable seed artifacts that complement Flyway and
Liquibase rather than replacing them.

The preferred product path is Maven-plugin and build-time workflow first: scan
the application model during development or CI, write schema context and seed
plans into `target/terubase`, and export artifacts that complement existing
migration and test workflows. The runtime starter remains useful as an optional
local playground, but it should not be the primary adoption path.

`terubase:plan` and `terubase:export-flyway` should not use AI tokens. AI
generation should be optional, provider-agnostic, and export-first when used.

## What TeruBase Is Not

- A generic fake-data generator
- An H2 console clone
- A production database administration API
- A migration tool replacing Flyway or Liquibase
- An enterprise test-data-management platform yet
- A reason to copy production data into development
- A runtime dependency every application must carry
- An OpenAI-only workflow

## Open-Source Wedge

The open-source wedge should solve one workflow well: run TeruBase from Maven
and quickly produce reviewable schema context, seed plans, and seed artifacts
from a Spring Boot application's JPA entities.

The Maven plugin is local-first, useful without a cloud account, easy to
inspect, and safe by default. Scenario templates, seed plans, metadata quality,
SQL export, tests, and README examples remain important, while adoption work
should avoid adding runtime weight to host applications.

The starter should remain available for optional local exploration and demos. It
should not become the primary installation path.

## Possible Future SaaS/Cloud Direction

Future hosted capabilities may include:

- Team-shared scenario libraries
- Private templates and workspace controls
- Repository scanning and schema history
- Seed-data versioning
- CI/CD generation APIs
- Privacy-friendly anonymization workflows
- Audit logs

These should not complicate the local starter before the core workflow is proven.

## Short-Term Priorities

1. Validate the released Maven plugin against real Spring Boot/JPA projects.
2. Improve plugin metadata for join columns, join tables, and additional JPA
   mapping patterns.
3. Make the plan-to-reviewed-SQL workflow clearer in documentation and demos.
4. Keep AI generation optional, provider-agnostic, and export-first.
5. Add output formats only after Flyway export usage is validated.
6. Keep the runtime starter stable as an optional local playground.
7. Do not add a frontend now.

## Long-Term Roadmap

1. Prove the Maven plugin workflow across real Spring Boot services.
2. Improve generation quality for common JPA relationship patterns.
3. Support reusable team scenarios and CI usage.
4. Add provider-agnostic optional AI generation after non-AI scan, plan, and
   export workflows are valuable on their own.
5. Evaluate hosted collaboration and governance features after adoption signals
   justify the added scope.
