package com.embabel.mealplanner.v0

import com.embabel.agent.api.common.SomeOf
import com.embabel.agent.domain.io.SystemOutput
import com.embabel.agent.domain.library.HasContent
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

data class HouseholdLunchPlanningRequest(
    val content: String,
    val receivedAt: Instant = Instant.now(),
)

enum class HouseholdMember {
    THOMAS,
    CASSANDRA,
}

data class LunchPlanningDefaults(
    val planningUnit: String = "weekday lunches",
    val mealCount: Int = 5,
    val effort: String = "easy to moderate",
    val healthTone: String = "high-protein and practical, with rough nutrition notes and without calorie compliance scoring",
    val householdStyle: String = "meals should work for both Thomas and Cassandra",
    val requiredSafetyBehavior: String = "avoid explicit allergies, intolerances, and strong dislikes from the request",
    val householdMembers: List<HouseholdMember> = listOf(HouseholdMember.THOMAS, HouseholdMember.CASSANDRA),
)

data class LunchSlot(
    val day: DayOfWeek,
    val date: LocalDate,
) {
    val label: String
        get() = day.name.lowercase().replaceFirstChar { it.titlecase() }
}

data class PlanningHorizon(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val slots: List<LunchSlot>,
    val source: String,
)

data class RequestConstraints(
    val explicitConstraints: List<String> = emptyList(),
    val allergiesOrIntolerances: List<String> = emptyList(),
    val avoidIngredients: List<String> = emptyList(),
    val strongDislikes: List<String> = emptyList(),
    val nutritionPriorities: List<String> = emptyList(),
    val effortHints: List<String> = emptyList(),
    val ingredientsToUse: List<String> = emptyList(),
    val requestedWeekText: String? = null,
    val requestedMealCount: Int? = null,
    val assumptions: List<PlanningAssumption> = emptyList(),
)

data class OneOffRecipeContext(
    val title: String? = null,
    val summary: String,
    val usableIdeas: List<String> = emptyList(),
    val constraints: List<String> = emptyList(),
)

data class InterpretedLunchRequest(
    val requestConstraints: RequestConstraints,
    val oneOffRecipeContext: OneOffRecipeContext? = null,
) : SomeOf

data class PlanningAssumption(
    val description: String,
)

data class LunchCandidate(
    val title: String,
    val description: String,
    val fitReason: String,
    val nutritionNote: String,
    val prepOrLeftoverNote: String? = null,
    val constraintHandling: List<String> = emptyList(),
    val mainIngredients: List<String> = emptyList(),
)

data class LunchCandidateSet(
    val candidates: List<LunchCandidate>,
)

data class PlannedLunch(
    val day: DayOfWeek,
    val date: LocalDate,
    val title: String,
    val description: String,
    val whyItFits: String,
    val nutritionNote: String,
    val prepOrLeftoverNote: String? = null,
    val constraintHandling: List<String> = emptyList(),
)

data class WeeklyLunchPlan(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val lunches: List<PlannedLunch>,
    val visibleConstraintHandling: List<String> = emptyList(),
    val assumptions: List<PlanningAssumption> = emptyList(),
)

data class ValidatedWeeklyLunchPlan(
    val plan: WeeklyLunchPlan,
    val validationNotes: List<String>,
)

data class WeeklyLunchPlanResponse(
    override val content: String,
    override val timestamp: Instant = Instant.now(),
) : SystemOutput, HasContent {
    override fun toString(): String = content
}
