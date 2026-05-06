package com.embabel.mealplanner.agent

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
class MealPlannerAgent(
    private val clock: Clock,
    private val prompts: MealPlannerPromptLibrary,
) {

    @Action(
        description = "Convert shell user input into a household lunch-planning request.",
        readOnly = true,
    )
    fun ingestHouseholdLunchPlanningRequest(userInput: UserInput): HouseholdLunchPlanningRequest =
        HouseholdLunchPlanningRequest(
            content = userInput.content.trim(),
            receivedAt = userInput.timestamp,
        )

    @Action(
        description = "Provide the supported lunch-planning defaults.",
        readOnly = true,
    )
    fun provideLunchPlanningDefaults(
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
    ): InterpretedLunchRequest {
        val interpreted = context.ai()
            .withDefaultLlm()
            .withId("meal-planner-interpret-request")
            .withSystemPrompt("You extract typed household lunch-planning facts for the supported workflow.")
            .creating(LlmInterpretedLunchRequest::class.java)
            .withExample(
                "No pasted recipe context",
                LlmInterpretedLunchRequest(
                    requestConstraints = RequestConstraints(
                        avoidIngredients = listOf("mushrooms"),
                        nutritionPriorities = listOf("high-protein"),
                        requestedWeekText = "next week",
                        requestedMealCount = 5,
                    ),
                    oneOffRecipeContext = null,
                ),
            )
            .withExample(
                "Pasted recipe context is present",
                LlmInterpretedLunchRequest(
                    requestConstraints = RequestConstraints(
                        effortHints = listOf("prep easy"),
                        ingredientsToUse = listOf("chickpeas"),
                    ),
                    oneOffRecipeContext = LlmOneOffRecipeContext(
                        title = "Lemony chickpea salad",
                        summary = "A pasted chickpea salad recipe that can inspire weekday lunches.",
                        usableIdeas = listOf("lemon dressing", "chilled chickpea base"),
                        constraints = listOf("use only for this run"),
                    ),
                ),
            )
            .fromPrompt(buildInterpretPrompt(request, defaults))

        return interpreted.toDomain()
    }

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
            .withId("meal-planner-draft-candidates")
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
    ): WeeklyLunchPlan {
        return context.ai()
            .withDefaultLlm()
            .withId("meal-planner-assemble-plan")
            .withSystemPrompt("You assemble a one-shot weekly lunch plan for the supported workflow.")
            .createObject(
                prompt = buildAssemblyPrompt(horizon, defaults, constraints, candidates),
                outputClass = WeeklyLunchPlan::class.java,
            )
    }

    @Action(
        description = "Validate the weekly lunch plan structure and visible constraint handling.",
        readOnly = true,
    )
    fun validateWeeklyLunchPlan(
        plan: WeeklyLunchPlan,
        constraints: RequestConstraints,
        defaults: LunchPlanningDefaults,
        context: OperationContext,
    ): ValidatedWeeklyLunchPlan {
        var currentPlan = plan
        var repairAttempt = 0

        while (true) {
            val validation = inspectWeeklyLunchPlan(currentPlan, constraints, defaults)
            if (validation.issues.isEmpty()) {
                return ValidatedWeeklyLunchPlan(plan = currentPlan, validationNotes = validation.notes)
            }

            if (repairAttempt >= MAX_REPAIR_ATTEMPTS) {
                error(
                    "Weekly lunch plan is invalid after $MAX_REPAIR_ATTEMPTS repair attempts: " +
                        validation.issues.joinToString("; "),
                )
            }

            repairAttempt += 1
            currentPlan = repairWeeklyLunchPlan(
                plan = currentPlan,
                constraints = constraints,
                defaults = defaults,
                issues = validation.issues,
                attempt = repairAttempt,
                context = context,
            )
        }
    }

    private fun inspectWeeklyLunchPlan(
        plan: WeeklyLunchPlan,
        constraints: RequestConstraints,
        defaults: LunchPlanningDefaults,
    ): ValidationInspection {
        val notes = mutableListOf<String>()
        val issues = mutableListOf<String>()

        if (plan.lunches.size == defaults.mealCount) {
            notes += "Plan contains ${defaults.mealCount} lunches."
        } else {
            issues += "Expected ${defaults.mealCount} weekday lunches, got ${plan.lunches.size}."
        }

        val expectedDays = DayOfWeek.entries
            .filter { it.value in DayOfWeek.MONDAY.value..DayOfWeek.FRIDAY.value }
            .take(defaults.mealCount)
        val actualDays = plan.lunches.map { it.day }
        if (actualDays == expectedDays) {
            notes += "Plan contains Monday-Friday lunches in order."
        } else {
            issues += "Expected Monday-Friday lunches in order, got $actualDays."
        }

        plan.lunches.forEach { lunch ->
            if (lunch.title.isBlank()) {
                issues += "Lunch for ${lunch.day} must have a title."
            }
            if (lunch.description.isBlank()) {
                issues += "Lunch for ${lunch.day} must have a description."
            }
            if (lunch.whyItFits.isBlank()) {
                issues += "Lunch for ${lunch.day} must explain why it fits."
            }
            if (lunch.nutritionNote.isBlank()) {
                issues += "Lunch for ${lunch.day} must include a rough nutrition note."
            }
        }
        if (issues.none { it.startsWith("Lunch for ") }) {
            notes += "Each lunch includes a title, description, fit reason, and rough nutrition note."
        }

        val missingVisibleConstraints = constraints.expectedVisibleConstraintTerms()
            .filterNot { term -> plan.visibleConstraintHandling.any { it.contains(term, ignoreCase = true) } }
        if (missingVisibleConstraints.isEmpty()) {
            notes += "Explicit constraints are surfaced in the plan."
        } else {
            issues += "Plan must visibly handle these request constraints: ${missingVisibleConstraints.joinToString()}."
        }

        val missingLunchConstraintTerms = constraints.expectedIngredientConstraintTerms()
            .filterNot { term ->
                plan.lunches.all { lunch ->
                    lunch.constraintHandling.any { it.contains(term, ignoreCase = true) } ||
                        lunch.description.contains(term, ignoreCase = true).not() &&
                        lunch.whyItFits.contains(term, ignoreCase = true).not()
                }
            }
        if (missingLunchConstraintTerms.isEmpty()) {
            notes += "Lunches do not contradict ingredient constraints."
        } else {
            issues += "Lunches must handle or avoid these ingredient constraints: ${missingLunchConstraintTerms.joinToString()}."
        }

        if (plan.assumptions.isNotEmpty()) {
            notes += "Plan includes assumptions the household can correct later."
        } else {
            issues += "Plan must include assumptions for underspecified planning details."
        }

        val moralizingTerms = listOf("cheat meal", "guilt", "bad food", "clean eating", "calorie compliance")
        val planText = plan.toString().lowercase()
        if (moralizingTerms.none { it in planText }) {
            notes += "Tone avoids moralizing nutrition language."
        } else {
            issues += "Plan must avoid moralizing or calorie-compliance language."
        }

        val unsupportedNutritionClaims = unsupportedNutritionClaimsIn(planText)
        if (unsupportedNutritionClaims.isEmpty()) {
            notes += "Nutrition notes avoid known unsupported source claims."
        } else {
            issues += "Plan must avoid unsupported nutrition claims: ${unsupportedNutritionClaims.joinToString()}."
        }

        return ValidationInspection(notes = notes, issues = issues)
    }

    private fun repairWeeklyLunchPlan(
        plan: WeeklyLunchPlan,
        constraints: RequestConstraints,
        defaults: LunchPlanningDefaults,
        issues: List<String>,
        attempt: Int,
        context: OperationContext,
    ): WeeklyLunchPlan =
        context.ai()
            .withDefaultLlm()
            .withId("meal-planner-repair-plan-$attempt")
            .withSystemPrompt("You repair structured weekly lunch plans by fixing validation issues without changing valid content unnecessarily.")
            .createObject(
                prompt = buildRepairPrompt(plan, constraints, defaults, issues),
                outputClass = WeeklyLunchPlan::class.java,
            )

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
        oneOffRecipeContext: OneOffRecipeContext?,
        context: OperationContext,
    ): WeeklyLunchPlanResponse {
        val generatedLunchText = context.ai()
            .withDefaultLlm()
            .withId("meal-planner-format-response")
            .withSystemPrompt("You write concise, practical command-line lunch descriptions without adding factual boilerplate.")
            .createObject(
                prompt = buildResponsePrompt(validatedPlan, horizon, constraints, oneOffRecipeContext),
                outputClass = String::class.java,
            )
            .trim()
        val content = buildFinalResponse(
            generatedLunchText = generatedLunchText,
            validatedPlan = validatedPlan,
            horizon = horizon,
            constraints = constraints,
            oneOffRecipeContext = oneOffRecipeContext,
        )
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

        # Defaults
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

        # Defaults
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

        # Defaults
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
        oneOffRecipeContext: OneOffRecipeContext?,
    ): String = """
        ${prompts.load("format-response")}

        # Planning Horizon
        ${horizon.asPromptBlock()}

        # Request Constraints
        ${constraints.asPromptBlock()}

        # One-Off Recipe Context
        ${oneOffRecipeContext?.asPromptBlock() ?: "None provided."}

        # Validated Plan
        ${validatedPlan.asPromptBlock()}
    """.trimIndent()

    private fun buildFinalResponse(
        generatedLunchText: String,
        validatedPlan: ValidatedWeeklyLunchPlan,
        horizon: PlanningHorizon,
        constraints: RequestConstraints,
        oneOffRecipeContext: OneOffRecipeContext?,
    ): String = buildString {
        appendLine("Week: ${horizon.weekStart} to ${horizon.weekEnd}")
        appendLine()
        appendLine(generatedLunchText.trim())
        appendLine()
        if (constraints.hasVisibleConstraints()) {
            appendLine("Constraints:")
            constraints.expectedVisibleConstraintSentences().forEach { appendLine("- $it") }
            appendLine()
        }
        appendLine("Assumptions:")
        validatedPlan.plan.assumptions.forEach { appendLine("- ${it.description}") }
        oneOffRecipeContext?.let {
            appendLine()
            appendLine("Recipe context: Used only for this run.")
        }
    }.trim()

    private fun buildRepairPrompt(
        plan: WeeklyLunchPlan,
        constraints: RequestConstraints,
        defaults: LunchPlanningDefaults,
        issues: List<String>,
    ): String = """
        ${prompts.load("repair-plan")}

        # Validation Issues To Fix
        ${issues.joinToString("\n") { "- $it" }}

        # Defaults
        ${defaults.asPromptBlock()}

        # Request Constraints
        ${constraints.asPromptBlock()}

        # Current Plan
        ${plan.asPromptBlock()}
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

    private fun WeeklyLunchPlan.asPromptBlock(): String = """
        Plan week: $weekStart to $weekEnd
        Lunches:
        ${lunches.joinToString("\n") { lunch ->
            "- ${lunch.day} ${lunch.date}: ${lunch.title}; ${lunch.description}; fit: ${lunch.whyItFits}; nutrition: ${lunch.nutritionNote}; prep: ${lunch.prepOrLeftoverNote ?: "none"}; constraints: ${lunch.constraintHandling.joinToString()}"
        }}
        Visible constraint handling:
        ${visibleConstraintHandling.joinToString("\n") { "- $it" }}
        Assumptions:
        ${assumptions.joinToString("\n") { "- ${it.description}" }}
    """.trimIndent()

    private fun ValidatedWeeklyLunchPlan.asPromptBlock(): String = """
        Validation notes:
        ${validationNotes.joinToString("\n") { "- $it" }}

        ${plan.asPromptBlock()}
    """.trimIndent()

    private fun RequestConstraints.expectedVisibleConstraintTerms(): List<String> =
        buildList {
            val ingredientTerms = expectedIngredientConstraintTerms()
            addAll(ingredientTerms)
            explicitConstraints
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .filterNot { explicit -> ingredientTerms.any { explicit.contains(it, ignoreCase = true) } }
                .forEach { add(it) }
        }.distinct()

    private fun RequestConstraints.expectedIngredientConstraintTerms(): List<String> =
        (allergiesOrIntolerances + avoidIngredients + strongDislikes).map { it.trim() }.filter { it.isNotBlank() }.distinct()

    private fun RequestConstraints.expectedVisibleConstraintSentences(): List<String> =
        buildList {
            allergiesOrIntolerances.forEach { add("Avoid $it.") }
            avoidIngredients.forEach { add("Avoid $it.") }
            strongDislikes.forEach { add("Avoid $it.") }
            explicitConstraints.forEach { add(it.ensureSentence()) }
        }.distinct()

    private fun String.ensureSentence(): String {
        val trimmed = trim()
        return if (trimmed.endsWith(".") || trimmed.endsWith("!") || trimmed.endsWith("?")) {
            trimmed
        } else {
            "$trimmed."
        }
    }

    private fun RequestConstraints.hasVisibleConstraints(): Boolean =
        explicitConstraints.isNotEmpty() ||
            allergiesOrIntolerances.isNotEmpty() ||
            avoidIngredients.isNotEmpty() ||
            strongDislikes.isNotEmpty()

    private fun unsupportedNutritionClaimsIn(planText: String): List<String> =
        UNSUPPORTED_NUTRIENT_SOURCE_CLAIMS
            .filter { (nutrient, ingredient) ->
                (planText.contains("$nutrient from $ingredient") ||
                    planText.contains("$ingredient is ${articleFor(nutrient)} $nutrient source") ||
                    planText.contains("$ingredient is a source of $nutrient"))
            }
            .map { (nutrient, ingredient) -> "$nutrient from $ingredient" }

    private fun articleFor(term: String): String =
        if (term.firstOrNull() in listOf('a', 'e', 'i', 'o', 'u')) "an" else "a"

    private companion object {
        val ISO_DATE = Regex("""\b\d{4}-\d{2}-\d{2}\b""")
        const val MAX_REPAIR_ATTEMPTS = 2
        val UNSUPPORTED_NUTRIENT_SOURCE_CLAIMS = listOf(
            "omega-3" to "broccoli",
            "omega 3" to "broccoli",
            "dha" to "spinach",
            "epa" to "spinach",
            "complete protein" to "cucumber",
        )
    }

    private data class ValidationInspection(
        val notes: List<String>,
        val issues: List<String>,
    )
}

data class LlmInterpretedLunchRequest(
    val requestConstraints: RequestConstraints = RequestConstraints(),
    val oneOffRecipeContext: LlmOneOffRecipeContext? = null,
) {
    fun toDomain(): InterpretedLunchRequest =
        InterpretedLunchRequest(
            requestConstraints = requestConstraints,
            oneOffRecipeContext = oneOffRecipeContext?.toDomainOrNull(),
        )
}

data class LlmOneOffRecipeContext(
    val title: String? = null,
    val summary: String? = null,
    val usableIdeas: List<String> = emptyList(),
    val constraints: List<String> = emptyList(),
) {
    fun toDomainOrNull(): OneOffRecipeContext? {
        val cleanTitle = title?.trim()?.takeIf { it.isNotBlank() }
        val cleanSummary = summary?.trim()?.takeIf { it.isNotBlank() }
        val cleanIdeas = usableIdeas.map { it.trim() }.filter { it.isNotBlank() }
        val cleanConstraints = constraints.map { it.trim() }.filter { it.isNotBlank() }
        if (cleanTitle == null && cleanSummary == null && cleanIdeas.isEmpty() && cleanConstraints.isEmpty()) {
            return null
        }
        return OneOffRecipeContext(
            title = cleanTitle,
            summary = cleanSummary ?: listOfNotNull(cleanTitle, cleanIdeas.joinToString().takeIf { it.isNotBlank() })
                .joinToString(": ")
                .ifBlank { "Recipe context provided for this run." },
            usableIdeas = cleanIdeas,
            constraints = cleanConstraints,
        )
    }
}
