# Product Spec v2: Recipe Backlog and Recipe Text Import

## Goal

Add recipe capture and discovery as planning inputs without building a browser interface.

The household can collect recipe ideas, paste recipe text, and ask weekly planning to consider saved backlog items.

## Added Product Unit

`Recipe backlog`

The backlog stores recipe ideas and normalized recipes the household might want to try.

## Interface

The household can ask:

```text
Find three easy lunch ideas we might want to try.
```

The household can save a candidate:

```text
Save the turkey wrap idea to the backlog.
```

The household can paste a recipe:

```text
Save this as a recipe we can use for lunches: [recipe text]
```

The household can plan with backlog context:

```text
Plan next week's lunches and consider one backlog recipe.
```

## Product Behavior

Recipe ideas should include:

* recipe name
* rough effort
* lunch suitability
* household fit notes
* main ingredients
* step-by-step instructions when available
* reason to save or skip

Pasted recipe text should be converted into a useful recipe object with a title, ingredients, step-by-step instructions, effort, serving notes, and household fit notes.

Weekly planning should include at most one new or experimental backlog recipe by default. If no backlog recipe fits the week, the response should say why.

## Minimal Persistence

Extend local single-household storage with a recipe backlog.

Each saved recipe needs enough information to be useful later: title, ingredients, step-by-step instructions when available, fit notes, effort, source note if available, and saved date.

## Supported Workflow

1. Ask for recipe ideas.
2. Save one or more recipe ideas to the backlog.
3. Paste a recipe text and save it as a normalized recipe.
4. Ask for a weekly lunch plan.
5. Receive a plan that uses or explicitly skips backlog recipes.

## Acceptance Checks

* Recipe ideas can be generated from a household request.
* A recipe can be saved to the backlog.
* Pasted recipe text can be converted into a saved recipe with ingredients and step-by-step instructions.
* Saved recipes survive restarting the app.
* Weekly planning can include a saved recipe.
* The plan respects profile and request constraints when considering backlog recipes.
* The response explains why a backlog recipe was selected or skipped.

## Exclusions

v2 does not include browser recipe exploration, automatic recipe website import, external grocery integrations, shopping lists, or feedback memory.
