# Source Spec: Personal Meal Planning Agent

## Context / Summary

### Goal

Build a private household meal-planning web app that helps **Thomas and Cassandra** decide what to eat, generate a realistic weekly lunch-first plan, and create a useful shopping list with low friction.

### Big Picture

This is a private assistant for one household, not a public SaaS meal planner and not a calorie-tracking app. The product should make weekly lunch planning easier than doing it manually by combining stable household preferences, weekly constraints, recipe backlog ideas, practical health defaults, ingredient reuse, plan acceptance, shopping-list generation, and weekly feedback.

### Details

The app supports a shared household interface used by either Thomas or Cassandra. There is **one UI role**: **Household Member**. Thomas and Cassandra are still represented separately inside the profile for food preferences, dislikes, and feedback, but the UI does not have different permissions, dashboards, or separate role-specific flows.

The primary planning unit is **weekday lunch for next week**. The default plan is **5 weekday lunches**, with optional dinners added only when requested. Breakfast remains profile-based and mostly unchanged unless ingredients are needed for the shopping list.

The app should also include an always-available **Recipe Explorer** and recipe text import. Household members can discover or capture recipes at any time and add them to a **Recipe Backlog**. During weekly planning, especially before shopping, the agent can suggest backlog recipes that fit the week.

Household access is private. The web app accepts a private household link or access code and remembers access through session or cookie state.

### Non-obvious Decisions

| Decision                                                                                  | Status                             |
| ----------------------------------------------------------------------------------------- | ---------------------------------- |
| One shared household UI role for Thomas and Cassandra                                     | Locked                             |
| No separate Thomas/Cassandra UX permissions                                               | Locked                             |
| Thomas and Cassandra still have separate preference sections inside the household profile | Locked                             |
| Lunch-first planning                                                                      | Locked                             |
| Default: 5 weekday lunches                                                                | Locked from product recommendation |
| Optional dinners, secondary to lunch                                                      | Locked                             |
| Breakfast is default-based, not actively planned weekly                                   | Locked                             |
| Shopping list generated after plan acceptance and a user-triggered shopping request       | Locked                             |
| Recipe Explorer is always available                                                       | Locked                             |
| Recipe Backlog can feed into weekly planning                                              | Locked                             |
| Pasted recipe text can be normalized into a saved recipe                                  | Locked                             |
| Health is supportive, approximate, and non-shaming, with rough nutrition notes            | Locked                             |
| Exact calorie compliance, grocery ordering, public SaaS, and multi-household support      | Out of scope                       |

---

## Product decisions

| Decision area | Intended behavior |
| --- | --- |
| Access | Private household link or access code, remembered through session or cookie state. |
| Household role | One shared **Household Member** role. Thomas and Cassandra appear as people in profile, preferences, meal notes, and feedback. |
| First-time setup | The product guides profile setup when no household profile exists. Required safety basics are collected first; optional fields can remain blank. |
| Planning entry | Dashboard is the primary home. Weekly planning defaults to next week unless the household explicitly chooses a different week. |
| Planning interface | Browser UI uses a compact planning form with free-text context. |
| Recipe capture | Recipe Explorer and pasted recipe text both feed the Recipe Backlog. |
| Shopping | Shopping is triggered by the household. There is no scheduled shopping date. |
| Feedback | Feedback can be clarified until the household accepts the summary or skips the week. |
| Mobile behavior | Mobile uses stacked cards, visible primary actions, and compact navigation. |
| Cross-tab behavior | Screens can refresh state on focus, page load, or manual reload. Real-time synchronization is not required. |

---

## Roles — UI visibility lens

### Role decision

There is exactly **one interactive UI role**:

> **Household Member** — either Thomas or Cassandra using the same shared household interface.

Thomas and Cassandra are not separate UI roles. They are household people whose preferences can appear inside profile fields, meal notes, and feedback labels.

| Actor               |          Is this a UI role? | What they see                                                                                                      | What they can do from the UI                                                                                                                 | Notes                                                                                     |
| ------------------- | --------------------------: | ------------------------------------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------- |
| Household Member    |                         Yes | All household screens: dashboard, profile, planner, plan review, recipe explorer, recipe backlog, shopping list, feedback | Start planning, edit inputs, revise meals, accept plan, generate shopping list, check shopping items, give feedback, add recipes to backlog | This role represents either Thomas or Cassandra. No per-person UI permission split.       |
| Thomas              |         No separate UI role | Appears as a person inside profile/preferences/feedback                                                            | Same as Household Member                                                                                                                     | The UI may show “Thomas preferences” as a profile section, not as a permission boundary.  |
| Cassandra           |         No separate UI role | Appears as a person inside profile/preferences/feedback                                                            | Same as Household Member                                                                                                                     | Cassandra has no separate login-specific journey. |
| Meal Planning Agent | System actor, not a UI role | Generates visible drafts, revisions, explanations, shopping lists, and feedback prompts                            | Does not “click”; responds to household actions                                                                                              | Architecture/services are out of scope.                                                   |

### Per-role visibility principle

Because there is one UI role, every household-facing screen is visible to any household member. The visibility question is therefore not “Thomas vs Cassandra,” but:

> “Does this component appear to the household member in this screen/state?”

---

## Entry points

### Global navigation

The app shell should expose these primary areas:

| Nav item  | Destination                     | Visible to Household Member | Purpose                                      |
| --------- | ------------------------------- | --------------------------: | -------------------------------------------- |
| Dashboard | `/`                             |                         Yes | Shows planned week state and next action.    |
| Plan      | `/plans/current` or `/plan/new` |                         Yes | Start or review weekly planning.             |
| Recipes   | `/recipes/backlog`              |                         Yes | View backlog and enter Recipe Explorer.      |
| Shopping  | `/shopping/current`             |                         Yes | Continue current shopping list if generated. |
| Profile   | `/profile`                      |                         Yes | Edit household preferences and defaults.     |
| Feedback  | `/feedback/current`             |                         Yes | Give end-of-week feedback when available.    |

### Primary entry points

| Entry point                  | User navigates from                                             | Destination                                                | Button/link copy                    | Notes                                                   |
| ---------------------------- | --------------------------------------------------------------- | ---------------------------------------------------------- | ----------------------------------- | ------------------------------------------------------- |
| Start weekly planning        | Dashboard empty/current-week card                               | `/plan/new?weekStart=YYYY-MM-DD`                           | `Plan next week` | Main planning entry point.                                    |
| Review active draft          | Dashboard active draft card                                     | `/plans/{planId}`                                          | `Review draft plan`                 | Appears after generation.                               |
| Continue accepted plan       | Dashboard accepted plan card                                    | `/plans/{planId}`                                          | `Open weekly plan`                  | Shows accepted meals and shopping state.                |
| Generate shopping list       | Accepted plan page                                              | `/plans/{planId}/shopping-list`                            | `Generate shopping list`            | Only after acceptance.                                    |
| Continue shopping            | Dashboard or nav                                                | `/shopping/current`                                        | `Open shopping list`                | Redirects to active list if one exists.                 |
| Recipe discovery             | Global nav, dashboard, empty recipe backlog, meal swap dead end | `/recipes/explore`                                         | `Explore recipes`                   | Always available.                                       |
| View recipe backlog          | Global nav or planning form                                     | `/recipes/backlog`                                         | `Recipe backlog`                    | Shows recipes to try.                                   |
| Add backlog recipe into plan | Weekly planning input or draft plan                             | `/recipes/backlog?mode=selectForPlan&weekStart=YYYY-MM-DD` | `Add from backlog`                  | Lets household choose backlog candidates for next week. |
| End-of-week feedback         | Dashboard prompt or plan page                                   | `/plans/{planId}/feedback`                                 | `Give quick feedback`               | Appears near or after week end.                         |
| Edit preferences             | Dashboard/profile reminder                                      | `/profile`                                                 | `Edit household profile`            | Available anytime.                                      |

### URL params, cookies, and tokens — UX needs only

| URL                                                        | UX parameter need   | Purpose                                                                         |               |                   |                                                                      |
| ---------------------------------------------------------- | ------------------- | ------------------------------------------------------------------------------- | ------------- | ----------------- | -------------------------------------------------------------------- |
| `/plan/new?weekStart=YYYY-MM-DD`                           | `weekStart`         | Anchors planning to a specific week. Defaults to next week when omitted.                                            |               |                   |                                                                      |
| `/plans/{planId}`                                          | `planId`            | Opens the specific weekly plan.                                                 |               |                   |                                                                      |
| `/plans/{planId}?focus=meal:{mealSlotId}`                  | Optional `focus`    | Deep-links to a specific meal card after revision or error.                     |               |                   |                                                                      |
| `/plans/{planId}/shopping-list`                            | `planId`            | Opens shopping list for an accepted plan.                                       |               |                   |                                                                      |
| `/plans/{planId}/feedback`                                 | `planId`            | Opens feedback for that week.                                                   |               |                   |                                                                      |
| `/recipes/explore?source=nav                               | dashboard           | swap                                                                            | emptyBacklog` | Optional `source` | Lets UI return the household member to the correct previous context. |
| `/recipes/backlog?mode=selectForPlan&weekStart=YYYY-MM-DD` | `mode`, `weekStart` | Shows backlog in weekly-planning selection mode.                                |               |                   |                                                                      |
| Private household link or access code                      | Required            | Establishes shared household access and stores it in session or cookie state. |               |                   |                                                                      |
| Per-person role token                                      | Not needed | There are no separate Thomas/Cassandra role views.                              |               |                   |                                                                      |

