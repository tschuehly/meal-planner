# Meal Planner Product Specs

## Purpose

The product is built as a sequence of small, demonstrable household meal-planning workflows. Each version must work end to end before the next version adds another product feature, interface, or stored data type.

The first version proves that the household can ask for a useful weekly lunch plan through the simplest available interface.

The project also has a separate Embabel showcase track. Product specs describe household behavior only; Embabel planning is captured in [agents/meal-planner-embabel-showcase-spec.md](agents/meal-planner-embabel-showcase-spec.md).

## Version Map

| Version | Added product unit | Interface | Demonstrates | Spec |
| --- | --- | --- | --- | --- |
| v0 | One-shot weekly lunch planning | Command-line conversation | A useful five-lunch plan from one request | [product-spec-v0.md](product-spec-v0.md) |
| v1 | Household profile | Command-line conversation | Saved household preferences influence future plans | [product-spec-v1.md](product-spec-v1.md) |
| v2 | Recipe backlog and recipe text import | Command-line conversation | Saved recipe ideas and pasted recipes can feed weekly planning | [product-spec-v2.md](product-spec-v2.md) |
| v3 | Plan revision and shopping list | Command-line conversation | A plan can be adjusted, accepted, and turned into a shopping list | [product-spec-v3.md](product-spec-v3.md) |
| v4 | Household web interface | Browser UI | The household can use the planner without command-line interaction | [product-spec-v4.md](product-spec-v4.md) |
| v5 | Weekly feedback memory | Browser UI | Past outcomes improve later plans | [product-spec-v5.md](product-spec-v5.md) |

## Persistence Path

| Version | Minimal persistence |
| --- | --- |
| v0 | None. The plan only needs to exist in the current response. |
| v1 | Local single-household profile storage. |
| v2 | Local recipe backlog storage. |
| v3 | Local current-plan and current-shopping-list storage. |
| v4 | Reuse the local single-household store behind the browser interface, plus private-link or access-code session state. |
| v5 | Local feedback history storage. |

The intended full product is captured in [source-spec.md](source-spec.md). Version specs define product behavior only; implementation planning belongs in the corresponding technical planning work.

## Resolved Product Decisions

| Source-spec question | Decision for planned slices |
| --- | --- |
| Access model | v4 uses a private household link or access code. Once accepted, the browser remembers access through session or cookie state. |
| First-time profile setup | Starting in v1, the product guides profile setup in conversation when no profile exists. In v4, the web UI exposes the same guided setup path. |
| Weekly planning interface | Command-line conversation for v0-v3. Single compact web form with free-text context in v4. |
| Week default | Weekly planning always means next week unless the household explicitly says otherwise. |
| Recipe entry | Pasted recipe text becomes a recipe-backlog import workflow in v2. v0 may use pasted recipe text only as one-off planning context. |
| Nutrition information | Nutrition matters from v0. Plans show rough nutrition notes, especially protein and practical meal balance, without calorie compliance scoring. |
| Shopping trigger | No scheduled shopping date. Shopping is user-triggered through requests such as wanting to shop for next week. |
| Experimental recipes | At most one new or experimental recipe per week by default, unless the household asks for more. |
| Plan acceptance role | One shared household role; no Cassandra-specific acceptance gate. |
| Feedback completion | Feedback can loop until the household accepts it, with skip still available for low-friction use. |
| Shopping list format | Command-line versions return text lists, while persisted shopping lists are still stored as structured product data. |
| v4 dashboard and interaction defaults | Dashboard remains primary home. Recipe exploration is button-based. Checked shopping items mute rather than auto-collapse. Cross-tab updates can use refresh or reload. |

## Versioning Rule

Each version must include:

* one primary product addition
* one demonstrable household workflow
* clear data that must be remembered, if any
* local acceptance checks
* explicit exclusions so the iteration remains small
