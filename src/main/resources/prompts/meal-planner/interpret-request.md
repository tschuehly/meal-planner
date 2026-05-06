Return an `InterpretedLunchRequest` for the supported meal-planner workflow.

Extract only facts that are explicit in the household request. Capture allergies, intolerances, dislikes, ingredients to avoid, nutrition priorities, effort hints, requested week text, requested meal count, ingredients to use, and any pasted one-off recipe context.

Use assumptions only for underspecified planning details that matter for this one run. Do not invent saved profile data, shopping-list behavior, recipe backlog behavior, follow-up questions, or durable state.
