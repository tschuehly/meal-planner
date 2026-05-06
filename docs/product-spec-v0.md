# Product Spec v0: One-Shot Weekly Lunch Planning

## Goal

Build the smallest useful meal-planning product experience.

The household can ask for a simple next-week lunch plan for Thomas and Cassandra and receive one concrete five-lunch plan with practical explanations, rough nutrition notes, and assumptions.

## Interface

v0 uses a command-line conversation.

The household enters one free-text request, for example:

```text
Plan five practical weekday lunches for Thomas and Cassandra. Keep it high-protein, easy to cook, and avoid mushrooms.
```

The product returns a structured plan in one response.

If the household pastes recipe text into the request, v0 may use it as one-off planning context. It does not save the recipe for later.

## Product Behavior

The response must include:

* five weekday lunch meals
* a short reason each meal fits
* rough nutrition notes for each meal, especially protein and meal balance
* visible handling of explicit dislikes, allergies, intolerances, or constraints from the request
* prep or leftover notes where useful
* assumptions the household can correct in a later request

The product should make a reasonable plan when the request is underspecified. It should use defaults instead of blocking the household with setup questions.

Weekly planning means next week unless the household explicitly asks for a different week.

## Household Defaults

| Category | Default |
| --- | --- |
| Planning unit | Weekday lunches |
| Number of meals | 5 |
| Effort | Easy to moderate |
| Health tone | High-protein and practical, with rough nutrition notes and without calorie compliance scoring |
| Household style | Meals should work for both Thomas and Cassandra |
| Required safety behavior | Avoid explicit allergies, intolerances, and strong dislikes from the request |

## Minimal Persistence

v0 does not need durable persistence.

The generated plan only needs to exist in the current command-line response. This keeps the first version focused on whether the household receives a useful plan.

## Supported Workflow

1. Start the command-line conversation.
2. Enter one weekly lunch-planning request.
3. Receive one structured five-lunch plan.
4. Review assumptions and decide what to ask for next.

## Output Format

The response should be structured:

```text
Weekly lunch plan

Monday: ...
Why it fits: ...
Nutrition note: ...

Tuesday: ...
Why it fits: ...
Nutrition note: ...

...

Assumptions
- ...

Adjustments you can ask for
- Make the week easier
- Replace a meal
- Avoid another ingredient
```

## Acceptance Checks

* A single request can produce a five-lunch plan.
* If no week is stated, the plan is for next week.
* The response reflects request constraints such as disliked ingredients.
* The response includes rough nutrition notes.
* The response includes assumptions used for missing details.
* The tone is practical and non-shaming.
* The product can handle an underspecified request by using defaults.

## Exclusions

v0 does not include:

* browser UI
* stored household profile
* stored plans
* recipe backlog
* saved recipe import or discovery
* plan acceptance
* shopping-list generation
* feedback memory
* authentication
* separate Thomas and Cassandra interfaces
