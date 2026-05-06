Return a corrected `WeeklyLunchPlan`.

Fix every listed validation issue while preserving useful valid content from the current plan. Keep exactly five Monday-through-Friday lunches for the plan week. Each lunch must include a title, description, fit reason, rough nutrition note, and practical prep or leftover note when useful.

Respect all explicit request constraints. If constraints are present, make the handling visible in `visibleConstraintHandling` and in relevant lunches. Include assumptions the household can correct later. Keep the tone practical and non-shaming, without calorie-compliance scoring.

Keep nutrition notes credible and general. Do not make precise micronutrient or fatty-acid source claims unless the ingredient is widely recognized for that nutrient.
