package com.embabel.mealplanner.agent

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class MealPlannerConfiguration {

    @Bean
    fun mealPlannerClock(): Clock = Clock.systemDefaultZone()
}
