# Meal Planner v0 Embabel Agent Spec

## Source Input

This spec translates [../product-spec-v0.md](../product-spec-v0.md) into a high-level Embabel agent design for the first product slice.

The product slice is a command-line, one-shot weekly lunch-planning workflow for Thomas and Cassandra. The household enters one free-text request and receives one structured five-lunch plan for next week unless it asks for a different week. The plan must explain why each lunch fits, include rough nutrition notes, visibly handle request constraints, and list assumptions the household can correct later.

## User Outcome

The household receives one practical weekday lunch plan with five concrete meals for Thomas and Cassandra, suitable for the requested week, request-specific constraints, and v0 defaults.

The goal-achieving output type is `WeeklyLunchPlanResponse`.

The outcome is one-shot. The v0 agent does not keep durable state, wait for follow-up turns, save recipes, revise plans, accept plans, or generate shopping lists.

## Domain Model

| Term | Meaning | Existing Type | New/Changed Type | Notes |
| --- | --- | --- | --- | --- |
| Household lunch-planning request | The single free-text command-line request from the household. | None | `HouseholdLunchPlanningRequest` | Starting input fact. |
| Household member | Thomas or Cassandra as people the plan must work for. | None | `HouseholdMember` enum or value object | Not a UI role. Used only to ground household fit. |
| Planning horizon | The target week and weekday lunch slots. | None | `PlanningHorizon` | Defaults to next week and five weekday lunches. |
| Lunch-planning defaults | v0 defaults for meal count, effort, health tone, and household style. | None | `LunchPlanningDefaults` | Deterministic defaults should be available without asking setup questions. |
| Request constraints | Explicit dislikes, allergies, intolerances, ingredients to avoid, requested week, effort hints, nutrition priorities, and pasted recipe context. | None | `RequestConstraints` | Safety-critical constraints should be visible in the final response. |
| One-off recipe context | Recipe text pasted into the request for this run only. | None | `OneOffRecipeContext` | It is not saved and does not imply recipe-backlog behavior. |
| Lunch candidate | A possible lunch meal with fit rationale, prep notes, and rough nutrition. | None | `LunchCandidate` | Intermediate domain fact for selection and validation. |
| Weekly lunch plan | Five selected weekday lunches plus assumptions and constraint-handling notes. | None | `WeeklyLunchPlan` | Main structured plan fact before response formatting. |
| Weekly lunch plan response | The command-line response text shown to the household. | None | `WeeklyLunchPlanResponse` | Final goal output. |
| Assumption | A reasonable default or inferred fact used because the request was underspecified. | None | `PlanningAssumption` | Must be shown so the household can correct it later. |

## Blackboard Facts

| Fact Type | Producer | Consumers | Why It Belongs On The Blackboard |
| --- | --- | --- | --- |
| `HouseholdLunchPlanningRequest` | `ingestHouseholdLunchPlanningRequest` | `provideLunchPlanningDefaults`, `resolvePlanningHorizon`, `interpretLunchPlanningRequest` | It is the starting request fact for the one-shot plan. |
| `LunchPlanningDefaults` | `provideLunchPlanningDefaults` | `interpretLunchPlanningRequest`, `draftLunchCandidates`, `validateWeeklyLunchPlan` | Defaults are domain policy, not prompt text. |
| `PlanningHorizon` | `resolvePlanningHorizon` | `draftLunchCandidates`, `assembleWeeklyLunchPlan`, `formatWeeklyLunchPlanResponse` | The planner needs a typed target week and weekday slots. |
| `RequestConstraints` | `interpretLunchPlanningRequest` | `draftLunchCandidates`, `validateWeeklyLunchPlan`, `formatWeeklyLunchPlanResponse` | Explicit safety and fit constraints must guide generation and be reported. |
| `OneOffRecipeContext` | `interpretLunchPlanningRequest` | `draftLunchCandidates`, `formatWeeklyLunchPlanResponse` | Pasted recipe text may influence the current plan and final note without persistence. |
| `LunchCandidateSet` | `draftLunchCandidates` | `assembleWeeklyLunchPlan` | Candidate meals are useful intermediate domain objects for selection. |
| `WeeklyLunchPlan` | `assembleWeeklyLunchPlan` or repair inside `validateWeeklyLunchPlan` | `validateWeeklyLunchPlan`, `formatWeeklyLunchPlanResponse` | The selected plan should be valid before presentation. |
| `ValidatedWeeklyLunchPlan` | `validateWeeklyLunchPlan` | `formatWeeklyLunchPlanResponse` | Validation records that the plan has five lunches, visible constraints, rough nutrition, assumptions, and practical tone. |
| `WeeklyLunchPlanResponse` | `formatWeeklyLunchPlanResponse` | Shell output | Final user-visible goal fact. |

## GOAP Flow