---

## User journeys

## Journey 1 — First-time household setup

### Goal

Let the household create enough profile context to generate a first useful plan without a long onboarding process.

### Big Picture

The household should not be blocked by a full profile, but the app must capture hard constraints before suggesting meals.

### Details

1. Household member opens `/`.
2. Dashboard detects no profile or incomplete profile.
3. Screen shows: “Let’s set the basics before the first plan.”
4. Required minimal fields:

    * allergies / intolerances
    * strong shared dislikes
    * Thomas-specific hard dislikes, if any
    * Cassandra-specific hard dislikes, if any
    * usual weekday lunch count, default 5
    * preferred cooking effort, default moderate/easy
5. Optional fields are visible but skippable:

    * pantry staples
    * kitchen equipment
    * health priority
    * breakfast defaults
    * budget sensitivity
6. Household member clicks `Save basics`.
7. App returns to dashboard with `Plan next week`.

### Empty/error/escape branches

| Branch                              | UX behavior                                                                        |
| ----------------------------------- | ---------------------------------------------------------------------------------- |
| Household skips optional fields     | App allows planning and labels assumptions clearly in generated plans.             |
| Required hard constraints missing   | Inline field message: “Add allergies/intolerances or choose ‘None known’.”         |
| Save fails                          | Keep form values, show top banner: “Couldn’t save profile. Try again.”             |
| Household wants to plan immediately | Button: `Use defaults and plan` appears after required safety basics are answered. |

### Completion and return path

Completion returns to `/` with the next action card: `Plan next week`.

---

## Journey 2 — Weekly planning, happy path

### Goal

Generate a concrete lunch-first weekly plan and accept it before creating a shopping list.

### Big Picture

Weekly planning should feel like confirming defaults, adding constraints, and reviewing one opinionated proposal, not choosing from a huge recipe catalog.

### Details

1. Household member starts at dashboard.
2. Clicks `Plan next week`.
3. Opens `/plan/new?weekStart=YYYY-MM-DD`.
4. Weekly planning screen shows compact defaults:

    * week start date
    * 5 lunches
    * dinners: none by default, optional 0–3
    * priority: high-protein, practical, not too much effort
    * special days
    * ingredients to use
    * cravings/dislikes next week
    * candidate recipes from backlog
    * free-text context
5. Household member optionally selects backlog recipes to consider.
6. Clicks `Generate draft plan`.
7. Loading state shows: “Building a realistic lunch plan…”
8. Draft plan appears at `/plans/{planId}`.
9. Household reviews:

    * meals by day
    * effort
    * rough nutrition labels
    * why selected
    * ingredient reuse
    * prep notes
    * fallback meal
10. Household clicks `Accept plan`.
11. Accepted state appears.
12. Household clicks `Generate shopping list`.
13. Shopping list appears grouped by store section.

### Empty/error/escape branches

| Branch                     | UX behavior                                                                                                                      |
| -------------------------- | -------------------------------------------------------------------------------------------------------------------------------- |
| No backlog recipes         | Planning form shows empty state: “No recipes in backlog yet. You can still generate a plan.” Button: `Explore recipes`.          |
| No ingredients to use      | Field remains empty; helper text says “Optional — add fridge/pantry items you want to use.”                                      |
| Generation fails           | Error card: “Couldn’t create a plan. Keep your inputs and try again.” Buttons: `Try again`, `Simplify next week`, `Edit inputs`. |
| Plan feels wrong           | Buttons on plan: `Revise plan`, `Swap meal`, `Make easier`, `Start from scratch`.                                                |
| User exits before acceptance | Dashboard shows draft plan card with `Continue review`.                                                                          |

### Completion and return path

After shopping list generation, return path is:

`Shopping list → Weekly plan → Dashboard`.

---

## Journey 3 — Weekly planning with recipe backlog integration

### Goal

Let the household discover recipes before planning, then let the agent suggest backlog recipes that fit the weekly plan.

### Big Picture

Recipe discovery is not a dead-end activity. It feeds the weekly planning loop.

### Details

1. Household member opens `Recipes` from nav.
2. Recipe backlog page shows recipes marked:

    * `Want to try`
    * `Good for lunch`
    * `Good for dinner`
    * `Repeat`
    * `Avoid`
3. Household clicks `Explore recipes`.
4. Recipe Explorer shows one recipe card at a time, or a compact list/card interface.
5. Household can choose:

    * `Save to backlog`
    * `Not interested`
    * `Maybe later`
    * `Open details`
6. Saved recipes appear in backlog.
7. On weekly planning day, planning form shows:

    * “Backlog recipes that might fit next week”
    * suggested candidates with reason labels
8. Household can select zero or more backlog recipes for consideration.
9. Agent integrates at most one experimental/new recipe per week by default unless user explicitly requests more.

### Empty/error/escape branches

| Branch                                    | UX behavior                                                                         |
| ----------------------------------------- | ----------------------------------------------------------------------------------- |
| Backlog empty                             | Show `Explore recipes` as forward action.                                           |
| Explorer has no suggestions               | Show “No suggestions right now.” Buttons: `Add recipe manually`, `Back to backlog`. |
| Household rejects all current suggestions | Show `Explore more recipes` and `Generate from trusted catalog`.                    |
| Saved recipe no longer fits planned week  | Show it as disabled or lower priority with reason: “Too much effort for next week.” |

### Completion and return path

Recipe Explorer return path depends on source:

| Source             | Return path                               |
| ------------------ | ----------------------------------------- |
| Nav                | `/recipes/backlog`                        |
| Planning form      | `/plan/new?weekStart=YYYY-MM-DD`          |
| Meal swap dead end | `/plans/{planId}?focus=meal:{mealSlotId}` |
| Dashboard          | `/`                                       |

---

## Journey 4 — Meal revision and swaps

### Goal

Let the household change only the problematic part of a plan without regenerating everything.

### Big Picture

Revision should be precise. The app should preserve what works and modify only the selected meal or theme.

### Details

1. Household member opens a draft plan.
2. On a meal card, clicks one of:

    * `Swap`
    * `Make easier`
    * `Make cheaper`
    * `Higher protein`
    * `Remove ingredient`
    * `Use what we have`
3. A revision drawer opens.
4. Drawer asks for optional context:

    * “What should change?”
    * “Any ingredient to avoid?”
5. Household clicks `Revise this meal`.
6. The selected card shows loading state.
7. Replacement appears with:

    * new title
    * why it fits
    * changed ingredients
    * effect on reuse/shopping
8. Household clicks `Keep change` or `Undo`.

### Empty/error/escape branches

| Branch                                       | UX behavior                                                                                                                      |
| -------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- |
| No suitable swap found                       | Show: “I don’t have a good swap from the trusted catalog.” Buttons: `Explore recipes`, `Use fallback meal`, `Relax constraints`. |
| Revision fails                               | Original meal remains. Error message appears inside drawer.                                                                      |
| Removing an ingredient breaks multiple meals | Show warning: “This affects 3 meals using yogurt sauce.” Buttons: `Revise affected meals`, `Cancel`.                             |
| Household wants full reset                   | Button: `Regenerate whole plan` with confirmation.                                                                               |

### Completion and return path

After revision, household returns to the same plan page and focused meal slot.

---

## Journey 5 — Plan acceptance and shopping list

### Goal

Accept the weekly plan first, then generate an editable, grouped shopping list.

### Big Picture

The shopping list should not be generated from an unstable draft. The plan must reach an accepted state first.

### Details

1. Household member opens draft plan.
2. Reviews meal cards, reuse notes, fallback, and optional dinners.
3. Clicks `Accept plan`.
4. Accepted state appears:

    * “Plan accepted”
    * `Generate shopping list`
5. Household clicks `Generate shopping list`.
6. Loading state shows: “Creating grouped shopping list…”
7. Shopping list appears:

    * grouped sections
    * quantities
    * pantry assumptions
    * editable items
    * checkboxes
    * add item field
8. Household checks items while shopping.
9. Checked items remain visible and muted.

### Empty/error/escape branches

| Branch                                   | UX behavior                                                                              |
| ---------------------------------------- | ---------------------------------------------------------------------------------------- |
| Plan has no meals                        | Acceptance disabled with message: “Add at least one planned meal before acceptance.”         |
| Shopping list generation fails           | Keep accepted plan. Show `Try again` and `Edit plan`.                                    |
| Shopping section empty                   | Hide section by default or show compact “No dairy needed.”                               |
| Pantry assumptions empty                 | Show “No pantry assumptions listed.” Button: `Edit pantry assumptions`.                  |
| User edits plan after shopping generated | Show banner: “Plan changed. Shopping list may need updating.” Button: `Regenerate list`. |

### Completion and return path

Shopping list terminal state has forward actions:

* `Back to plan`
* `Give feedback after the week`
* `Plan another week`
* `Explore recipes`

---

## Journey 6 — Weekly feedback

### Goal

Collect short end-of-week feedback so future plans improve and ask fewer questions.

### Big Picture

Feedback should be lightweight and useful, not a review chore.

### Details

1. Dashboard shows feedback prompt near the end of or after the planned week.
2. Household member clicks `Give quick feedback`.
3. Feedback screen asks:

    * Which meal should we repeat?
    * Which meal should we avoid?
    * Was cooking too much effort?
    * Did anything go unused?
    * Should next week be easier, healthier, cheaper, or more varied?
4. Household can answer with quick chips and optional free text.
5. Clicks `Save feedback`.
6. Completion screen confirms:

    * repeat meals saved
    * avoid meals saved
    * unused ingredients noted
    * next-week bias captured

