package com.seipseip.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.seipseip.feature.property.presentation.PropertyDetailRoute
import com.seipseip.feature.property.presentation.PropertyDetailViewModel
import com.seipseip.feature.property.presentation.PropertyFormRoute
import com.seipseip.feature.property.presentation.PropertyFormViewModel
import com.seipseip.feature.property.presentation.PropertyListRoute
import java.util.UUID

private object Routes {
    const val PROPERTY_LIST = "properties"
    const val PROPERTY_CREATE = "properties/create"
    const val PROPERTY_DETAIL = "properties/{propertyId}"
    const val PROPERTY_EDIT = "properties/{propertyId}/edit"

    fun detail(id: UUID) = "properties/$id"
    fun edit(id: UUID) = "properties/$id/edit"
}

@Composable
fun TenantLeafNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.PROPERTY_LIST) {
        composable(Routes.PROPERTY_LIST) {
            PropertyListRoute(
                onCreate = { navController.navigate(Routes.PROPERTY_CREATE) },
                onSelect = { navController.navigate(Routes.detail(it)) },
            )
        }
        composable(Routes.PROPERTY_CREATE) {
            PropertyFormRoute(
                onBack = navController::popBackStack,
                onSaved = { id ->
                    navController.navigate(Routes.detail(id)) {
                        popUpTo(Routes.PROPERTY_LIST) { inclusive = false }
                    }
                },
            )
        }
        composable(
            route = Routes.PROPERTY_DETAIL,
            arguments = listOf(navArgument(PropertyDetailViewModel.PROPERTY_ID_ARGUMENT) { type = NavType.StringType }),
        ) {
            PropertyDetailRoute(
                onBack = navController::popBackStack,
                onEdit = { navController.navigate(Routes.edit(it)) },
                onDeleted = {
                    navController.navigate(Routes.PROPERTY_LIST) {
                        popUpTo(Routes.PROPERTY_LIST) { inclusive = true }
                    }
                },
            )
        }
        composable(
            route = Routes.PROPERTY_EDIT,
            arguments = listOf(navArgument(PropertyFormViewModel.PROPERTY_ID_ARGUMENT) { type = NavType.StringType }),
        ) {
            PropertyFormRoute(
                onBack = navController::popBackStack,
                onSaved = { id ->
                    navController.navigate(Routes.detail(id)) {
                        popUpTo(Routes.PROPERTY_DETAIL) { inclusive = true }
                    }
                },
            )
        }
    }
}
