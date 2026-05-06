# Meal Planner

Private Embabel meal-planning agent for Thomas and Cassandra.

The supported workflow is a one-shot command-line lunch planner. You enter one free-text request, and the agent returns one weekday lunch plan with five meals, fit reasons, rough nutrition notes, visible constraint handling, and assumptions.

The agent does not save recipes, keep household state, revise accepted plans, or generate shopping lists.

## Prerequisites

- Java 21 or newer
- Maven via the included `./mvnw` wrapper
- `llama-server` available on your `PATH`
- GPT-OSS 20B cached locally, or network access for the first model download

## Start The Model

Run the local OpenAI-compatible model server:

```bash
./scripts/llama-server.sh
```

By default this starts `llama-server` on `127.0.0.1:8080` with `ggml-org/gpt-oss-20b-GGUF` in offline mode.

If the model is not cached yet, allow the first download:

```bash
LLAMA_SERVER_OFFLINE=false ./scripts/llama-server.sh
```

Useful overrides:

```bash
LLAMA_SERVER_PORT=8081 ./scripts/llama-server.sh
LLAMA_SERVER_HF_REPO=ggml-org/gpt-oss-20b-GGUF ./scripts/llama-server.sh
```

## Start The Agent Shell

In a second terminal, start the Embabel shell configured for the local model server:

```bash
./scripts/llama-server-shell.sh
```

The shell uses the `llama-server` Spring profile and the `llama-server-models` Maven profile. It reads the model endpoint from `LLAMA_SERVER_BASE_URL`, defaulting to `http://127.0.0.1:8080`.

If you changed the server port, pass the matching base URL:

```bash
LLAMA_SERVER_BASE_URL=http://127.0.0.1:8081 ./scripts/llama-server-shell.sh
```

## Use The Agent

At the Embabel shell prompt, inspect the runtime if useful:

```text
models
agents
```

Run one lunch-planning request with one-shot execution:

```text
x "Plan high-protein weekday lunches for Thomas and Cassandra next week. Avoid mushrooms, keep prep easy, and use chickpeas if they fit."
```

The long-form command is also supported:

```text
execute "Plan high-protein weekday lunches for Thomas and Cassandra next week. Avoid mushrooms, keep prep easy, and use chickpeas if they fit."
```

You can also paste recipe text into the same request:

```text
x "Plan lunches for next week and avoid mushrooms. Use this recipe as inspiration for one lunch only: roasted chickpeas with lemon yogurt, cucumber, herbs, and rice."
```

The response should include:

- Monday through Friday lunches
- why each lunch fits Thomas and Cassandra
- rough nutrition notes
- visible handling of constraints such as allergies, dislikes, and avoided ingredients
- assumptions the household can correct in a later request

## Supported Request Shape

The agent works best when the request includes any relevant:

- target week, such as `next week`, `this week`, `in two weeks`, or an ISO date like `2026-06-17`
- allergies, intolerances, dislikes, or ingredients to avoid
- nutrition priorities, such as high-protein or lighter lunches
- effort hints, such as easy prep or leftovers
- ingredients to use
- pasted recipe context for the current run

If no week is specified, the agent plans for next week.

## How The Agent Works

`MealPlannerAgent` is a typed Embabel GOAP agent. The shell input becomes a `HouseholdLunchPlanningRequest`, deterministic actions add defaults and resolve the week, LLM actions interpret constraints, draft candidates, assemble a plan, and format the final response.

Before formatting, validation checks the plan structure and required content. Invalid plans get a bounded repair loop with up to two model repair attempts. If the plan still fails validation, the agent fails loudly with concrete validation issues.

## Test

Run the focused test suite:

```bash
./mvnw clean test
```

The tests cover week resolution, prompt wiring, validation, bounded repair behavior, and annotated agent metadata.
