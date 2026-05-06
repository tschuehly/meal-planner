# Meal Planner

Private household meal-planning agent for Thomas and Cassandra.

This repository is the cleaned project scaffold for the meal planner. The project builds a private household meal planner and uses it to showcase Embabel through iterative agent-building slices.

Product behavior is split into iterative specs in [docs/product-spec-index.md](docs/product-spec-index.md), starting with [docs/product-spec-v0.md](docs/product-spec-v0.md). The full source product spec is captured in [docs/source-spec.md](docs/source-spec.md). The Embabel showcase plan is captured in [docs/agents/meal-planner-embabel-showcase-spec.md](docs/agents/meal-planner-embabel-showcase-spec.md).

## Run

The supported local agent workflow uses `llama-server` with GPT-OSS 20B.

```bash
./scripts/llama-server.sh
```

Start the Embabel shell in another terminal:

```bash
./scripts/llama-server-shell.sh
```

The shell prompt accepts commands. Use `chat` to start a chat session, or inspect the runtime with commands such as `models`, `profiles`, and `agents`.

If GPT-OSS 20B is not cached locally yet, run the server once with network access enabled:

```bash
LLAMA_SERVER_OFFLINE=false ./scripts/llama-server.sh
```

## Test

```bash
./mvnw test
```