| Step | Action | Inputs | Output | Purpose | LLM? | Tools? | Goal? |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 0 | `ingestHouseholdLunchPlanningRequest` | `UserInput` | `HouseholdLunchPlanningRequest` | Adapt shell-style free text into the v0 request fact. | No | No | No |
| 1 | `provideLunchPlanningDefaults` | `HouseholdLunchPlanningRequest` | `LunchPlanningDefaults` | Provide the supported defaults for weekday lunches, effort, health tone, and household style. | No | No | No |
| 2 | `resolvePlanningHorizon` | `HouseholdLunchPlanningRequest`, `LunchPlanningDefaults` | `PlanningHorizon` | Resolve the requested week, defaulting to next week, and create Monday-Friday lunch slots. | No | Optional date/time service | No |
| 3 | `interpretLunchPlanningRequest` | `HouseholdLunchPlanningRequest`, `LunchPlanningDefaults` | `InterpretedLunchRequest` containing `RequestConstraints` and optional `OneOffRecipeContext` | Extract explicit constraints, preferences, allergies, intolerances, dislikes, nutrition priorities, effort hints, and pasted recipe context. | Yes | No | No |
| 4 | `draftLunchCandidates` | `PlanningHorizon`, `LunchPlanningDefaults`, `RequestConstraints`, optional `OneOffRecipeContext` | `LunchCandidateSet` | Generate enough practical lunch options to support five weekday choices with fit reasons, prep notes, and rough nutrition. | Yes | No | No |
| 5 | `assembleWeeklyLunchPlan` | `PlanningHorizon`, `LunchPlanningDefaults`, `RequestConstraints`, `LunchCandidateSet` | `WeeklyLunchPlan` | Select five lunches, assign them to weekdays, preserve variety and practicality, and record assumptions plus visible constraint handling. | Yes | No | No |
| 6 | `validateWeeklyLunchPlan` | `WeeklyLunchPlan`, `RequestConstraints`, `LunchPlanningDefaults` | `ValidatedWeeklyLunchPlan` | Inspect the plan, repair invalid structure or unsupported content with a bounded model call, then validate the repaired plan. | Conditional | No | No |
| 7 | `formatWeeklyLunchPlanResponse` | `ValidatedWeeklyLunchPlan`, `PlanningHorizon`, `RequestConstraints`, optional `OneOffRecipeContext` | `WeeklyLunchPlanResponse` | Produce the structured command-line response in the supported format. | Yes | No | Yes |

## Structured Output Contracts

| Action | Output Type | Required Shape | Nullable/Absent Values | Validation And Defaults | Examples Needed | Formatter Truthfulness Risk |
| --- | --- | --- | --- | --- | --- | --- |
| `interpretLunchPlanningRequest` | `LlmInterpretedLunchRequest` converted to `InterpretedLunchRequest` | `requestConstraints` plus optional LLM recipe context. Domain `OneOffRecipeContext` still requires a summary. | Absent or empty recipe context normalizes to `null` before blackboard binding. | A tolerant LLM boundary type absorbs `{}` and converts only meaningful recipe context into the strict domain type. | No-context and with-context examples. | The formatter receives optional recipe context directly and mentions recipe context only when present. |
| `draftLunchCandidates` | `LunchCandidateSet` | Enough candidates to choose five lunches with title, description, fit, nutrition, prep, constraints, and ingredients. | Optional prep notes and constraint handling may be empty. | Candidate content is checked through plan validation after assembly. | Not required for v0. | Candidate text is not user-visible unless selected into a validated plan. |
| `assembleWeeklyLunchPlan` | `WeeklyLunchPlan` | Five Monday-Friday lunches with visible constraints and assumptions. | Empty assumptions or missing visible constraint coverage remain invalid. | Validation checks structure, assumptions, full visible constraint coverage, tone, and known unsupported nutrition claims. | Not required for v0. | The validated plan is the source for response formatting. |
| `repairWeeklyLunchPlan` | `WeeklyLunchPlan` | Corrected plan preserving valid content. | Missing required content remains invalid until repaired. | Bounded repair loop allows two attempts, then fails loudly. | Not required for v0. | Repaired plan must pass validation before formatting. |
| `formatWeeklyLunchPlanResponse` | `String` lunch body wrapped with deterministic factual sections in `WeeklyLunchPlanResponse` | LLM writes only the Monday-Friday lunch body; code writes week, constraints, assumptions, and recipe-context note. | Recipe context note is omitted when context is absent. | Prompt receives the validated plan, request constraints, planning horizon, and optional recipe context; application code adds factual boilerplate from typed state. | Not required for v0. | Formatter has all facts it may mention, and factual sections are deterministic. |

## Advanced Pattern Decisions

