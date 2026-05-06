# Meal Planner Embabel Showcase

## Purpose

This project has two goals:

* build a private household meal planner for Thomas and Cassandra
* showcase Embabel and the `embabel-agent-builder` skill through iterative, spec-first agent slices

The product behavior lives in [../source-spec.md](../source-spec.md) and the product slice plan lives in [../product-spec-index.md](../product-spec-index.md). This document only describes the Embabel showcase arc. It is not an implementation spec.

## Showcase Principles

| Principle | Meaning |
| --- | --- |
| Product-first demos | Each Embabel feature must solve a real meal-planning problem. |
| One main concept per slice | Each version should make one Embabel capability easy to see and explain. |
| Manual skill invocation | Each slice gets planned by explicitly invoking `embabel-agent-builder`. |
| Fresh implementation sessions | Implementation should happen in a new session from an approved slice spec. |
| Product specs stay product-only | Embabel mechanics belong in per-slice agent specs, not product behavior docs. |
| Shell before web | Command-line slices prove the agent workflows before the browser UI adds interface concerns. |

## Showcase Roadmap

| Version | Product Slice | Embabel Concept To Showcase | Demo Moment |
| --- | --- | --- | --- |
| v0 | One-shot weekly lunch planning | Typed GOAP from request to goal output | The shell turns one request into a complete next-week lunch plan with nutrition notes. |
| v1 | Household profile | Blackboard facts plus local persistence | A short planning request uses saved household preferences. |
| v2 | Recipe backlog and recipe text import | Recipe-focused subagent and structured extraction | Pasted recipe text becomes a saved recipe and later influences planning. |
| v3 | Plan revision and shopping list | Stateful revision loop and structured outputs | The household iterates on a plan, accepts it, then receives a structured shopping list. |
| v4 | Household web interface | Browser invocation and planner observability | The web UI drives the same workflows and shows enough planner state to make Embabel understandable. |
| v5 | Weekly feedback memory | Feedback loop over persisted household memory | Prior feedback changes a later plan and the plan explains the influence. |

## Slice Workflow

For each version:

1. Start a new planning session.
2. Explicitly invoke `embabel-agent-builder`.
3. Ask for a high-level Embabel agent spec for exactly one product slice.
4. Review and adjust that slice spec.
5. Start a new implementation session.
6. Explicitly invoke `embabel-agent-builder` again.
7. Implement only the approved slice spec.
8. Verify the demo moment before moving to the next slice.

## Spec Naming

Use one Embabel spec per slice:

```text
docs/agents/meal-planner-v0-agent-spec.md
docs/agents/meal-planner-v1-agent-spec.md
docs/agents/meal-planner-v2-agent-spec.md
docs/agents/meal-planner-v3-agent-spec.md
docs/agents/meal-planner-v4-agent-spec.md
docs/agents/meal-planner-v5-agent-spec.md
```

Each slice spec should contain the domain model, blackboard facts, GOAP flow, implementation plan, tests, assumptions, and open questions for that slice only.

## Current Recommendation

Start with v0 only. Do not pre-plan detailed Embabel flows for later versions until the earlier slice has been implemented and verified.

The later product roadmap is useful, but detailed agent design too early will likely produce abstractions that do not survive contact with the actual shell workflow.

## Open Questions

* For v4, planner observability is the recommended showcase focus. Private access is necessary product behavior, but it is less distinctive as an Embabel demo.
