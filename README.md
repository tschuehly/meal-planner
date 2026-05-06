# Meal Planner

Private household meal-planning agent for Thomas and Cassandra.

This repository is the cleaned project scaffold for the meal planner. Product behavior is captured in [docs/product-spec-v0.md](docs/product-spec-v0.md).

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
