# Meal Planner

Private household meal-planning agent for Thomas and Cassandra.

This repository is the cleaned project scaffold for the meal planner. The project builds a private household meal planner and uses it to showcase Embabel through iterative agent-building slices.

Product behavior is split into iterative specs in [docs/product-spec-index.md](docs/product-spec-index.md), starting with [docs/product-spec-v0.md](docs/product-spec-v0.md). The full source product spec is captured in [docs/source-spec.md](docs/source-spec.md). The Embabel showcase plan is captured in [docs/agents/meal-planner-embabel-showcase-spec.md](docs/agents/meal-planner-embabel-showcase-spec.md).

## Run

Set one model provider key when running agent workflows:

```bash
export OPENAI_API_KEY=...
```

or:

```bash
export ANTHROPIC_API_KEY=...
```

Start the Embabel shell:

```bash
./scripts/shell.sh
```

## Test

```bash
./mvnw test
```
