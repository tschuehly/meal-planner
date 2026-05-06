package com.embabel.mealplanner.v0

import org.springframework.stereotype.Component

@Component
class MealPlannerV0PromptLibrary {

    fun load(name: String): String {
        val resourcePath = "prompts/v0/$name.md"
        val resource = Thread.currentThread().contextClassLoader.getResource(resourcePath)
            ?: error("Missing prompt resource: $resourcePath")
        return resource.readText()
    }
}