### Empty/error/escape branches

| Branch                              | UX behavior                                                                                   |
| ----------------------------------- | --------------------------------------------------------------------------------------------- |
| No accepted plan exists             | Feedback page shows empty state: “No completed plan to review yet.” Button: `Plan next week`. |
| Household gives ambiguous feedback  | Ask a short follow-up question before saving the feedback summary.                            |
| Household skips meal-level feedback | Allow `Skip this week`.                                                                       |
| Save fails                          | Keep answers, show retry banner.                                                              |
| Feedback already submitted          | Show summary and `Edit feedback`.                                                             |

### Completion and return path

After feedback, app returns to dashboard with next action:

`Plan next week`.

---

## Screens

---

## Screen 1 — App shell / navigation

**Path:** Persistent across app
**Role:** Household Member
**Purpose:** Provide stable navigation to planning, recipes, shopping, feedback, and profile.

### Components visible

| Component             |    Visible? | Behavior                                    |
| --------------------- | ----------: | ------------------------------------------- |
| App title             |         Yes | Opens dashboard.                            |
| Dashboard nav         |         Yes | Opens `/`.                                  |
| Plan nav              |         Yes | Opens current plan or new planning entry.   |
| Recipes nav           |         Yes | Opens recipe backlog.                       |
| Shopping nav          |         Yes | Opens current shopping list or empty state. |
| Profile nav           |         Yes | Opens household profile.                    |
| Feedback prompt badge | Conditional | Appears when feedback is due.               |
| Mobile menu           | Mobile only | Collapses nav into menu or bottom tabs.     |

### Button behavior

| Button      | Behavior                                                                  |
| ----------- | ------------------------------------------------------------------------- |
| `Dashboard` | Navigate to `/`.                                                          |
| `Plan`      | If active plan exists, open it. Otherwise open `/plan/new?weekStart=...`. |
| `Recipes`   | Navigate to `/recipes/backlog`.                                           |
| `Shopping`  | Open current shopping list if available; otherwise shopping empty state.  |
| `Profile`   | Navigate to `/profile`.                                                   |

### Empty / error states

If current plan lookup fails, nav still renders and Dashboard shows recoverable error.

### Mobile vs desktop

Desktop uses top or left nav. Mobile uses bottom nav or hamburger menu with primary action button preserved.

### ASCII mockup

```text
+-------------------------------------------------------------+
| Meal Planner                  [Dashboard] [Plan] [Recipes]  |
|                               [Shopping] [Profile]          |
+-------------------------------------------------------------+
|                                                             |
|  Page content                                                |
|                                                             |
+-------------------------------------------------------------+

Mobile:
+-----------------------------+
| Meal Planner            ☰   |
+-----------------------------+
| Page content                 |
|                             |
+-----------------------------+
| Home | Plan | Recipes | Shop |
+-----------------------------+
```

---

## Screen 2 — Dashboard

**Path:** `/`
**Role:** Household Member
**Purpose:** Show the current household planning state and the next best action.

### Components visible

| Component                   |    Visible? | Notes                                     |
| --------------------------- | ----------: | ----------------------------------------- |
| Current/next week card      |         Yes | Shows plan state.                         |
| Primary action button       |         Yes | Changes by state.                         |
| Recipe backlog teaser       |         Yes | Shows saved recipes to try.               |
| Shopping status             | Conditional | Appears if list generated.                |
| Feedback prompt             | Conditional | Appears when due.                         |
| Profile completeness prompt | Conditional | Appears when required basics are missing. |

### Per-button behavior

| State                  | Button                   | Behavior                                         |
| ---------------------- | ------------------------ | ------------------------------------------------ |
| No profile basics      | `Set up basics`          | Opens `/profile?mode=basics`.                    |
| No active plan         | `Plan next week`         | Opens `/plan/new?weekStart=YYYY-MM-DD`.          |
| Draft exists           | `Review draft plan`      | Opens `/plans/{planId}`.                         |
| Accepted plan, no list | `Generate shopping list` | Opens accepted plan or shopping-list generation. |
| Shopping list exists   | `Open shopping list`     | Opens `/plans/{planId}/shopping-list`.           |
| Feedback due           | `Give quick feedback`    | Opens `/plans/{planId}/feedback`.                |
| Always                 | `Explore recipes`        | Opens `/recipes/explore?source=dashboard`.       |

### Empty / error states

| State                | UI                                                             |
| -------------------- | -------------------------------------------------------------- |
| No plan              | Friendly empty card: “No plan for next week yet.”              |
| Plan load error      | Error card with `Retry` and `Start new plan`.                  |
| Empty recipe backlog | Teaser says “No saved recipes yet.” Button: `Explore recipes`. |

### Mobile vs desktop

Dashboard cards stack vertically. Primary action is sticky near bottom if no active modal is open.

### ASCII mockup

```text
+-------------------------------------------------------------+
| Dashboard                                                   |
+-------------------------------------------------------------+
| This week                                                   |
| Week of Mon, YYYY-MM-DD                                     |
|                                                             |
| [ No plan yet ]                                             |
| Plan 5 practical high-protein lunches for next week.         |
|                                                             |
| [Plan next week]      [Explore recipes]                     |
+-------------------------------------------------------------+
| Recipe backlog                                              |
| 0 recipes saved to try                                      |
| [Explore recipes]                                           |
+-------------------------------------------------------------+
| Household profile                                           |
| Basics complete: Allergies, dislikes, lunch defaults         |
| [Edit profile]                                              |
+-------------------------------------------------------------+

Draft state:
+-------------------------------------------------------------+
| Draft plan ready                                            |
| 5 lunches • 0 dinners • fallback included                    |
| [Review draft plan]                                         |
+-------------------------------------------------------------+

Accepted state:
+-------------------------------------------------------------+
| Plan accepted                                               |
| Shopping list not generated yet                             |
| [Generate shopping list]                                    |
+-------------------------------------------------------------+
```

---

## Screen 3 — Household profile

**Path:** `/profile`
**Role:** Household Member
**Purpose:** Store stable household preferences so the app asks fewer questions over time.

### Components visible

| Component             | Visible? | Notes                                            |
| --------------------- | -------: | ------------------------------------------------ |
| Household basics      |      Yes | Shared allergies, intolerances, shared dislikes. |
| Thomas preferences    |      Yes | Person-specific likes/dislikes/goals.            |
| Cassandra preferences |      Yes | Person-specific likes/dislikes/wishes.           |
| Pantry staples        |      Yes | Common assumed-at-home items.                    |
| Kitchen equipment     |      Yes | Air fryer, oven, stovetop, etc.                  |
| Lunch defaults        |      Yes | Lunch count, effort, repetition.                 |
| Breakfast defaults    |      Yes | Mostly used for shopping assumptions.            |
| Health priorities     |      Yes | Supportive, approximate, non-judgmental.         |
| Budget sensitivity    |      Yes | Practical budget preference.                     |

### Required before first plan

| Field                   |                     Required? | UX behavior                                         |
| ----------------------- | ----------------------------: | --------------------------------------------------- |
| Allergies/intolerances  | Yes, or explicit “None known” | Prevents first plan generation if unanswered.       |
| Strong dislikes         | Yes, or explicit “None known” | Prevents first plan generation if unanswered.       |
| Cassandra hard dislikes | Yes, or explicit “None known” | Keeps plan Cassandra-aware.                         |
| Thomas hard dislikes    | Yes, or explicit “None known” | Keeps plan household-aware.                         |
| Weekday lunch count     |                Defaulted to 5 | Can proceed with default.                           |
| Preferred effort        |    Defaulted to moderate/easy | Can proceed with default.                           |
| Pantry staples          |                      Optional | Missing values become assumptions in plan/shopping. |
| Health priority         |                      Optional | Defaults to high-protein practical.                 |

### Per-button behavior

| Button              | Behavior                                                |
| ------------------- | ------------------------------------------------------- |
| `Save profile`      | Saves all sections.                                     |
| `Save basics`       | Saves minimum required fields and returns to dashboard. |
| `Add pantry item`   | Adds editable pantry row.                               |
| `Remove`            | Removes row with undo.                                  |
| `Reset to defaults` | Confirmation required.                                  |
| `Plan next week`    | Appears after basics complete; opens weekly planning.   |

### Empty / error states

| State              | UI                                                                         |
| ------------------ | -------------------------------------------------------------------------- |
| No pantry items    | “No pantry staples listed yet. Shopping lists will show more assumptions.” |
| No health priority | “Default: high-protein, practical, not too much effort.”                   |
| Save error         | Top banner, keep field values.                                             |
| Required blank     | Inline message with `None known` option.                                   |

### Mobile vs desktop

Desktop shows sections in two columns where practical. Mobile uses accordion sections.

### ASCII mockup

```text
+-------------------------------------------------------------+
| Household Profile                                           |
+-------------------------------------------------------------+
| Basics                                                      |
| Allergies / intolerances                                    |
| [ None known __________________________ ]                    |
| Shared hard dislikes                                        |
| [ ____________________________________ ]                    |
|                                                             |
| Thomas preferences                                          |
| Likes        [_________________________]                    |
| Hard avoids  [_________________________]                    |
| Health focus [High-protein practical v]                     |
|                                                             |
| Cassandra preferences                                       |
| Likes / wishes [_______________________]                    |
| Hard avoids    [_______________________]                    |
|                                                             |
| Lunch defaults                                              |
| Weekday lunches [5 v]   Effort [Easy / Moderate v]          |
| Repetition      [Some repetition is okay v]                 |
|                                                             |
| Pantry staples                                             |
| - olive oil        [remove]                                 |
| - salt             [remove]                                 |
| [Add pantry item]                                           |
|                                                             |
| [Save basics] [Save profile] [Plan next week]               |
+-------------------------------------------------------------+

Empty pantry:
| No pantry staples listed yet.                               |
| Shopping lists will show more assumptions until you add some.|
| [Add pantry item]                                           |
```