| Pattern | Decision | Reason |
| --- | --- | --- |
| Stateful loops | Avoid | v0 is a one-shot response. Validation repair is a bounded local loop inside one action, not a persisted conversation or external state loop. |
| Chatbot process | Avoid | The command-line shell accepts a free-text request, but v0 should not pause for setup questions or manage a long-lived conversation. |
| Tool exposure | Avoid for v0 | No external tools are required. Date resolution can be a normal service call if needed. Exposing tools to the LLM would add risk without product value. |
| RAG | Avoid | v0 has no saved recipe backlog or knowledge corpus. Pasted recipe text is already request-local context. |
| Subagents | Avoid | The whole slice is one narrow planning goal. A recipe extraction subagent would be premature until recipe backlog or saved import exists. |

## Implementation Plan

| Status | Work Item | Notes |
| --- | --- | --- |
| done | Add domain types | Request, defaults, horizon, constraints, recipe context, candidate, plan, validation, and response types live under `com.embabel.mealplanner.agent`. |
| done | Add deterministic defaults and planning-horizon action | The default is next week, Monday through Friday; explicit ISO dates and simple week phrases resolve deterministically. |
| done | Add LLM prompt resources for request interpretation, candidate drafting, plan assembly, validation repair, and response formatting | Prompt resources live under `src/main/resources/prompts/meal-planner`. |
| done | Add Embabel agent actions for the GOAP flow | `MealPlannerAgent` wires typed actions from shell-style `UserInput` to `WeeklyLunchPlanResponse`. |
| done | Add shell-facing invocation path | `ingestHouseholdLunchPlanningRequest` adapts Embabel shell `UserInput` into the v0 request fact. |
| done | Add validation and repair logic | Validation checks five weekday lunches, visible constraint handling, rough nutrition notes, assumptions, and non-shaming tone; invalid plans receive up to two repair attempts. |
| done | Add structured-output safeguards | Interpretation uses a tolerant LLM boundary type, absent recipe context normalizes to `null`, and strict domain recipe context still requires a summary. |
| done | Add deterministic factual response sections | The LLM writes the lunch body; application code adds the week, constraints, assumptions, and recipe-context note from typed state. |
| done | Add focused tests | Tests cover deterministic defaults, week resolution, prompt wiring, validation, bounded repair, and annotated agent metadata. |
| pending | Verify with the local shell | Run the v0 acceptance prompt against the supported llama-server shell and inspect the live model response. |

## Tests And Verification

| Status | Check | Notes |
| --- | --- | --- |
| done | `./mvnw test` | Compiles and runs focused tests. |
| done | Default week behavior | No week in the request resolves to next week. |
| done | Five-lunch structure | Validation requires Monday through Friday lunches. |
| done | Constraint handling | Validation requires visible constraint handling when constraints are present. |
| done | Rough nutrition notes | Validation requires a rough nutrition note on each meal. |
| done | Assumptions section | Validation requires assumptions in the plan. |
| done | Non-shaming tone | Validation rejects a small set of moralizing and calorie-compliance terms. |
| done | Structured-output binding acceptance | Tests cover empty optional recipe context normalization through the tolerant LLM boundary type and formatter prompts for absent and present recipe context. |
| done | Constraint coverage acceptance | Tests cover missing and partially surfaced visible constraints. |
| done | Nutrition claim guard | Validation repairs known unsupported nutrient-source claims such as omega-3 from broccoli. |
| done | Repair behavior | Invalid plans trigger a repair prompt and repeated invalid repairs fail after the bounded attempts. |
| done | One-off recipe behavior | Prompting supports pasted recipe text as request-local context and response formatting receives recipe context explicitly. |

## Open Questions

None.

## Assumptions

- The initial implementation target is Kotlin with Spring Boot and the existing Embabel starter dependencies.
- The local shell workflow described in the README is the supported v0 interface.
- The household is always Thomas and Cassandra for v0; there is no configurable household profile yet.
- "High-protein" remains an approximate planning preference, not a numeric compliance target.
- Nutrition notes should be approximate and plain-language unless a later spec adds nutritional data sources.
- The final response can be structured text; v0 does not require machine-readable JSON output for the household.
- The defaults action accepts the request fact as a planning anchor because annotated Embabel actions require an input parameter.

## Flow Challenge Notes

- `LunchPlanningDefaults`, `PlanningHorizon`, `RequestConstraints`, and `WeeklyLunchPlan` are meaningful domain facts. `WeeklyLunchPlanResponse` is presentation output, but it is still an appropriate goal fact because the v0 product is command-line text.
- The only mildly questionable split is `draftLunchCandidates` followed by `assembleWeeklyLunchPlan`. If the first implementation feels too heavy, those can be combined, but the spec keeps them separate because it makes candidate quality and final plan validation easier to reason about.
- Invalid plans should be repaired within the bounded validation repair loop or fail loudly with concrete validation issues.
- No action should mutate external state in v0, so reruns are safe except for LLM cost and nondeterminism.
- Facts do not need to persist across turns in v0.
