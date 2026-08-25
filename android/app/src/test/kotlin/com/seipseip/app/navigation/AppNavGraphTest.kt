package com.seipseip.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class AppNavGraphTest {
    @Test
    fun initialRouteFollowsSavedSessionFlags() {
        assertEquals(
            Route.Login,
            initialRouteFor(isLoggedIn = false, isTutorialCompleted = false, isOnboardingCompleted = false),
        )
        assertEquals(
            Route.Login,
            initialRouteFor(isLoggedIn = false, isTutorialCompleted = false, isOnboardingCompleted = true),
        )
        assertEquals(
            Route.Welcome,
            initialRouteFor(isLoggedIn = true, isTutorialCompleted = false, isOnboardingCompleted = false),
        )
        assertEquals(
            Route.Consent,
            initialRouteFor(isLoggedIn = true, isTutorialCompleted = false, isOnboardingCompleted = true),
        )
        assertEquals(
            Route.Home,
            initialRouteFor(isLoggedIn = true, isTutorialCompleted = true, isOnboardingCompleted = true),
        )
    }
}