---

## Screen 4 — Weekly planning input

**Path:** `/plan/new?weekStart=YYYY-MM-DD`
**Role:** Household Member
**Purpose:** Collect short weekly context before draft generation.

### Components visible

| Component                   | Visible? | Notes                                                                                                 |
| --------------------------- | -------: | ----------------------------------------------------------------------------------------------------- |
| Week selector               |      Yes | Defaults to next week.                                                                                |
| Lunch count                 |      Yes | Defaults to 5.                                                                                        |
| Dinner inclusion            |      Yes | Defaults to none.                                                                                     |
| Special day constraints     |      Yes | No cooking, eating out, travel, busy days.                                                            |
| Ingredients to use          |      Yes | Free-text or chips.                                                                                   |
| Cravings/dislikes next week |      Yes | Temporary weekly modifiers.                                                                           |
| Priority chips              |      Yes | Easy, cheap, high-protein, lighter, comforting, varied, use existing ingredients, meal-prep friendly. |
| Recipe backlog candidates   |      Yes | Shows candidate recipes if backlog exists.                                                            |
| Free-text context           |      Yes | Optional flexible instruction box.                                                                    |

### Per-button behavior

| Button                | Behavior                                                         |
| --------------------- | ---------------------------------------------------------------- |
| `Generate draft plan` | Starts draft generation; blocking loading state.                 |
| `Add from backlog`    | Opens `/recipes/backlog?mode=selectForPlan&weekStart=...`.       |
| `Explore recipes`     | Opens `/recipes/explore?source=planning`.                        |
| `Use easy mode`       | Sets fewer cooked meals, more leftovers/fallbacks, lower effort. |
| `Cancel`              | Returns to dashboard or previous plan.                           |

### Empty / error states

| State                           | UI                                                                                            |
| ------------------------------- | --------------------------------------------------------------------------------------------- |
| Recipe backlog empty            | “No saved recipes yet. You can still generate from the trusted catalog.”                      |
| No candidate recipes fit        | “Nothing in backlog looks ideal for next week.” Buttons: `Ignore backlog`, `Explore recipes`. |
| Required profile basics missing | Inline card links to profile basics.                                                          |
| Generation error                | Keep inputs, show retry/simplify/edit options.                                                |

### Mobile vs desktop

Mobile shows form as stacked cards. Priority chips wrap. Primary action remains sticky at bottom.

### ASCII mockup

```text
+-------------------------------------------------------------+
| Plan a Week                                                 |
+-------------------------------------------------------------+
| Week                                                        |
| [ Mon YYYY-MM-DD v ]                                        |
|                                                             |
| Meals                                                       |
| Lunches [5 v]                                               |
| Dinners [None v]  (optional, secondary)                     |
|                                                             |
| Priority next week                                          |
| [High-protein] [Practical] [Not too much effort]            |
| [Easy] [Cheap] [Lighter] [Comforting] [Varied]              |
| [Use existing ingredients] [Meal-prep friendly]             |
|                                                             |
| Special days / constraints                                  |
| [ e.g. Wednesday no cooking, Friday flexible __________ ]    |
|                                                             |
| Use ingredients already at home                             |
| [ e.g. peppers, quark, eggs ____________________________ ]   |
|                                                             |
| Cravings, dislikes, avoid next week                         |
| [ _____________________________________________________ ]    |
|                                                             |
| Recipe backlog candidates                                   |
| No saved recipes yet.                                       |
| [Explore recipes]                                           |
|                                                             |
| Extra context                                               |
| [ Anything else the agent should know next week _______ ]    |
|                                                             |
| [Cancel]                              [Generate draft plan] |
+-------------------------------------------------------------+

Loading:
+-------------------------------------------------------------+
| Building a realistic lunch plan...                          |
| Checking preferences, reuse, effort, and fallback options.   |
| [progress indicator]                                        |
+-------------------------------------------------------------+
```

---

## Screen 5 — Weekly plan review

**Path:** `/plans/{planId}`
**Role:** Household Member
**Purpose:** Review, revise, and accept the generated weekly plan.

### Components visible

| Component                |      Visible? | Notes                                              |
| ------------------------ | ------------: | -------------------------------------------------- |
| Plan status banner       |           Yes | Draft, accepted, shopping generated, feedback due. |
| Week summary             |           Yes | Lunch count, dinners, priority, effort.            |
| Meal cards               |           Yes | One card per planned meal/flexible slot.           |
| Optional dinners section |   Conditional | Empty state if none.                               |
| Ingredient reuse notes   |           Yes | Required in every plan.                            |
| Prep-ahead notes         |           Yes | Practical guidance.                                |
| Fallback meal            |           Yes | Required.                                          |
| Assumptions              |           Yes | Flags uncertainty.                                 |
| Acceptance controls        |    Draft only | `Accept plan`, `Revise plan`.                     |
| Shopping CTA             | Accepted only | `Generate shopping list` / `Open shopping list`.   |

### Meal card content

Each meal card should show:

* day and meal type
* title
* short reason
* effort level
* rough protein label
* rough calorie/portion note when available
* leftover/packability note
* reused ingredients
* buttons for precise revision

### Per-button behavior

| Button                   | Behavior                                                                                                                                    |
| ------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------- |
| `Swap`                   | Opens revision drawer for selected meal.                                                                                                    |
| `Make easier`            | Revises selected meal toward lower effort.                                                                                                  |
| `Make cheaper`           | Revises selected meal toward cheaper ingredients.                                                                                           |
| `Higher protein`         | Revises selected meal toward higher protein.                                                                                                |
| `Remove ingredient`      | Opens ingredient-specific revision input.                                                                                                   |
| `Add Cassandra pick`     | Adds or biases one meal toward Cassandra preference. Copy may still appear even with one shared role because Cassandra is a profile person. |
| `Add comfort meal`       | Revises plan to include one comfort-style meal.                                                                                             |
| `Regenerate whole plan`  | Confirmation required.                                                                                                                      |
| `Accept plan`           | Changes status to accepted.                                                                                                                 |
| `Generate shopping list` | Starts shopping list generation after acceptance.                                                                                             |

### Empty / error states

| State                   | UI                                                                                   |
| ----------------------- | ------------------------------------------------------------------------------------ |
| Plan has zero meals     | “No meals planned yet.” Buttons: `Edit inputs`, `Generate again`, `Explore recipes`. |
| Optional dinners absent | “No dinners planned. Lunch is the focus next week.”                                  |
| Reuse notes empty       | Show “No major ingredient reuse identified.” Button: `Improve reuse`.                |
| Meal generation partial | Show successful meals and empty failed slots with `Fill this slot`.                  |
| Revision error          | Keep original meal and show inline retry.                                            |

### Mobile vs desktop

Desktop weekly overview can use columns by day. Mobile uses stacked meal cards. Meal card details collapse behind `Details`.

### ASCII mockup

```text
+-------------------------------------------------------------+
| Week of Mon YYYY-MM-DD                         Status: Draft |
+-------------------------------------------------------------+
| Summary                                                     |
| 5 lunches • 0 dinners • high-protein practical • moderate    |
| Reused ingredients: peppers, rice, yogurt sauce, chicken     |
| Fallback: tuna pasta / eggs + bread + salad                  |
|                                                             |
| [Regenerate whole plan]                   [Accept plan]    |
+-------------------------------------------------------------+
| Monday lunch                                                |
| Chicken rice bowl                                           |
| Why: high protein, good leftovers, uses peppers              |
| Effort: medium   Protein: high   Portion: filling            |
| Reuse: rice, peppers, yogurt sauce                           |
| [Swap] [Make easier] [Higher protein] [Remove ingredient]   |
+-------------------------------------------------------------+
| Tuesday lunch                                               |
| Tuna pasta salad                                            |
| Why: pantry-friendly, quick, good cold                       |
| Effort: easy     Protein: medium-high                       |
| [Swap] [Make cheaper] [Make easier]                         |
+-------------------------------------------------------------+
| Wednesday lunch                                             |
| Leftover chicken bowl / flexible                            |
| Why: avoids overplanning                                    |
| Effort: very easy                                           |
| [Swap] [Use fallback]                                       |
+-------------------------------------------------------------+
| Optional dinners                                            |
| No dinners planned. Lunch is the focus next week.            |
| [Add dinner]                                                |
+-------------------------------------------------------------+
| Assumptions                                                 |
| - Assumed olive oil, salt, pepper, paprika are at home       |
| - Not sure about Cassandra and lentils, so avoided them      |
+-------------------------------------------------------------+

Accepted state:
+-------------------------------------------------------------+
| Week of Mon YYYY-MM-DD                      Status: Accepted |
| [Open shopping list] or [Generate shopping list]             |
+-------------------------------------------------------------+
```

---

## Screen 6 — Meal revision drawer

**Path:** Overlay on `/plans/{planId}`
**Role:** Household Member
**Purpose:** Revise one meal or a constrained part of the plan.

### Components visible

