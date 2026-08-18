package com.seipseip.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.seipseip.app.Complete
import com.seipseip.app.Consent
import com.seipseip.app.Denied
import com.seipseip.app.FirstUse
import com.seipseip.app.Login
import com.seipseip.app.Permissions
import com.seipseip.app.SignUp
import com.seipseip.app.Welcome
import com.seipseip.app.feature.guide.GuideDetailScreen
import com.seipseip.app.feature.guide.GuideZoneScreen
import com.seipseip.app.feature.home.HomeScreen
import com.seipseip.app.feature.inspection.AnalysisProgressScreen
import com.seipseip.app.feature.inspection.CaptureResultsScreen
import com.seipseip.app.feature.inspection.FinishConfirmScreen
import com.seipseip.app.feature.inspection.InspectionPrepScreen
import com.seipseip.app.feature.inspection.LiveInspectionScreen
import com.seipseip.app.feature.inspection.ObservationScreen
import com.seipseip.app.feature.inspection.TutorialChecklistScreen
import com.seipseip.app.feature.inspection.TutorialScreen
import com.seipseip.app.feature.magazine.MagazineDetailScreen
import com.seipseip.app.feature.magazine.MagazineScreen
import com.seipseip.app.feature.profile.ProfileScreen
import com.seipseip.app.feature.property.PropertyDetailScreen
import com.seipseip.app.feature.property.PropertyFormScreen
import com.seipseip.app.feature.property.PropertyListScreen
import com.seipseip.app.feature.property.PropertySelectScreen
import com.seipseip.app.feature.report.ReportDetailScreen
import com.seipseip.app.feature.report.ReportListScreen
import com.seipseip.app.feature.state.EmptyPropertyScreen
import com.seipseip.app.feature.state.HomeProcessingScreen
import com.seipseip.app.feature.state.LoadingScreen

object Route {
    const val Loading = "loading"
    const val Login = "login"
    const val SignUp = "signup"
    const val FirstUse = "first_use"
    const val Welcome = "welcome"
    const val Consent = "consent"
    const val Permissions = "permissions"
    const val Denied = "denied"
    const val Complete = "complete"
    const val Home = "home"
    const val HomeProcessing = "home_processing"
    const val PropertyList = "properties"
    const val PropertyForm = "property_form"
    const val PropertyDetail = "property_detail"
    const val PropertySelect = "property_select"
    const val PropertyEmpty = "property_empty"
    const val InspectionPrep = "inspection_prep"
    const val Tutorial = "tutorial"
    const val TutorialChecklist = "tutorial_checklist"
    const val GuideZone = "guide/{zone}"
    const val GuideDetail = "guide_detail/{zone}/{item}"
    const val LiveInspection = "live/{zone}"
    const val FinishConfirm = "finish_confirm"
    const val Analysis = "analysis"
    const val CaptureResults = "capture_results"
    const val Observation = "observation/{zone}"
    const val Reports = "reports"
    const val ReportDetail = "report_detail"
    const val Profile = "profile"
    const val Magazine = "magazine"
    const val MagazineDetail = "magazine_detail"

    fun guideZone(zone: String) = "guide/$zone"
    fun guideDetail(zone: String, item: Int) = "guide_detail/$zone/$item"
    fun liveInspection(zone: String) = "live/$zone"
    fun observation(zone: String) = "observation/$zone"
}

