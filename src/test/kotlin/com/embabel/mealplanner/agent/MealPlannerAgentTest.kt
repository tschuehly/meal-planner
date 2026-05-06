package com.embabel.mealplanner.agent

import com.embabel.agent.api.annotation.support.AgentMetadataReader
import com.embabel.agent.test.unit.FakeOperationContext
import com.embabel.agent.core.Agent as CoreAgent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class MealPlannerAgentTest {

    private val clock: Clock = Clock.fixed(
        Instant.parse("2026-05-06T10:00:00Z"),
        ZoneId.of("Europe/Berlin"),
    )
    private val agent = MealPlannerAgent(clock, MealPlannerPromptLibrary())

    @Test
    fun `default planning horizon is next Monday through Friday`() {
        val request = HouseholdLunchPlanningRequest("Plan easy high-protein lunches.")
        val defaults = agent.provideLunchPlanningDefaults(request)

        val horizon = agent.resolvePlanningHorizon(request, defaults)

        assertEquals(LocalDate.of(2026, 5, 11), horizon.weekStart)
        assertEquals(LocalDate.of(2026, 5, 15), horizon.weekEnd)
        assertEquals(listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY), horizon.slots.map { it.day })
        assertEquals("Defaulted to next week.", horizon.source)
    }

    @Test
    fun `explicit ISO date resolves to the containing weekday week`() {
        val request = HouseholdLunchPlanningRequest("Plan lunches for the week of 2026-06-17.")
        val defaults = agent.provideLunchPlanningDefaults(request)

        val horizon = agent.resolvePlanningHorizon(request, defaults)

        assertEquals(LocalDate.of(2026, 6, 15), horizon.weekStart)
        assertEquals(LocalDate.of(2026, 6, 19), horizon.weekEnd)
        assertEquals("Resolved from the request text.", horizon.source)
    }

    @Test
    fun `interpret action sends request and defaults to the LLM`() {
        val context = FakeOperationContext.create()
        val expected = InterpretedLunchRequest(
            requestConstraints = RequestConstraints(
                avoidIngredients = listOf("mushrooms"),
                nutritionPriorities = listOf("high protein"),
                assumptions = listOf(PlanningAssumption("Lunches can use leftovers.")),
            ),
        )
        context.expectResponse(
            LlmInterpretedLunchRequest(
                requestConstraints = expected.requestConstraints,
                oneOffRecipeContext = null,
            ),
        )

        val result = agent.interpretLunchPlanningRequest(
            HouseholdLunchPlanningRequest("Plan next week and avoid mushrooms."),
            LunchPlanningDefaults(),
            context,
        )

        assertEquals(expected, result)
        val prompt = context.llmInvocations.first().prompt
        assertTrue(prompt.contains("avoid mushrooms"))
        assertTrue(prompt.contains("Defaults"))
        assertEquals("meal-planner-interpret-request", context.llmInvocations.first().interaction.id.value)
    }

    @Test
    fun `interpret action normalizes empty recipe context to absent`() {
        val context = FakeOperationContext.create()
        context.expectResponse(
            LlmInterpretedLunchRequest(
                requestConstraints = RequestConstraints(ingredientsToUse = listOf("chickpeas")),
                oneOffRecipeContext = LlmOneOffRecipeContext(),
            ),
        )

        val result = agent.interpretLunchPlanningRequest(
            HouseholdLunchPlanningRequest("Use chickpeas if they fit."),
            LunchPlanningDefaults(),
            context,
        )

        assertEquals(null, result.oneOffRecipeContext)
        assertTrue(context.llmInvocations.first().prompt.contains("oneOffRecipeContext"))
    }

    @Test
    fun `validation accepts a five lunch plan with visible constraints and assumptions`() {
        val context = FakeOperationContext.create()
        val constraints = RequestConstraints(
            explicitConstraints = listOf("avoid mushrooms"),
            avoidIngredients = listOf("mushrooms"),
        )

        val validated = agent.validateWeeklyLunchPlan(
            plan = validPlan(),
            constraints = constraints,
            defaults = LunchPlanningDefaults(),
            context = context,
        )

        assertTrue(validated.validationNotes.any { it.contains("Monday-Friday") })
        assertEquals(5, validated.plan.lunches.size)
        assertEquals(0, context.llmInvocations.size)
    }

    @Test
    fun `validation repairs constrained plans without visible constraint handling`() {
        val context = FakeOperationContext.create()
        context.expectResponse(validPlan())
        val plan = validPlan().copy(visibleConstraintHandling = emptyList())
        val constraints = RequestConstraints(avoidIngredients = listOf("mushrooms"))

        val validated = agent.validateWeeklyLunchPlan(plan, constraints, LunchPlanningDefaults(), context)

        assertEquals(validPlan(), validated.plan)
        assertEquals("meal-planner-repair-plan-1", context.llmInvocations.first().interaction.id.value)
        assertTrue(context.llmInvocations.first().prompt.contains("mushrooms"))
    }

    @Test
    fun `validation repairs missing assumptions`() {
        val context = FakeOperationContext.create()
        context.expectResponse(validPlan())
        val plan = validPlan().copy(assumptions = emptyList())

        val validated = agent.validateWeeklyLunchPlan(plan, RequestConstraints(), LunchPlanningDefaults(), context)

        assertEquals(validPlan(), validated.plan)
        assertEquals("meal-planner-repair-plan-1", context.llmInvocations.first().interaction.id.value)
        assertTrue(context.llmInvocations.first().prompt.contains("assumptions"))
    }

    @Test
    fun `validation repairs partially surfaced visible constraints`() {
        val context = FakeOperationContext.create()
        val repaired = validPlan().copy(
            visibleConstraintHandling = listOf("Avoid mushrooms.", "Avoid peanuts."),
        )
        context.expectResponse(repaired)
        val plan = validPlan().copy(visibleConstraintHandling = listOf("Avoid mushrooms."))
        val constraints = RequestConstraints(avoidIngredients = listOf("mushrooms", "peanuts"))

        val validated = agent.validateWeeklyLunchPlan(plan, constraints, LunchPlanningDefaults(), context)

        assertEquals(repaired, validated.plan)
        assertTrue(context.llmInvocations.first().prompt.contains("peanuts"))
    }

    @Test
    fun `validation repairs unsupported nutrition claims`() {
        val context = FakeOperationContext.create()
        context.expectResponse(validPlan())
        val badLunch = validPlan().lunches.first().copy(
            nutritionNote = "High protein with omega-3 from broccoli.",
        )
        val plan = validPlan().copy(lunches = listOf(badLunch) + validPlan().lunches.drop(1))

        val validated = agent.validateWeeklyLunchPlan(plan, RequestConstraints(), LunchPlanningDefaults(), context)

        assertEquals(validPlan(), validated.plan)
        assertEquals("meal-planner-repair-plan-1", context.llmInvocations.first().interaction.id.value)
        assertTrue(context.llmInvocations.first().prompt.contains("omega-3 from broccoli"))
    }

    @Test
    fun `validation fails after bounded unsuccessful repairs`() {
        val context = FakeOperationContext.create()
        val invalidLunch = validPlan().lunches.first().copy(title = "")
        val invalidPlan = validPlan().copy(lunches = listOf(invalidLunch) + validPlan().lunches.drop(1))
        context.expectResponse(invalidPlan)
        context.expectResponse(invalidPlan)

        val failure = assertThrows(IllegalStateException::class.java) {
            agent.validateWeeklyLunchPlan(invalidPlan, RequestConstraints(), LunchPlanningDefaults(), context)
        }

        assertTrue(failure.message!!.contains("2 repair attempts"))
        assertEquals(2, context.llmInvocations.size)
    }

    @Test
    fun `format response prompt does not mention absent recipe context`() {
        val context = FakeOperationContext.create()
        context.expectResponse("Week: 2026-05-11 to 2026-05-15")

        val response = agent.formatWeeklyLunchPlanResponse(
            validatedPlan = ValidatedWeeklyLunchPlan(validPlan(), listOf("Plan contains 5 lunches.")),
            horizon = PlanningHorizon(
                weekStart = LocalDate.of(2026, 5, 11),
                weekEnd = LocalDate.of(2026, 5, 15),
                slots = emptyList(),
                source = "Test.",
            ),
            constraints = RequestConstraints(),
            oneOffRecipeContext = null,
            context = context,
        )

        val prompt = context.llmInvocations.first().prompt
        assertTrue(prompt.contains("# One-Off Recipe Context"))
        assertTrue(prompt.contains("None provided."))
        assertTrue("Recipe context:" !in response.content)
    }

    @Test
    fun `format response prompt includes provided recipe context`() {
        val context = FakeOperationContext.create()
        context.expectResponse("Week: 2026-05-11 to 2026-05-15")

        val response = agent.formatWeeklyLunchPlanResponse(
            validatedPlan = ValidatedWeeklyLunchPlan(validPlan(), listOf("Plan contains 5 lunches.")),
            horizon = PlanningHorizon(
                weekStart = LocalDate.of(2026, 5, 11),
                weekEnd = LocalDate.of(2026, 5, 15),
                slots = emptyList(),
                source = "Test.",
            ),
            constraints = RequestConstraints(),
            oneOffRecipeContext = OneOffRecipeContext(summary = "Pasted chickpea salad recipe."),
            context = context,
        )

        val prompt = context.llmInvocations.first().prompt
        assertTrue(prompt.contains("Pasted chickpea salad recipe."))
        assertTrue(response.content.contains("Recipe context: Used only for this run."))
    }

    @Test
    fun `agent metadata exposes the goal and expected actions`() {
        val metadata = AgentMetadataReader().createAgentMetadata(agent) as CoreAgent

        assertTrue(
            metadata.goals.any { it.outputType?.isAssignableTo(WeeklyLunchPlanResponse::class.java) == true },
            "Expected a goal producing WeeklyLunchPlanResponse.",
        )
        val actionNames = metadata.actions.map { it.name }
        assertTrue(actionNames.any { it.endsWith(".ingestHouseholdLunchPlanningRequest") })
        assertTrue(actionNames.any { it.endsWith(".interpretLunchPlanningRequest") })
        assertTrue(actionNames.any { it.endsWith(".formatWeeklyLunchPlanResponse") })
    }

    private fun validPlan(): WeeklyLunchPlan {
        val weekStart = LocalDate.of(2026, 5, 11)
        val lunches = (0L until 5L).map { offset ->
            val date = weekStart.plusDays(offset)
            PlannedLunch(
                day = date.dayOfWeek,
                date = date,
                title = "Lunch ${offset + 1}",
                description = "A practical bowl with grains, vegetables, and protein.",
                whyItFits = "Works for Thomas and Cassandra and avoids the requested ingredient.",
                nutritionNote = "Protein-forward with vegetables and steady carbs.",
                prepOrLeftoverNote = "Prep components once and assemble quickly.",
                constraintHandling = listOf("No mushrooms."),
            )
        }
        return WeeklyLunchPlan(
            weekStart = weekStart,
            weekEnd = weekStart.plusDays(4),
            lunches = lunches,
            visibleConstraintHandling = listOf("The plan avoids mushrooms."),
            assumptions = listOf(PlanningAssumption("Lunches should be easy to moderate effort.")),
        )
    }
}