| Component                       |       Visible? | Notes                                                    |
| ------------------------------- | -------------: | -------------------------------------------------------- |
| Selected meal summary           |            Yes | Shows current meal being revised.                        |
| Quick revision buttons          |            Yes | Easier, cheaper, higher protein, remove ingredient, etc. |
| Free-text instruction           |            Yes | Optional.                                                |
| Backlog recipe suggestions      |    Conditional | Appears if relevant.                                     |
| Original vs proposed comparison | After revision | Shows what changed.                                      |
| Keep/undo controls              | After revision | Lets household accept or revert.                         |

### Per-button behavior

| Button              | Behavior                                    |
| ------------------- | ------------------------------------------- |
| `Revise this meal`  | Starts blocking revision for selected card. |
| `Use fallback meal` | Replaces selected slot with fallback.       |
| `Explore recipes`   | Opens explorer with return context.         |
| `Keep change`       | Applies proposed revision.                  |
| `Undo`              | Restores previous meal.                     |
| `Cancel`            | Closes drawer without change.               |

### Empty / error states

| State                                     | UI                                             |
| ----------------------------------------- | ---------------------------------------------- |
| No suitable replacement                   | “No good swap found from the trusted catalog.” |
| Backlog has no matching recipe            | “No backlog recipes fit this slot.”            |
| Revision timeout/failure                  | Original meal remains. `Try again` visible.    |
| Ingredient removal affects multiple meals | Warning with affected meal list.               |

### Mobile vs desktop

Desktop uses right-side drawer. Mobile uses full-screen sheet.

### ASCII mockup

```text
+-------------------------------------------------------------+
| Revise Monday lunch                                      X  |
+-------------------------------------------------------------+
| Current meal                                                |
| Chicken rice bowl                                           |
| Effort: medium • Protein: high • Uses peppers, rice          |
|                                                             |
| What should change?                                         |
| [Make easier] [Make cheaper] [Higher protein]               |
| [Remove ingredient] [Use fallback meal]                     |
|                                                             |
| Optional instruction                                        |
| [ e.g. no peppers, less cooking, Cassandra-friendly ____ ]   |
|                                                             |
| Backlog ideas                                               |
| - Turkey wrap box       [Consider]                          |
| - Greek pasta salad     [Consider]                          |
|                                                             |
| [Cancel]                              [Revise this meal]    |
+-------------------------------------------------------------+

No swap:
+-------------------------------------------------------------+
| No good swap found from the trusted catalog.                 |
| Try one of these:                                            |
| [Use fallback meal] [Explore recipes] [Relax constraints]    |
+-------------------------------------------------------------+
```

---

## Screen 7 — Shopping list

**Path:** `/plans/{planId}/shopping-list`
**Role:** Household Member
**Purpose:** Provide an editable, checkable, grouped shopping list after plan acceptance.

### Components visible

| Component                      |    Visible? | Notes                                                                            |
| ------------------------------ | ----------: | -------------------------------------------------------------------------------- |
| Plan link/header               |         Yes | Shows week and source plan.                                                      |
| Store sections                 |         Yes | Produce, protein, dairy, pantry, frozen, bakery, spices/sauces, household/other. |
| Item checkboxes                |         Yes | Checkable while shopping.                                                        |
| Quantity and flexibility notes |         Yes | “3 bell peppers, any color.”                                                     |
| Pantry assumptions             |         Yes | Explicit assumed-at-home items.                                                  |
| Add item input                 |         Yes | Household can add non-plan items.                                                |
| Edit/delete item controls      |         Yes | For manual corrections.                                                          |
| Regenerate banner              | Conditional | Appears if plan changed after list generation.                                   |

### Per-button behavior

| Button               | Behavior                                          |
| -------------------- | ------------------------------------------------- |
| Checkbox             | Optimistically marks item checked/unchecked.      |
| `Add item`           | Adds item to selected or default section.         |
| `Edit`               | Turns item row into editable state.               |
| `Delete`             | Removes item with undo.                           |
| `Regenerate list`    | Rebuilds list after confirmation if plan changed. |
| `Back to plan`       | Opens `/plans/{planId}`.                          |
| `Mark shopping done` | Optional completion state; does not close access. |

### Empty / error states

| State                       | UI                                                                                         |
| --------------------------- | ------------------------------------------------------------------------------------------ |
| No shopping list yet        | “Shopping list is generated after plan acceptance.” Button: `Open plan`.                     |
| Empty section               | Hide by default; optional compact text: “No frozen items needed.”                          |
| List generated but no items | “No items needed based on this plan and pantry assumptions.” Button: `Review assumptions`. |
| Save checkbox fails         | Revert checkbox and show inline message.                                                   |
| Plan changed                | Banner: “Plan changed. Shopping list may be outdated.”                                     |

### Mobile vs desktop

Mobile prioritizes checkboxes and section accordions. Item notes are secondary text.

### ASCII mockup

```text
+-------------------------------------------------------------+
| Shopping List — Week of Mon YYYY-MM-DD                      |
| Source: Accepted plan                         [Back to plan]|
+-------------------------------------------------------------+
| Produce                                                     |
| [ ] 3 bell peppers, any color                               |
| [ ] 1 cucumber                                              |
| [ ] salad greens                                            |
|                                                             |
| Protein                                                     |
| [ ] chicken breast or thighs, ~800g                         |
| [ ] canned tuna, 2 cans                                     |
|                                                             |
| Dairy                                                       |
| [ ] Greek yogurt / quark                                    |
|                                                             |
| Pantry                                                      |
| [ ] rice                                                    |
| [ ] pasta                                                   |
|                                                             |
| Add item                                                    |
| [ item name __________________ ] [section v] [Add item]     |
+-------------------------------------------------------------+
| Assumed at home                                             |
| - olive oil                                                 |
| - salt                                                      |
| - pepper                                                    |
| - paprika powder                                            |
| [Edit pantry assumptions]                                   |
+-------------------------------------------------------------+
| [Mark shopping done]                                        |
+-------------------------------------------------------------+

Outdated:
+-------------------------------------------------------------+
| Plan changed after this list was generated.                  |
| [Regenerate list] [Keep current list]                        |
+-------------------------------------------------------------+
```

---

## Screen 8 — Weekly feedback

**Path:** `/plans/{planId}/feedback`
**Role:** Household Member
**Purpose:** Capture lightweight feedback after a week.

### Components visible

| Component          |           Visible? | Notes                                    |
| ------------------ | -----------------: | ---------------------------------------- |
| Meal feedback list | Yes if plan exists | Repeat/avoid chips per meal.             |
| Effort question    |                Yes | Too much / okay / easy.                  |
| Unused ingredients |                Yes | Free-text or chips.                      |
| Next-week priority |                Yes | Easier, healthier, cheaper, more varied. |
| Optional notes     |                Yes | Flexible input.                          |
| Save button        |                Yes | Saves feedback.                          |

### Per-button behavior

| Button             | Behavior                                        |
| ------------------ | ----------------------------------------------- |
| `Repeat`           | Marks meal as repeat candidate.                 |
| `Avoid`            | Marks meal as avoid.                            |
| `Worked for lunch` | Marks lunch suitability positive.               |
| `Bad as leftovers` | Marks leftover suitability negative.            |
| `Save feedback`    | Saves answers and returns to dashboard summary. |
| `Skip this week`   | Records no feedback and returns to dashboard.   |
| `Edit feedback`    | Reopens submitted feedback.                     |

### Empty / error states

| State             | UI                                                           |
| ----------------- | ------------------------------------------------------------ |
| No accepted plan  | “No completed plan to review yet.” Button: `Plan next week`. |
| No meals in plan  | “This plan has no meals to review.”                          |
| Save error        | Keep selections and show retry.                              |
| Already submitted | Show saved summary and `Edit feedback`.                      |

### Mobile vs desktop

Mobile uses one question per card. Meal rows become compact selectable cards.

### ASCII mockup

```text
+-------------------------------------------------------------+
| Quick Feedback — Week of Mon YYYY-MM-DD                     |
+-------------------------------------------------------------+
| Which meals should we repeat or avoid?                      |
|                                                             |
| Chicken rice bowl                                           |
| [Repeat] [Avoid] [Worked for lunch] [Bad as leftovers]      |
|                                                             |
| Tuna pasta salad                                            |
| [Repeat] [Avoid] [Worked for lunch] [Bad as leftovers]      |
|                                                             |
| Was cooking too much effort?                                |
| [Too much] [About right] [Easy]                             |
|                                                             |
| Did anything go unused?                                     |
| [ _________________________________________________ ]        |
|                                                             |
| Next week should be...                                      |
| [Easier] [Healthier] [Cheaper] [More varied]                |
|                                                             |
| Optional notes                                              |
| [ _________________________________________________ ]        |
|                                                             |
| [Skip this week]                         [Save feedback]    |
+-------------------------------------------------------------+

Submitted:
+-------------------------------------------------------------+
| Feedback saved.                                             |
| Repeat: Chicken rice bowl                                   |
| Avoid: none                                                 |
| Next week bias: easier                                      |
| [Edit feedback] [Plan next week]                            |
+-------------------------------------------------------------+
```

---

## Screen 9 — Recipe backlog

**Path:** `/recipes/backlog`
**Role:** Household Member
**Purpose:** Show recipes saved for future planning and allow integration into a weekly plan.

### Components visible

