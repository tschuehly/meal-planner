package com.embabel.mealplanner.v0

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

class MealPlannerV0AgentTest {

    private val clock: Clock = Clock.fixed(
        Instant.parse("2026-05-06T10:00:00Z"),
        ZoneId.of("Europe/Berlin"),
    )
    private val agent = MealPlannerV0Agent(clock, MealPlannerV0PromptLibrary())

    @Test
    fun `default planning horizon is next Monday through Friday`() {
        val request = HouseholdLunchPlanningRequest("Plan easy high-protein lunches.")
        val defaults = agent.provideV0LunchPlanningDefaults(request)

        val horizon = agent.resolvePlanningHorizon(request, defaults)

        assertEquals(LocalDate.of(2026, 5, 11), horizon.weekStart)
        assertEquals(LocalDate.of(2026, 5, 15), horizon.weekEnd)
        assertEquals(listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY), horizon.slots.map { it.day })
        assertEquals("Defaulted to next week.", horizon.source)
    }

    @Test
    fun `explicit ISO date resolves to the containing weekday week`() {
        val request = HouseholdLunchPlanningRequest("Plan lunches for the week of 2026-06-17.")
        val defaults = agent.provideV0LunchPlanningDefaults(request)

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
        context.expectResponse(expected)

        val result = agent.interpretLunchPlanningRequest(
            HouseholdLunchPlanningRequest("Plan next week and avoid mushrooms."),
            LunchPlanningDefaults(),
            context,
        )

        assertEquals(expected, result)
        val prompt = context.llmInvocations.first().prompt
        assertTrue(prompt.contains("avoid mushrooms"))
        assertTrue(prompt.contains("v0 Defaults"))
        assertEquals("meal-planner-v0-interpret-request", context.llmInvocations.first().interaction.id.value)
    }

    @Test
    fun `validation accepts a five lunch plan with visible constraints and assumptions`() {
        val constraints = RequestConstraints(
            explicitConstraints = listOf("avoid mushrooms"),
            avoidIngredients = listOf("mushrooms"),
        )

        val validated = agent.validateWeeklyLunchPlan(
            plan = validPlan(),
            constraints = constraints,
            defaults = LunchPlanningDefaults(),
        )

        assertTrue(validated.validationNotes.any { it.contains("Monday-Friday") })
        assertEquals(5, validated.plan.lunches.size)
    }

    @Test
    fun `validation rejects constrained plans without visible constraint handling`() {
        val plan = validPlan().copy(visibleConstraintHandling = emptyList())
        val constraints = RequestConstraints(avoidIngredients = listOf("mushrooms"))

        val failure = assertThrows(IllegalArgumentException::class.java) {
            agent.validateWeeklyLunchPlan(plan, constraints, LunchPlanningDefaults())
        }

        assertTrue(failure.message!!.contains("visible constraint handling"))
    }

    @Test
    fun `validation rejects missing assumptions`() {
        val plan = validPlan().copy(assumptions = emptyList())

        val failure = assertThrows(IllegalArgumentException::class.java) {
            agent.validateWeeklyLunchPlan(plan, RequestConstraints(), LunchPlanningDefaults())
        }

        assertTrue(failure.message!!.contains("assumptions"))
    }

    @Test
    fun `agent metadata exposes the v0 goal and expected actions`() {
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
