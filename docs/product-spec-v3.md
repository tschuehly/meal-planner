# Product Spec v3: Plan Revision and Shopping List

## Goal

Add plan refinement and shopping-list generation to the command-line workflow.

The household can ask for a meal replacement, accept the plan, and generate a practical shopping list grouped by store section.

## Added Product Units

| Product unit | Purpose |
| --- | --- |
| Plan revision | Replace or adjust one meal while preserving the rest of the plan. |
| Plan acceptance | Mark a generated plan as ready for shopping. |
| Shopping list | Convert an accepted plan into grouped ingredients. |

## Interface

Example requests:

```text
Replace Wednesday with something easier.
```

```text
This plan is okay.
```

```text
I want to go shopping for next week.
```

## Product Behavior

Plan revision should preserve good parts of the current plan instead of regenerating everything by default. The product may continue suggesting adjustments until the household accepts the plan.

Shopping-list generation should be user-triggered. The list should group items by store section and separate pantry assumptions from items to buy.

Command-line shopping lists are displayed as text, but the saved shopping list should still be structured so later requests can modify it.

## Minimal Persistence

Extend local single-household storage with:

* the current draft plan
* acceptance status
* the current shopping list
* pantry assumptions used for the list

Only the current active plan needs to be stored in v3. Historical plans can wait.

## Supported Workflow

1. Generate a weekly lunch plan.
2. Ask for one meal to be replaced or simplified.
3. Review the revised plan and keep adjusting until it is accepted.
4. Accept the plan.
5. Ask to go shopping for next week.
6. Generate a grouped shopping list.

## Acceptance Checks

* A revision can replace one meal without regenerating every meal.
* The revised plan still respects household constraints.
* Shopping-list generation is triggered by the household.
* The shopping list is grouped by store section.
* The shopping list is saved as structured data even when shown as text.
* Pantry assumptions are listed separately from items to buy.

## Exclusions

v3 does not include browser UI, shopping item checkboxes, grocery ordering, plan history, or feedback memory.
