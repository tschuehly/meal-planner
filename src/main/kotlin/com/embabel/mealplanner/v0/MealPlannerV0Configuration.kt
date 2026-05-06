package com.embabel.mealplanner.v0

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class MealPlannerV0Configuration {

    @Bean
    fun mealPlannerClock(): Clock = Clock.systemDefaultZone()
}
