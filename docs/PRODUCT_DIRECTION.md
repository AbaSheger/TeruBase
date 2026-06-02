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

- Generate relationship-aware local seed data from JPA entities.
- Create realistic demo scenarios without production records.
- Export reviewable SQL for local use and CI pipelines.
- Populate an isolated H2 database to test application behavior.
- Inspect entity metadata before generating data.

## Product Promise

> From JPA entities to realistic runnable seed data in minutes.

## What TeruBase Is

TeruBase is a Spring Boot-native AI seed-data copilot. It discovers JPA entities
and relationships, prepares AI-ready schema context, and generates safe,
reviewable seed SQL. It supports export-first workflows and isolated local
execution.

Its product angle is the full path from application model to useful scenario:
schema discovery, scenario intent, relationship-aware generation, safe SQL
export, and optional local execution.

## What TeruBase Is Not

- A generic fake-data generator
- An H2 console clone
- A production database administration API
- A migration tool replacing Flyway or Liquibase
- An enterprise test-data-management platform yet
- A reason to copy production data into development

## Open-Source Wedge

The open-source starter should solve one workflow well: add TeruBase to a Spring
Boot service and quickly produce realistic runnable seed data from its JPA
entities.

The starter should remain local-first, useful without a cloud account, easy to
inspect, and safe by default. Scenario templates, seed plans, metadata quality,
SQL export, profile guards, tests, and README examples are the immediate focus.

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

1. Add scenario templates and `/terubase/api/scenarios`.
2. Add an AI-ready seed-plan endpoint.
3. Improve JPA metadata extraction.
4. Add safe SQL export endpoints.
5. Block risky endpoints in production profiles by default.
6. Expand automated tests.
7. Add clear README examples.

## Long-Term Roadmap

1. Prove the local starter workflow across real Spring Boot services.
2. Improve generation quality for common JPA relationship patterns.
3. Support reusable team scenarios and CI usage.
4. Evaluate hosted collaboration and governance features after adoption signals
   justify the added scope.

