package com.embabel.mealplanner.v0

import com.embabel.agent.api.annotation.AchievesGoal
import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.Agent
import com.embabel.agent.api.common.OperationContext
import com.embabel.agent.domain.io.UserInput
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Agent(
    description = "Creates one-shot weekday lunch plans for Thomas and Cassandra from a free-text household request.",
)
class MealPlannerV0Agent(
    private val clock: Clock,
    private val prompts: MealPlannerV0PromptLibrary,
) {

    @Action(
        description = "Convert shell user input into a v0 household lunch-planning request.",
        readOnly = true,
    )
    fun ingestHouseholdLunchPlanningRequest(userInput: UserInput): HouseholdLunchPlanningRequest =
        HouseholdLunchPlanningRequest(
            content = userInput.content.trim(),
            receivedAt = userInput.timestamp,
        )

    @Action(
        description = "Provide the supported v0 lunch-planning defaults.",
        readOnly = true,
    )
    fun provideV0LunchPlanningDefaults(
        request: HouseholdLunchPlanningRequest,
    ): LunchPlanningDefaults {
        require(request.content.isNotBlank()) { "Lunch-planning request must not be blank." }
        return LunchPlanningDefaults()
    }

    @Action(
        description = "Resolve the target planning week and Monday-Friday lunch slots.",
        readOnly = true,
    )
    fun resolvePlanningHorizon(
        request: HouseholdLunchPlanningRequest,
        defaults: LunchPlanningDefaults,
    ): PlanningHorizon {
        val weekStart = resolveRequestedWeekStart(request.content)
        val slots = (0L until defaults.mealCount.toLong()).map { offset ->
            val date = weekStart.plusDays(offset)
            LunchSlot(day = date.dayOfWeek, date = date)
        }
        return PlanningHorizon(
            weekStart = weekStart,
            weekEnd = weekStart.plusDays(defaults.mealCount.toLong() - 1),
            slots = slots,
            source = if (containsWeekSignal(request.content)) {
                "Resolved from the request text."
            } else {
                "Defaulted to next week."
            },
        )
    }

    @Action(
        description = "Extract explicit constraints, preferences, and any one-off recipe context from the request.",
        readOnly = true,
    )
    fun interpretLunchPlanningRequest(
        request: HouseholdLunchPlanningRequest,
        defaults: LunchPlanningDefaults,
        context: OperationContext,
    ): InterpretedLunchRequest =
        context.ai()
            .withDefaultLlm()
            .withId("meal-planner-v0-interpret-request")
            .withSystemPrompt("You extract typed household lunch-planning facts for the supported v0 workflow.")
            .createObject(
                prompt = buildInterpretPrompt(request, defaults),
                outputClass = InterpretedLunchRequest::class.java,
            )

    @Action(
        description = "Draft practical weekday lunch candidates with fit reasons and rough nutrition notes.",
        readOnly = true,
    )
    fun draftLunchCandidates(
        horizon: PlanningHorizon,
        defaults: LunchPlanningDefaults,
        constraints: RequestConstraints,
        oneOffRecipeContext: OneOffRecipeContext?,
        context: OperationContext,
    ): LunchCandidateSet =
        context.ai()
            .withDefaultLlm()
            .withId("meal-planner-v0-draft-candidates")
            .withSystemPrompt("You draft practical lunch candidates for Thomas and Cassandra without using tools.")
            .createObject(
                prompt = buildCandidatePrompt(horizon, defaults, constraints, oneOffRecipeContext),
                outputClass = LunchCandidateSet::class.java,
            )

    @Action(
        description = "Select five candidates and assign them to weekday lunch slots.",
        readOnly = true,
    )
    fun assembleWeeklyLunchPlan(
        horizon: PlanningHorizon,
        defaults: LunchPlanningDefaults,
        constraints: RequestConstraints,
        candidates: LunchCandidateSet,
        context: OperationContext,
    ): WeeklyLunchPlan =
        context.ai()
            .withDefaultLlm()
            .withId("meal-planner-v0-assemble-plan")
            .withSystemPrompt("You assemble a one-shot weekly lunch plan for the supported v0 workflow.")
            .createObject(
                prompt = buildAssemblyPrompt(horizon, defaults, constraints, candidates),
                outputClass = WeeklyLunchPlan::class.java,
            )

    @Action(
        description = "Validate the weekly lunch plan structure and visible constraint handling.",
        readOnly = true,
    )
    fun validateWeeklyLunchPlan(
        plan: WeeklyLunchPlan,
        constraints: RequestConstraints,
        defaults: LunchPlanningDefaults,
    ): ValidatedWeeklyLunchPlan {
        val notes = mutableListOf<String>()
        require(plan.lunches.size == defaults.mealCount) {
            "Expected ${defaults.mealCount} weekday lunches, got ${plan.lunches.size}."
        }

        val expectedDays = DayOfWeek.entries
            .filter { it.value in DayOfWeek.MONDAY.value..DayOfWeek.FRIDAY.value }
            .take(defaults.mealCount)
        val actualDays = plan.lunches.map { it.day }
        require(actualDays == expectedDays) {
            "Expected Monday-Friday lunches in order, got $actualDays."
        }
        notes += "Plan contains ${defaults.mealCount} Monday-Friday lunches."

        plan.lunches.forEach { lunch ->
            require(lunch.title.isNotBlank()) { "Lunch for ${lunch.day} must have a title." }
            require(lunch.description.isNotBlank()) { "Lunch for ${lunch.day} must have a description." }
            require(lunch.whyItFits.isNotBlank()) { "Lunch for ${lunch.day} must explain why it fits." }
            require(lunch.nutritionNote.isNotBlank()) { "Lunch for ${lunch.day} must include a rough nutrition note." }
        }
        notes += "Each lunch includes a fit reason and rough nutrition note."

        if (constraints.hasVisibleConstraints()) {
            require(plan.visibleConstraintHandling.isNotEmpty()) {
                "Requests with explicit constraints must include visible constraint handling."
            }
            notes += "Explicit constraints are surfaced in the plan."
        }

        require(plan.assumptions.isNotEmpty()) {
            "Plan must include assumptions for underspecified v0 planning details."
        }
        notes += "Plan includes assumptions the household can correct later."

        val moralizingTerms = listOf("cheat meal", "guilt", "bad food", "clean eating", "calorie compliance")
        val planText = plan.toString().lowercase()
        require(moralizingTerms.none { it in planText }) {
            "Plan must avoid moralizing or calorie-compliance language."
        }
        notes += "Tone avoids moralizing nutrition language."

        return ValidatedWeeklyLunchPlan(plan = plan, validationNotes = notes)
    }

    @AchievesGoal(
        description = "Return a structured five-lunch weekday plan for Thomas and Cassandra.",
        tags = ["meal-planning", "lunch", "household"],
        examples = [
            "Plan high-protein weekday lunches for next week and avoid mushrooms.",
            "Use this pasted recipe as inspiration for next week's lunches.",
        ],
    )
    @Action(
        description = "Format the validated plan as the command-line response.",
        readOnly = true,
    )
    fun formatWeeklyLunchPlanResponse(
        validatedPlan: ValidatedWeeklyLunchPlan,
        horizon: PlanningHorizon,
        constraints: RequestConstraints,
        context: OperationContext,
    ): WeeklyLunchPlanResponse {
        val content = context.ai()
            .withDefaultLlm()
            .withId("meal-planner-v0-format-response")
            .withSystemPrompt("You write concise, practical command-line meal-planning responses.")
            .createObject(
                prompt = buildResponsePrompt(validatedPlan, horizon, constraints),
                outputClass = String::class.java,
            )
            .trim()
        return WeeklyLunchPlanResponse(content = content)
    }

    private fun resolveRequestedWeekStart(content: String): LocalDate {
        val today = LocalDate.now(clock)
        val currentWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val lower = content.lowercase()
        val explicitDate = ISO_DATE.find(lower)?.value?.let(LocalDate::parse)
        return when {
            explicitDate != null -> explicitDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            "this week" in lower -> currentWeekStart
            "in two weeks" in lower || "two weeks from now" in lower -> currentWeekStart.plusWeeks(2)
            "next week" in lower -> currentWeekStart.plusWeeks(1)
            else -> currentWeekStart.plusWeeks(1)
        }
    }

    private fun containsWeekSignal(content: String): Boolean {
        val lower = content.lowercase()
        return listOf("this week", "next week", "in two weeks", "two weeks from now", "week of").any { it in lower } ||
            ISO_DATE.containsMatchIn(lower)
    }

    private fun buildInterpretPrompt(
        request: HouseholdLunchPlanningRequest,
        defaults: LunchPlanningDefaults,
    ): String = """
        ${prompts.load("interpret-request")}

        # Household Request
        ${request.content}

        # v0 Defaults
        ${defaults.asPromptBlock()}
    """.trimIndent()

    private fun buildCandidatePrompt(
        horizon: PlanningHorizon,
        defaults: LunchPlanningDefaults,
        constraints: RequestConstraints,
        oneOffRecipeContext: OneOffRecipeContext?,
    ): String = """
        ${prompts.load("draft-candidates")}

        # Planning Horizon
        ${horizon.asPromptBlock()}

        # v0 Defaults
        ${defaults.asPromptBlock()}

        # Request Constraints
        ${constraints.asPromptBlock()}

        # One-Off Recipe Context
        ${oneOffRecipeContext?.asPromptBlock() ?: "None provided."}
    """.trimIndent()

    private fun buildAssemblyPrompt(
        horizon: PlanningHorizon,
        defaults: LunchPlanningDefaults,
        constraints: RequestConstraints,
        candidates: LunchCandidateSet,
    ): String = """
        ${prompts.load("assemble-plan")}

        # Planning Horizon
        ${horizon.asPromptBlock()}

        # v0 Defaults
        ${defaults.asPromptBlock()}

        # Request Constraints
        ${constraints.asPromptBlock()}

        # Lunch Candidates
        ${candidates.asPromptBlock()}
    """.trimIndent()

    private fun buildResponsePrompt(
        validatedPlan: ValidatedWeeklyLunchPlan,
        horizon: PlanningHorizon,
        constraints: RequestConstraints,
    ): String = """
        ${prompts.load("format-response")}

        # Planning Horizon
        ${horizon.asPromptBlock()}

        # Request Constraints
        ${constraints.asPromptBlock()}

        # Validated Plan
        ${validatedPlan.asPromptBlock()}
    """.trimIndent()

    private fun LunchPlanningDefaults.asPromptBlock(): String = """
        Planning unit: $planningUnit
        Meal count: $mealCount
        Effort: $effort
        Health tone: $healthTone
        Household style: $householdStyle
        Safety behavior: $requiredSafetyBehavior
        Household members: ${householdMembers.joinToString()}
    """.trimIndent()

    private fun PlanningHorizon.asPromptBlock(): String = """
        Week: $weekStart to $weekEnd
        Source: $source
        Slots:
        ${slots.joinToString("\n") { "- ${it.label}, ${it.date}" }}
    """.trimIndent()

    private fun RequestConstraints.asPromptBlock(): String = """
        Explicit constraints: ${explicitConstraints.ifEmpty { listOf("none") }.joinToString()}
        Allergies or intolerances: ${allergiesOrIntolerances.ifEmpty { listOf("none") }.joinToString()}
        Avoid ingredients: ${avoidIngredients.ifEmpty { listOf("none") }.joinToString()}
        Strong dislikes: ${strongDislikes.ifEmpty { listOf("none") }.joinToString()}
        Nutrition priorities: ${nutritionPriorities.ifEmpty { listOf("none") }.joinToString()}
        Effort hints: ${effortHints.ifEmpty { listOf("none") }.joinToString()}
        Ingredients to use: ${ingredientsToUse.ifEmpty { listOf("none") }.joinToString()}
        Requested week text: ${requestedWeekText ?: "none"}
        Requested meal count: ${requestedMealCount ?: "default"}
        Assumptions: ${assumptions.ifEmpty { listOf(PlanningAssumption("none")) }.joinToString { it.description }}
    """.trimIndent()

    private fun OneOffRecipeContext.asPromptBlock(): String = """
        Title: ${title ?: "untitled"}
        Summary: $summary
        Usable ideas: ${usableIdeas.ifEmpty { listOf("none") }.joinToString()}
        Constraints: ${constraints.ifEmpty { listOf("none") }.joinToString()}
    """.trimIndent()

    private fun LunchCandidateSet.asPromptBlock(): String =
        candidates.joinToString("\n\n") { candidate ->
            """
            - ${candidate.title}
              Description: ${candidate.description}
              Fit: ${candidate.fitReason}
              Nutrition: ${candidate.nutritionNote}
              Prep: ${candidate.prepOrLeftoverNote ?: "none"}
              Constraint handling: ${candidate.constraintHandling.ifEmpty { listOf("none") }.joinToString()}
              Main ingredients: ${candidate.mainIngredients.ifEmpty { listOf("none") }.joinToString()}
            """.trimIndent()
        }

    private fun ValidatedWeeklyLunchPlan.asPromptBlock(): String = """
        Validation notes:
        ${validationNotes.joinToString("\n") { "- $it" }}

        Plan week: ${plan.weekStart} to ${plan.weekEnd}
        Lunches:
        ${plan.lunches.joinToString("\n") { lunch ->
            "- ${lunch.day} ${lunch.date}: ${lunch.title}; ${lunch.description}; fit: ${lunch.whyItFits}; nutrition: ${lunch.nutritionNote}; prep: ${lunch.prepOrLeftoverNote ?: "none"}; constraints: ${lunch.constraintHandling.joinToString()}"
        }}
        Visible constraint handling:
        ${plan.visibleConstraintHandling.joinToString("\n") { "- $it" }}
        Assumptions:
        ${plan.assumptions.joinToString("\n") { "- ${it.description}" }}
    """.trimIndent()

    private fun RequestConstraints.hasVisibleConstraints(): Boolean =
        explicitConstraints.isNotEmpty() ||
            allergiesOrIntolerances.isNotEmpty() ||
            avoidIngredients.isNotEmpty() ||
            strongDislikes.isNotEmpty()

    private companion object {
        val ISO_DATE = Regex("""\b\d{4}-\d{2}-\d{2}\b""")
    }
}
