# Product Spec v1: Household Profile

## Goal

Add a reusable household profile so future planning requests can use stable preferences without repeating them every time.

When no profile exists, the product guides the household through creating one before or during the first profile-aware planning request.

## Added Product Unit

`Household profile`

The profile stores household-level defaults and person-specific hard constraints for Thomas and Cassandra.

## Interface

The household can save or update profile facts through the command-line conversation:

```text
Remember that Cassandra dislikes mushrooms and Thomas prefers high-protein lunches.
```

The household can then plan with a shorter prompt:

```text
Plan lunches for next week.
```

If no profile exists, the product asks for the minimum useful household facts and creates the profile from the answers.

## Product Behavior

The product should remember:

* allergies and intolerances
* strong shared dislikes
* Thomas-specific hard dislikes or preferences
* Cassandra-specific hard dislikes or preferences
* usual weekday lunch count
* preferred cooking effort
* pantry staples, when provided

Planning responses should show which saved profile assumptions influenced the plan.

Profile setup should not become a long onboarding flow. The product should collect safety-critical and high-impact preferences first, then continue planning with explicit assumptions for anything missing.

## Minimal Persistence

Use local single-household storage.

The store only needs to persist one household profile on the developer machine. It does not need accounts, synchronization, conflict resolution, or multi-household support.

## Supported Workflow

1. Save household facts.
2. If no profile exists, answer the guided setup questions.
3. Ask for a weekly lunch plan.
4. Receive a plan that uses saved preferences and request-specific constraints.
5. Update a profile fact and plan again.

## Acceptance Checks

* Profile facts can be saved locally.
* If no profile exists, the product can create one through guided conversation.
* A later planning request uses saved dislikes and preferences.
* Person-specific constraints for Thomas and Cassandra can both influence one shared household plan.
* The final plan distinguishes saved assumptions from constraints stated in the current request.

## Exclusions

v1 does not include browser profile screens, recipe backlog, saved recipe text import, shopping lists, feedback memory, or multiple household profiles.
