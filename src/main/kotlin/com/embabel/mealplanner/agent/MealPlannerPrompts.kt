package com.embabel.mealplanner.agent

import org.springframework.stereotype.Component

@Component
class MealPlannerPromptLibrary {

    fun load(name: String): String {
        val resourcePath = "prompts/meal-planner/$name.md"
        val resource = Thread.currentThread().contextClassLoader.getResource(resourcePath)
            ?: error("Missing prompt resource: $resourcePath")
        return resource.readText()
    }
}