| Component            |             Visible? | Notes                                                      |
| -------------------- | -------------------: | ---------------------------------------------------------- |
| Backlog list/grid    |                  Yes | Recipes saved from explorer or manual entry.               |
| Filter chips         |                  Yes | Lunch, dinner, high-protein, easy, comfort, repeat, avoid. |
| Planning-mode banner |          Conditional | Appears when opened from weekly planning.                  |
| Recipe cards         |                  Yes | Title, fit labels, effort, lunch suitability.              |
| Empty state          |    Yes if no recipes | Forward action to explorer.                                |
| Explore button       |                  Yes | Always available.                                          |
| Add recipe text      |                  Yes | Lets the household paste a known recipe and save it in normalized form. |

### Per-button behavior

| Button                | Behavior                                                                |
| --------------------- | ----------------------------------------------------------------------- |
| `Explore recipes`     | Opens `/recipes/explore?source=backlog`.                                |
| `Open`                | Opens recipe detail.                                                    |
| `Use next week`       | In planning mode, marks recipe as candidate for weekly plan.            |
| `Add recipe text`     | Opens a paste/import form and saves a normalized recipe with ingredients and steps. |
| `Remove from backlog` | Removes with undo.                                                      |
| `Mark repeat`         | Tags as repeat candidate.                                               |
| `Mark avoid`          | Tags as avoid; remains visible under avoid filter or hidden by default. |

### Empty / error states

| State               | UI                                                                         |
| ------------------- | -------------------------------------------------------------------------- |
| Backlog empty       | “No recipes saved yet. Explore a few ideas now so planning day is easier.” |
| Filter returns none | “No recipes match these filters.” Button: `Clear filters`.                 |
| Load error          | “Couldn’t load recipe backlog.” Buttons: `Retry`, `Explore recipes`.       |

### Mobile vs desktop

Desktop can use grid cards. Mobile uses vertical cards with short labels.

### ASCII mockup

```text
+-------------------------------------------------------------+
| Recipe Backlog                                              |
+-------------------------------------------------------------+
| Saved recipes to try later                                  |
| [Lunch] [Dinner] [Easy] [High-protein] [Comfort] [Repeat]   |
|                                                             |
| No recipes saved yet.                                       |
| Save a few ideas now so planning day is easier.              |
|                                                             |
| [Explore recipes] [Add recipe manually]                     |
+-------------------------------------------------------------+

With recipes:
+-------------------------------------------------------------+
| [ ] Turkey wrap box                                         |
| Lunch • easy • high-protein • good cold                     |
| [Open] [Use next week] [Remove]                             |
+-------------------------------------------------------------+
| [ ] Greek pasta salad                                       |
| Lunch • easy • good for leftovers                           |
| [Open] [Use next week] [Remove]                             |
+-------------------------------------------------------------+
```

---

## Screen 10 — Recipe Explorer

**Path:** `/recipes/explore`
**Role:** Household Member
**Purpose:** Let the household discover recipes anytime and build a backlog of meals to try.

### Components visible

| Component                   | Visible? | Notes                                                                          |
| --------------------------- | -------: | ------------------------------------------------------------------------------ |
| Recipe suggestion card/list |      Yes | One recipe at a time or compact card feed. Exact interaction style still open. |
| Fit explanation             |      Yes | Why this might work for the household.                                         |
| Tags                        |      Yes | Lunch, dinner, effort, protein, leftover suitability.                          |
| Save/skip controls          |      Yes | Builds backlog quickly.                                                        |
| Details button              |      Yes | Shows ingredients and notes.                                                   |
| Return button               |      Yes | Goes back to source context.                                                   |

### Per-button behavior

| Button                | Behavior                                                          |
| --------------------- | ----------------------------------------------------------------- |
| `Save to backlog`     | Adds recipe to backlog; optimistic success toast.                 |
| `Maybe later`         | Keeps available but does not prioritize.                          |
| `Not interested`      | Hides from future suggestions unless reset.                       |
| `Open details`        | Opens recipe detail view.                                         |
| `Use in current plan` | Conditional when explorer opened from plan context.               |
| `Back`                | Returns to backlog/planning/plan/dashboard depending on `source`. |

### Empty / error states

| State             | UI                                                                             |
| ----------------- | ------------------------------------------------------------------------------ |
| No suggestions    | “No recipe suggestions right now.” Buttons: `Add manually`, `Back to backlog`. |
| Recipe load error | “Couldn’t load recipe ideas.” Button: `Try again`.                             |
| Save fails        | Keep card visible and show retry.                                              |

### Mobile vs desktop

Mobile can feel swipe-like, but the locked UX requirement is quick card decisions. A true swipe gesture is optional; visible buttons are required for accessibility and testability.

### ASCII mockup

```text
+-------------------------------------------------------------+
| Explore Recipes                                             |
+-------------------------------------------------------------+
| Recipe idea                                                 |
|                                                             |
| Turkey wrap lunch boxes                                     |
| Easy • high-protein • good cold • lunch-friendly             |
|                                                             |
| Why it might fit:                                           |
| Uses simple ingredients, packs well, and can be adjusted     |
| with sauce/toppings separately.                             |
|                                                             |
| Ingredients preview                                         |
| turkey, wraps, salad, yogurt sauce, cucumber                 |
|                                                             |
| [Not interested] [Maybe later] [Save to backlog]            |
| [Open details]                                              |
+-------------------------------------------------------------+

Planning context:
+-------------------------------------------------------------+
| This could fit the planned week.                             |
| [Use in current plan] [Save to backlog]                     |
+-------------------------------------------------------------+

No suggestions:
+-------------------------------------------------------------+
| No recipe suggestions right now.                             |
| [Add recipe manually] [Back to backlog]                     |
+-------------------------------------------------------------+
```

---

## Screen 11 — Recipe detail

**Path:** `/recipes/{recipeId}`
**Role:** Household Member
**Purpose:** Show enough recipe information to decide whether to save, use, repeat, or avoid a recipe.

### Components visible

| Component          | Visible? | Notes                                       |
| ------------------ | -------: | ------------------------------------------- |
| Recipe title       |      Yes | Clear name.                                 |
| Suitability labels |      Yes | Lunch/dinner, leftover, protein, effort.    |
| Ingredients        |      Yes | Approximate quantities where available.     |
| Prep/cooking notes |      Yes | Concise, not recipe-blog style.             |
| Household notes    |      Yes | Thomas/Cassandra preference notes if known. |
| Action buttons     |      Yes | Save, use next week, mark avoid, back.      |

### Per-button behavior

| Button            | Behavior                                       |
| ----------------- | ---------------------------------------------- |
| `Save to backlog` | Saves recipe.                                  |
| `Use next week`   | Adds as candidate in current planning context. |
| `Mark repeat`     | Tags recipe positively.                        |
| `Mark avoid`      | Tags recipe negatively.                        |
| `Back`            | Returns to explorer/backlog/plan source.       |

### Empty / error states

| State               | UI                                                                       |
| ------------------- | ------------------------------------------------------------------------ |
| Recipe missing      | “Recipe not found.” Buttons: `Back to backlog`, `Explore recipes`.       |
| Ingredients missing | Show “Ingredients not fully specified yet.” Button: `Edit recipe notes`. |

### Mobile vs desktop

Mobile places sticky action bar at bottom.

### ASCII mockup

```text
+-------------------------------------------------------------+
| Turkey wrap lunch boxes                                     |
+-------------------------------------------------------------+
| Labels: lunch • easy • high-protein • good cold              |
| Effort: easy   Time: ~20 min                                |
|                                                             |
| Why it fits                                                 |
| Practical for workday lunches and adjustable toppings.       |
|                                                             |
| Ingredients                                                 |
| - wraps                                                     |
| - turkey or chicken                                         |
| - salad greens                                              |
| - cucumber                                                  |
| - yogurt sauce                                              |
|                                                             |
| Household notes                                             |
| Keep sauce separate. Add spice only on the side.             |
|                                                             |
| [Save to backlog] [Use next week] [Mark avoid]              |
+-------------------------------------------------------------+
```

---

## State transitions across roles

Because the product has one shared UI role, every transition is perceived by the **Household Member**. Thomas and Cassandra do not receive different role-specific views.

UX behavior should not depend on a specific real-time transport. Intended behavior:

* Active screen changes update immediately after the user action completes.
* Long-running agent actions use blocking loading states.
* Quick local edits, such as shopping checkbox changes or saving a recipe to backlog, can be optimistic with undo/retry.
* If the same household is open in another tab/device, the stale screen should show an “Updated elsewhere” banner on refresh, polling, or focus return. Exact transport belongs to tech.

