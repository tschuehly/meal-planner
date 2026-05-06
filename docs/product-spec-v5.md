# Product Spec v5: Weekly Feedback Memory

## Goal

Add household feedback that improves later plans.

The household can mark meals as repeat, avoid, too much effort, or useful for leftovers. Later planning uses that feedback as household memory.

## Added Product Unit

`Weekly feedback`

Feedback stores meal-level and week-level signals from accepted plans.

## Interface

The browser interface exposes feedback after a planned week:

| Feedback input | Purpose |
| --- | --- |
| Repeat | Bring a meal back in future plans. |
| Avoid | Reduce or block a meal in future plans. |
| Too much effort | Bias later plans toward easier meals. |
| Good leftovers | Prefer similar meals for workday lunches. |
| Unused ingredients | Reduce waste in future planning. |

## Product Behavior

Feedback should influence later plans without turning the product into a scoring system.

The product should:

* repeat meals the household liked
* avoid meals the household rejected
* adjust effort based on week-level feedback
* account for unused ingredients
* explain feedback influence briefly in later plans
* ask follow-up questions when feedback is ambiguous, until the household accepts the feedback summary or skips

## Minimal Persistence

Extend local single-household storage with feedback history.

Store only the signals needed for future planning: plan week, meal title, repeat or avoid status, effort feedback, leftover usefulness, unused ingredients, and submitted date.

## Supported Workflow

1. Complete or review an accepted weekly plan.
2. Submit quick feedback.
3. Clarify feedback if needed.
4. Accept the feedback summary or skip the week.
5. Start a later planning request.
6. Receive a plan that reflects prior feedback where relevant.

## Acceptance Checks

* Feedback can be submitted for an accepted plan.
* Ambiguous feedback can be clarified before saving.
* Later plans can repeat meals marked successful.
* Later plans avoid meals marked as poor fits.
* Effort feedback changes later planning toward easier or more ambitious meals.
* The final plan explains feedback influence without scoring or shaming the household.

## Exclusions

v5 does not include nutrition scoring, calorie compliance, public accounts, automated grocery ordering, or multi-household analytics.
