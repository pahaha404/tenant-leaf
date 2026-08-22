package com.seipseip.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class AppNavGraphTest {
    @Test
    fun initialRouteFollowsSavedSessionFlags() {
        assertEquals(Route.Login, initialRouteFor(isLoggedIn = false, isTutorialCompleted = false))
        assertEquals(Route.Welcome, initialRouteFor(isLoggedIn = true, isTutorialCompleted = false))
        assertEquals(Route.Home, initialRouteFor(isLoggedIn = true, isTutorialCompleted = true))
    }
}