| State change                        | Trigger                         | Before                             | During                                               | After                                    | Other open sessions                                              |
| ----------------------------------- | ------------------------------- | ---------------------------------- | ---------------------------------------------------- | ---------------------------------------- | ---------------------------------------------------------------- |
| Profile incomplete → basics saved   | `Save basics`                   | Dashboard blocks planning or warns | Button loading                                       | Dashboard shows `Plan next week`         | Show updated profile state on refresh/focus.                     |
| No plan → draft generating          | `Generate draft plan`           | Weekly input form                  | Blocking loading: “Building a realistic lunch plan…” | Draft plan page                          | Dashboard shows draft after refresh/focus.                       |
| Draft generating → generation error | Generation failure              | Loading state                      | —                                                    | Error card with preserved inputs         | Other sessions see no new draft.                                 |
| Draft → revised meal pending        | `Revise this meal`              | Meal card visible                  | Selected card/drawer loading                         | Proposed replacement shown               | Other sessions may show update banner.                           |
| Proposed revision → kept            | `Keep change`                   | Proposed replacement visible       | Quick save loading                                   | Meal card updated                        | Other sessions show updated draft after refresh/focus.           |
| Draft → accepted                    | `Accept plan`                  | Draft controls visible             | Button loading                                       | Status: Accepted; shopping CTA visible   | Other sessions show accepted state after refresh/focus.          |
| Accepted → shopping list generating | `Generate shopping list`        | Accepted plan                      | Blocking loading                                     | Shopping list page                       | Other sessions show shopping list available after refresh/focus. |
| Shopping unchecked → checked        | Checkbox click                  | Item unchecked                     | Optimistic check                                     | Item checked/muted                       | Other sessions update on refresh/focus.                          |
| Recipe suggestion → backlog saved   | `Save to backlog`               | Recipe card visible                | Optimistic toast                                     | Recipe appears in backlog                | Other sessions update on refresh/focus.                          |
| Feedback empty → submitted          | `Save feedback`                 | Feedback form                      | Button loading                                       | Feedback summary                         | Dashboard shows next-week prompt after refresh/focus.            |
| Plan changed after shopping list    | Meal revision after list exists | Shopping list exists               | Revision loading                                     | Banner: “Shopping list may be outdated.” | Other sessions see stale/update banner.                          |

---

## Terminal states and dead ends

Every end state must have a forward action.

| Terminal/dead-end state              | Required forward action                                                                       |
| ------------------------------------ | --------------------------------------------------------------------------------------------- |
| No profile basics                    | `Set up basics` or explicit `Use defaults and plan` after required hard constraints answered. |
| No active plan                       | `Plan next week`.                                                                             |
| Draft plan generated but disliked    | `Revise plan`, `Swap meal`, `Regenerate whole plan`, `Explore recipes`.                       |
| No suitable meal swap                | `Use fallback meal`, `Explore recipes`, `Relax constraints`.                                  |
| Empty recipe backlog                 | `Explore recipes`, `Add recipe manually`.                                                     |
| Explorer has no suggestions          | `Add recipe manually`, `Back to backlog`.                                                     |
| Accepted plan with no shopping list  | `Generate shopping list`.                                                                     |
| Shopping list generated              | `Back to plan`, `Mark shopping done`, `Give feedback`, `Plan another week`.                   |
| Feedback submitted                   | `Plan next week`, `Edit feedback`, `Explore recipes`.                                         |
| Feedback unavailable because no plan | `Plan next week`.                                                                             |
| Plan generation error                | `Try again`, `Simplify next week`, `Edit inputs`.                                             |
| Shopping list outdated               | `Regenerate list`, `Keep current list`.                                                       |

---

## Product invariants

### I-1 visibility — per-URL answer

UX visibility only. This does not define permission enforcement.

| URL / screen                                               | Who sees the GET in the UI? | Who can trigger the main POST/action from the UI? | Main visible action                         |
| ---------------------------------------------------------- | --------------------------- | ------------------------------------------------- | ------------------------------------------- |
| `/`                                                        | Household Member            | Household Member                                  | Navigate to next action.                    |
| `/profile`                                                 | Household Member            | Household Member                                  | Save household profile.                     |
| `/profile?mode=basics`                                     | Household Member            | Household Member                                  | Save required basics.                       |
| `/plan/new?weekStart=YYYY-MM-DD`                           | Household Member            | Household Member                                  | Generate draft plan.                        |
| `/plans/{planId}`                                          | Household Member            | Household Member                                  | Revise, accept, regenerate, open shopping. |
| `/plans/{planId}?focus=meal:{mealSlotId}`                  | Household Member            | Household Member                                  | Revise focused meal.                        |
| `/plans/{planId}/shopping-list`                            | Household Member            | Household Member                                  | Generate/edit/check shopping list.          |
| `/shopping/current`                                        | Household Member            | Household Member                                  | Continue active shopping list.              |
| `/plans/{planId}/feedback`                                 | Household Member            | Household Member                                  | Submit/edit feedback.                       |
| `/feedback/current`                                        | Household Member            | Household Member                                  | Submit/edit active feedback.                |
| `/recipes/backlog`                                         | Household Member            | Household Member                                  | Save/remove/mark/use recipes.               |
| `/recipes/backlog?mode=selectForPlan&weekStart=YYYY-MM-DD` | Household Member            | Household Member                                  | Select backlog recipes for planning.        |
| `/recipes/explore`                                         | Household Member            | Household Member                                  | Save/skip/use recipe suggestions.           |
| `/recipes/{recipeId}`                                      | Household Member            | Household Member                                  | Save/use/mark recipe.                       |

### I-2 role+ID model — per-action answer

UX intent only. Server-side derivation and authorization are tech scope.

| UI action                | IDs the Household Member implicitly acts on                                | UX intent                                                         |
| ------------------------ | -------------------------------------------------------------------------- | ----------------------------------------------------------------- |
| Save profile             | Household profile ID; person preference labels Thomas/Cassandra            | Update stable household defaults and individual preference notes. |
| Start weekly plan        | Household ID; `weekStart`                                                  | Create a plan for this household/week.                            |
| Generate draft plan      | Household ID; `weekStart`; weekly input state; selected backlog recipe IDs | Ask agent for one concrete draft plan.                            |
| Revise meal              | `planId`; `mealSlotId`; optional recipe/ingredient constraint              | Change only selected meal unless full regeneration requested.     |
| Regenerate whole plan    | `planId`; weekly input state                                               | Replace draft proposal.                                           |
| Accept plan             | `planId`                                                                   | Mark this weekly plan as accepted by the household.               |
| Generate shopping list   | `planId`                                                                   | Create grouped list from accepted plan.                           |
| Check shopping item      | `shoppingListId`; `shoppingItemId`                                         | Mark item as bought/done.                                         |
| Add shopping item        | `shoppingListId`; section label                                            | Add household/manual item to list.                                |
| Edit pantry assumptions  | Household profile ID; pantry item IDs                                      | Update assumed-at-home items.                                     |
| Save recipe to backlog   | `recipeId`; household recipe backlog ID                                    | Store recipe as something to consider later.                      |
| Use recipe next week     | `recipeId`; `weekStart` or `planId`; optional `mealSlotId`                 | Make recipe a candidate for planned weekly plan or selected slot. |
| Mark recipe repeat/avoid | `recipeId`; household feedback/preference context                          | Influence future planning.                                        |
| Submit feedback          | `planId`; meal IDs/slots; feedback selections                              | Teach future plans what worked.                                   |

### I-7 empty state — per-aggregate answer

| Aggregate UI                 | Empty state                                                                                  | Shorter than expected                                            | Ties                                                                     |
| ---------------------------- | -------------------------------------------------------------------------------------------- | ---------------------------------------------------------------- | ------------------------------------------------------------------------ |
| Dashboard active plan        | “No plan for next week yet.” Button: `Plan next week`.                                       | If draft incomplete, show partial draft and `Continue planning`. | Not applicable.                                                          |
| Meal plan lunch list         | “No lunches planned yet.” Buttons: `Edit inputs`, `Generate again`.                          | Show available meals and empty slots with `Fill this slot`.      | Not applicable.                                                          |
| Optional dinners             | “No dinners planned. Lunch is the focus next week.” Button: `Add dinner`.                    | Show planned dinners only; do not imply error.                   | Not applicable.                                                          |
| Flexible days                | “No flexible days selected.”                                                                 | If fewer flexible days than expected, show actual count.         | Not applicable.                                                          |
| Ingredient reuse notes       | “No major ingredient reuse identified.” Button: `Improve reuse`.                             | Show the reuse that exists; do not invent reuse.                 | If multiple ingredients reused equally, list all or alphabetize.         |
| Prep-ahead notes             | “No prep-ahead steps needed.”                                                                | Show only relevant prep.                                         | Not applicable.                                                          |
| Fallback meals               | Fallback is required; if missing, show error: “Add fallback before acceptance.”                | If only one fallback exists, show one.                           | If multiple equal fallback candidates, show top 2–3.                     |
| Recipe backlog               | “No recipes saved yet. Explore a few ideas now so planning day is easier.”                   | Show saved recipes count and `Explore more`.                     | If same fit score, sort by recently saved.                               |
| Backlog candidates for week  | “No backlog recipes look ideal for next week.” Buttons: `Ignore backlog`, `Explore recipes`. | Show fewer candidates and explain why.                           | If tied, prefer easier/lunch-suitable recipes.                           |
| Recipe explorer suggestions  | “No recipe suggestions right now.” Buttons: `Add manually`, `Back to backlog`.               | Show available suggestions only.                                 | If tied, prefer trusted/easy/lunch-compatible.                           |
| Shopping list sections       | Hide empty sections or show compact “No dairy needed.”                                       | Show only generated items; allow manual add.                     | If item belongs to multiple sections, use most shopper-friendly section. |
| Pantry assumptions           | “No pantry assumptions listed.” Button: `Edit pantry assumptions`.                           | Show known assumptions only.                                     | Not applicable.                                                          |
| Feedback meal list           | “No meals to review.” Button: `Back to plan`.                                                | Show meals that exist; allow partial feedback.                   | If multiple meals equally liked, allow multiple repeats.                 |
| Plan history, if shown later | “No previous plans yet.” Button: `Plan next week`.                                           | Show available history.                                          | Sort ties by most recent.                                                |

---

## UX-specific product rules

### Acceptance and shopping

* Draft plans can be revised freely.
* The shopping list is generated only after the plan is accepted.
* If the plan changes after shopping list generation, the shopping list shows an outdated warning.
* The user must always have a forward action from a blocked state.