@Composable
fun AppNavGraph(navController: NavHostController) {
    fun goToTab(tab: String) {
        navController.navigate(tab) {
            launchSingleTop = true
            popUpTo(Route.Home)
        }
    }

    NavHost(navController = navController, startDestination = Route.Loading) {
        composable(Route.Loading) {
            LoadingScreen {
                navController.navigate(Route.Login) {
                    popUpTo(Route.Loading) { inclusive = true }
                }
            }
        }
        composable(Route.Login) {
            Login { destination ->
                navController.navigate(when (destination) { "signup" -> Route.SignUp; "guest" -> Route.Welcome; else -> Route.Complete })
            }
        }
        composable(Route.SignUp) {
            SignUp(
                back = navController::popBackStack,
                next = { navController.navigate(Route.FirstUse) },
            )
        }
        composable(Route.FirstUse) {
            FirstUse(
                back = navController::popBackStack,
                next = { navController.navigate(Route.Consent) },
            )
        }
        composable(Route.Welcome) {
            Welcome(
                back = navController::popBackStack,
                next = { navController.navigate(Route.Consent) },
            )
        }
        composable(Route.Consent) {
            Consent(
                back = navController::popBackStack,
                next = { navController.navigate(Route.Permissions) },
            )
        }
        composable(Route.Permissions) {
            Permissions(
                back = navController::popBackStack,
                onGranted = { navController.navigate(Route.Tutorial) },
                onDenied = { navController.navigate(Route.Denied) },
            )
        }
        composable(Route.Denied) {
            Denied(back = navController::popBackStack)
        }
        composable(Route.Complete) {
            Complete {
                navController.navigate(Route.Tutorial) {
                    popUpTo(Route.Login) { inclusive = true }
                }
            }
        }
        composable(Route.Home) {
            HomeScreen(
                onOpenProperties = { navController.navigate(Route.PropertyList) },
                onOpenReports = { navController.navigate(Route.Reports) },
                onOpenMagazine = { navController.navigate(Route.Magazine) },
                onOpenMagazineArticle = { navController.navigate(Route.MagazineDetail) },
                onStartInspection = { navController.navigate(Route.InspectionPrep) },
                onTabSelected = { tab ->
                    goToTab(
                        when (tab) {
                            "property" -> Route.PropertyList
                            "report" -> Route.Reports
                            "profile" -> Route.Profile
                            else -> Route.Home
                        },
                    )
                },
            )
        }
        composable(Route.HomeProcessing) {
            HomeProcessingScreen(
                onOpenProperties = { navController.navigate(Route.PropertyList) },
                onOpenReports = { navController.navigate(Route.Reports) },
                onOpenMagazine = { navController.navigate(Route.Magazine) },
                onTabSelected = { tab ->
                    goToTab(
                        when (tab) {
                            "property" -> Route.PropertyList
                            "report" -> Route.Reports
                            "profile" -> Route.Profile
                            else -> Route.Home
                        },
                    )
                },
            )
        }
        composable(Route.PropertyList) {
            PropertyListScreen(
                onAddProperty = { navController.navigate(Route.PropertyForm) },
                onOpenProperty = { navController.navigate(Route.PropertyDetail) },
                onTabSelected = { tab ->
                    goToTab(
                        when (tab) {
                            "home" -> Route.Home
                            "report" -> Route.Reports
                            "profile" -> Route.Profile
                            else -> Route.PropertyList
                        },
                    )
                },
            )
        }
        composable(Route.PropertyForm) {
            PropertyFormScreen(
                onBack = navController::popBackStack,
                onSaved = { navController.navigate(Route.PropertyDetail) },
            )
        }
        composable(Route.PropertyDetail) {
            PropertyDetailScreen(
                onBack = navController::popBackStack,
                onStartInspection = { navController.navigate(Route.InspectionPrep) },
                onOpenReport = { navController.navigate(Route.ReportDetail) },
                onTabSelected = { tab ->
                    goToTab(
                        when (tab) {
                            "home" -> Route.Home
                            "report" -> Route.Reports
                            "profile" -> Route.Profile
                            else -> Route.PropertyList
                        },
                    )
                },
            )
        }
        composable(Route.PropertyEmpty) {
            EmptyPropertyScreen(
                onBack = navController::popBackStack,
                onAddProperty = { navController.navigate(Route.PropertyForm) },
            )
        }
        composable(Route.PropertySelect) {
            PropertySelectScreen(
                onBack = navController::popBackStack,
                onSelected = { navController.navigate(Route.InspectionPrep) },
            )
        }
        composable(Route.InspectionPrep) {
            InspectionPrepScreen(
                onBack = navController::popBackStack,
                onStartInspection = { navController.navigate(Route.liveInspection("entry")) },
                onSelectProperty = { navController.navigate(Route.PropertySelect) },
            )
        }
        composable(Route.Tutorial) {
            TutorialScreen(
                onBack = navController::popBackStack,
                onNext = { navController.navigate(Route.TutorialChecklist) },
            )
        }
        composable(Route.TutorialChecklist) {
            TutorialChecklistScreen(
                onBack = navController::popBackStack,
                onOpenGuide = { navController.navigate(Route.guideZone("entry")) },
                onStart = { navController.navigate(Route.liveInspection("entry")) },
            )
        }
        composable(
            route = Route.GuideZone,
            arguments = listOf(navArgument("zone") { type = NavType.StringType }),
        ) {
            val zone = it.arguments?.getString("zone") ?: "entry"
            GuideZoneScreen(
                zoneId = zone,
                onBack = navController::popBackStack,
                onOpenDetail = { item -> navController.navigate(Route.guideDetail(zone, item)) },
                onNextGuide = { nextZone -> navController.navigate(Route.guideZone(nextZone)) },
                onStartInspection = { navController.navigate(Route.Home) },
            )
        }
        composable(
            route = Route.GuideDetail,
            arguments = listOf(
                navArgument("zone") { type = NavType.StringType },
                navArgument("item") { type = NavType.IntType },
            ),
        ) {
            val zone = it.arguments?.getString("zone") ?: "entry"
            val item = it.arguments?.getInt("item") ?: 0
            val detailZone = com.seipseip.app.feature.common.UiCatalog.zone(zone)
            val nextGuideZone = com.seipseip.app.feature.common.UiCatalog.nextZone(zone)
            val nextLabel = when {
                item < detailZone.items.lastIndex -> "다음 확인 요소 보기"
                nextGuideZone != null -> "${nextGuideZone.title} 가이드 보기"
                else -> "홈으로 가기"
            }
            GuideDetailScreen(
                zoneId = zone,
                itemIndex = item,
                onBack = navController::popBackStack,
                nextLabel = nextLabel,
                onNext = {
                    when {
                        item < detailZone.items.lastIndex -> navController.navigate(Route.guideDetail(zone, item + 1))
                        nextGuideZone != null -> navController.navigate(Route.guideZone(nextGuideZone.id))
                        else -> navController.navigate(Route.Home)
                    }
                },
            )
        }
        composable(
            route = Route.LiveInspection,
            arguments = listOf(navArgument("zone") { type = NavType.StringType }),
        ) {
            val zone = it.arguments?.getString("zone") ?: "entry"
            LiveInspectionScreen(
                zoneId = zone,
                onBack = navController::popBackStack,
                onOpenGuide = { item -> navController.navigate(Route.guideDetail(zone, item)) },
                onNextZone = { nextZone -> navController.navigate(Route.liveInspection(nextZone)) },
                onFinish = { navController.navigate(Route.FinishConfirm) },
            )
        }
        composable(Route.FinishConfirm) {
            FinishConfirmScreen(
                onBack = navController::popBackStack,
                onConfirm = { navController.navigate(Route.Analysis) },
            )
        }
        composable(Route.Analysis) {
            AnalysisProgressScreen(
                onBackToHome = {
                    navController.navigate(Route.HomeProcessing) {
                        popUpTo(Route.Home) { inclusive = true }
                    }
                },
                onOpenResults = { navController.navigate(Route.CaptureResults) },
            )
        }
        composable(Route.CaptureResults) {
            CaptureResultsScreen(
                onBack = navController::popBackStack,
                onOpenObservation = { zone -> navController.navigate(Route.observation(zone)) },
                onHome = {
                    navController.navigate(Route.Home) {
                        popUpTo(Route.Home) { inclusive = true }
                    }
                },
            )
        }
        composable(
            route = Route.Observation,
            arguments = listOf(navArgument("zone") { type = NavType.StringType }),
        ) {
            val zone = it.arguments?.getString("zone") ?: "entry"
            ObservationScreen(
                zoneId = zone,
                onBack = navController::popBackStack,
                onNextZone = { nextZone -> navController.navigate(Route.observation(nextZone)) },
                onOpenReport = { navController.navigate(Route.ReportDetail) },
            )
        }
        composable(Route.Reports) {
            ReportListScreen(
                onOpenReport = { navController.navigate(Route.ReportDetail) },
                onTabSelected = { tab ->
                    goToTab(
                        when (tab) {
                            "home" -> Route.Home
                            "property" -> Route.PropertyList
                            "profile" -> Route.Profile
                            else -> Route.Reports
                        },
                    )
                },
            )
        }
        composable(Route.ReportDetail) {
            ReportDetailScreen(
                onBack = navController::popBackStack,
                onOpenProperty = { navController.navigate(Route.PropertyDetail) },
            )
        }
        composable(Route.Profile) {
            ProfileScreen(
                onTabSelected = { tab ->
                    goToTab(
                        when (tab) {
                            "home" -> Route.Home
                            "property" -> Route.PropertyList
                            "report" -> Route.Reports
                            else -> Route.Profile
                        },
                    )
                },
            )
        }
        composable(Route.Magazine) {
            MagazineScreen(
                onBack = navController::popBackStack,
                onOpenArticle = { navController.navigate(Route.MagazineDetail) },
            )
        }
        composable(Route.MagazineDetail) {
            MagazineDetailScreen(onBack = navController::popBackStack)
        }
    }
}