### Recipe exploration

* Recipe Explorer is always accessible from nav.
* Empty backlog must push toward exploration, not a dead end.
* Weekly planning can suggest backlog recipes that fit the planned week.
* Default novelty limit: no more than one experimental/new recipe per week unless the household asks for more.

### Health tone

* Health labels must be practical and approximate.
* Good copy: “Protein-focused but still realistic.”
* Bad copy: “You failed your calorie target.”
* No success/failure scoring.
* No exact calorie compliance UI.

### Planning style

* The agent should propose a concrete plan.
* It should not dump a large option list.
* It should explain assumptions briefly.
* It should preserve good parts during revision.
* It should use modular compromises when Thomas/Cassandra preferences differ.

---

## Playwright scenarios

* Household opens dashboard with no profile basics → completes basics → returns to dashboard → sees `Plan next week`.
* Household starts weekly planning → accepts default 5 lunches and no dinners → generates draft plan.
* Household starts weekly planning with empty recipe backlog → sees empty backlog state → continues generating from trusted catalog.
* Household explores recipes from nav → saves recipe to backlog → sees recipe in backlog.
* Household opens planning form → selects one backlog recipe candidate → generates draft plan that considers it.
* Household reviews draft plan → swaps Monday lunch → keeps revised meal → other meals remain unchanged.
* Household removes an ingredient from one meal → affected meals warning appears → household cancels.
* Household rejects all swap suggestions → uses fallback meal.
* Household accepts draft plan → shopping-list CTA appears.
* Household tries to open shopping list before acceptance → sees “generated after plan acceptance” empty state.
* Household generates shopping list from accepted plan → sees grouped sections and pantry assumptions.
* Household checks shopping items → items become checked/muted and remain in list.
* Household edits accepted plan after shopping list generation → shopping list shows outdated banner.
* Household completes weekly feedback → marks one meal repeat, one meal avoid, and next week easier → dashboard shows next planning action.
* Household opens feedback with no accepted plan → sees empty state and `Plan next week`.
* Household opens app on mobile → weekly plan renders as stacked meal cards with sticky primary action.
* Household has two tabs open → revises plan in one tab → other tab shows updated/stale banner on focus or refresh.

---

## User stories

### Navigation and entry-point stories

* **US-1**: As a household member, I can open the dashboard and see the next best action so that I do not need to remember where I left off.
* **US-2**: As a household member, I can start weekly planning from the dashboard so that planning has one obvious entry point.
* **US-3**: As a household member, I can open Recipe Explorer from global navigation so that recipe discovery is always available.
* **US-4**: As a household member, I can open the current shopping list from navigation so that I can use it while shopping without finding the plan first.
* **US-5**: As a household member, I can return from Recipe Explorer to the place I came from so that exploration does not interrupt planning.

### Profile stories

* **US-6**: As a household member, I can enter allergies, intolerances, and hard dislikes before the first plan so that unsafe or strongly disliked meals are avoided.
* **US-7**: As a household member, I can maintain separate Thomas and Cassandra preference sections so that the household plan reflects both people without separate UI roles.
* **US-8**: As a household member, I can save pantry staples so that shopping lists clearly distinguish what to buy from what is assumed at home.
* **US-9**: As a household member, I can leave optional profile fields blank so that setup does not become a long onboarding process.
* **US-10**: As a household member, I can update health priorities in supportive language so that the app helps with weight-management goals without feeling judgmental.

### Weekly planning stories

* **US-11**: As a household member, I can choose which week to plan so that plans are anchored to the correct dates.
* **US-12**: As a household member, I can accept a default of 5 weekday lunches so that normal weekly planning is fast.
* **US-13**: As a household member, I can add optional dinners so that dinner planning supports lunch leftovers when needed.
* **US-14**: As a household member, I can mark special days with no cooking or unusual constraints so that the plan stays realistic.
* **US-15**: As a household member, I can list ingredients already at home so that the plan reduces waste.
* **US-16**: As a household member, I can choose weekly priorities like easy, cheap, high-protein, lighter, comforting, varied, use existing ingredients, or meal-prep friendly so that the agent understands next week?s bias.
* **US-17**: As a household member, I can add free-text weekly context so that unusual constraints are not forced into rigid form fields.
* **US-18**: As a household member, I can generate one concrete draft plan instead of browsing many options so that decision fatigue is reduced.
* **US-19**: As a household member, I can see a loading state during generation so that I know the app is working.

### Recipe backlog and exploration stories

* **US-20**: As a household member, I can explore recipes at any time so that I can build a backlog before planning.
* **US-21**: As a household member, I can save a recipe to the backlog so that it can be considered in future weekly plans.
* **US-22**: As a household member, I can mark a recipe as not interested so that poor suggestions stop appearing.
* **US-23**: As a household member, I can view an empty recipe backlog state with an `Explore recipes` action so that an empty backlog is not a dead end.
* **US-24**: As a household member, I can select backlog recipes during weekly planning so that saved ideas can be integrated into the planned week.
* **US-25**: As a household member, I can open recipe details before saving or using a recipe so that I can check ingredients, effort, and lunch suitability.
* **US-26**: As a household member, I can add or save only one experimental recipe by default for a week so that novelty does not make the plan risky.

### Plan review and revision stories

* **US-27**: As a household member, I can review a weekly plan at a glance so that I understand meals, effort, reuse, fallback, and assumptions quickly.
* **US-28**: As a household member, I can see why each meal was chosen so that I trust the agent’s proposal.
* **US-29**: As a household member, I can see effort and rough protein labels on meal cards so that lunch practicality and health fit are visible.
* **US-30**: As a household member, I can swap one meal so that I do not need to regenerate the whole plan.
* **US-31**: As a household member, I can make one meal easier so that the plan adapts to a stressful week.
* **US-32**: As a household member, I can make one meal cheaper so that the plan avoids unnecessary cost.
* **US-33**: As a household member, I can request higher protein for one meal so that the plan supports health goals without strict tracking.
* **US-34**: As a household member, I can remove one ingredient from a meal so that temporary dislikes or pantry realities are respected.
* **US-35**: As a household member, I can use a fallback meal when no good swap exists so that revision never dead-ends.
* **US-36**: As a household member, I can regenerate the whole plan with confirmation so that I can recover from a bad draft without accidental loss.
* **US-37**: As a household member, I can see a partial-generation error with successful meals preserved so that one failed slot does not discard the whole plan.

### Acceptance and shopping stories

* **US-38**: As a household member, I can accept a draft plan so that the app knows it is stable enough for shopping-list generation.
* **US-39**: As a household member, I cannot generate a final shopping list from an unaccepted draft so that shopping does not reflect unstable meals.
* **US-40**: As a household member, I can generate a grouped shopping list after acceptance so that shopping is practical in the store.
* **US-41**: As a household member, I can see pantry assumptions on the shopping list so that I know what the app thinks is already at home.
* **US-42**: As a household member, I can check off shopping items so that the list works during shopping.
* **US-43**: As a household member, I can add manual shopping items so that household needs outside the meal plan are not lost.
* **US-44**: As a household member, I can edit or delete shopping items so that list mistakes are easy to fix.
* **US-45**: As a household member, I can see an outdated-list banner if the plan changes after shopping list generation so that I know whether to regenerate.

### Feedback stories

* **US-46**: As a household member, I can give quick feedback at the end of the week so that future plans improve.
* **US-47**: As a household member, I can mark meals to repeat so that successful meals come back.
* **US-48**: As a household member, I can mark meals to avoid so that failed meals are less likely to return.
* **US-49**: As a household member, I can say whether cooking effort was too much so that future plans become more realistic.
* **US-50**: As a household member, I can record unused ingredients so that future plans reduce waste.
* **US-51**: As a household member, I can choose whether next week should be easier, healthier, cheaper, or more varied so that feedback directly influences the next planning cycle.
* **US-52**: As a household member, I can skip feedback for a week so that the app remains low-friction.

### Error, empty, and mobile stories

* **US-53**: As a household member, I can retry plan generation after an error without losing my weekly inputs.
* **US-54**: As a household member, I can recover from an empty meal plan by editing inputs, generating again, or exploring recipes.
* **US-55**: As a household member, I can use the app on mobile with stacked cards and visible primary actions so that planning and shopping work away from desktop.
* **US-56**: As a household member, I can see when another tab or device may have changed the plan so that I do not unknowingly edit stale information.

---

## Resolved product decisions

| Decision area | Intended behavior |
| --- | --- |
| Access | Private household link or access code, remembered through session or cookie state. |
| First-time profile setup | Guided by the product when no profile exists; safety basics first, optional details later. |
| Weekly planning interface | Single compact form with free-text context in the web UI. |
| Dashboard | Primary app home. |
| Week default | Plan next week unless the household explicitly selects another week. |
| Shopping trigger | No scheduled shopping date; the household triggers shopping-list generation. |
| Recipe Explorer interaction | Button-based cards; swipe-like interaction is optional decoration only. |
| Recipe text import | Supported through backlog paste/import. |
| Nutrition | Rough protein, portion, balance, and optional calorie notes; no calorie compliance scoring. |
| Cross-tab behavior | Refresh on focus, page load, or manual reload is enough. |
| Plan acceptance | One shared household role can accept a plan. |
| Feedback | The product can ask clarifying questions until the household accepts the summary or skips. |
| Checked shopping items | Checked items remain visible and muted. |
| Experimental recipes | At most one new or experimental recipe per week by default, unless the household asks for more. |
