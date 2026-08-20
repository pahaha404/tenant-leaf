package com.seipseip.app.navigation

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.seipseip.app.feature.guide.ChecklistOverviewScreen
import com.seipseip.app.feature.guide.GuideDetailScreen
import com.seipseip.app.feature.guide.GuideZoneScreen
import com.seipseip.app.feature.home.HomeScreen
import com.seipseip.app.feature.inspection.AnalysisProgressScreen
import com.seipseip.app.feature.inspection.InspectionCountdownScreen
import com.seipseip.app.feature.inspection.FinishConfirmScreen
import com.seipseip.app.feature.inspection.InspectionPrepScreen
import com.seipseip.app.feature.inspection.InspectionPermissionWarningScreen
import com.seipseip.app.feature.inspection.LiveInspectionScreen
import com.seipseip.app.feature.inspection.ObservationScreen
import com.seipseip.app.feature.inspection.TutorialChecklistScreen
import com.seipseip.app.feature.inspection.TutorialScreen
import com.seipseip.app.feature.magazine.MagazineDetailScreen
import com.seipseip.app.feature.magazine.MagazineScreen
import com.seipseip.app.feature.profile.ProfileScreen
import com.seipseip.app.feature.property.PropertyDetailScreen
import com.seipseip.app.feature.property.PropertyInfoScreen
import com.seipseip.app.feature.property.PropertyFormScreen
import com.seipseip.app.feature.property.location.AddressPickerScreen
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
    const val SignupOnboarding = "signup_onboarding"
    const val FirstUse = "first_use"
    const val Welcome = "welcome"
    const val Consent = "consent"
    const val Permissions = "permissions"
    const val Denied = "denied"
    const val Complete = "complete"
    const val Home = "home"
    const val HomeProcessing = "home_processing"
    const val ChecklistOverview = "checklist_overview"
    const val PropertyList = "properties"
    const val PropertyForm = "property_form"
    const val AddressPicker = "address_picker"
    const val PropertyDetail = "property_detail"
    const val PropertyInfo = "property_info"
    const val PropertySelect = "property_select"
    const val PropertyEmpty = "property_empty"
    const val InspectionPrep = "inspection_prep"
    const val InspectionPermissionWarning = "inspection_permission_warning"
    const val InspectionCountdown = "inspection_countdown/{zone}"
    const val Tutorial = "tutorial"
    const val TutorialChecklist = "tutorial_checklist"
    const val GuideZone = "guide/{zone}"
    const val GuideDetail = "guide_detail/{zone}/{item}"
    const val LiveInspection = "live/{zone}/{startedAt}"
    const val FinishConfirm = "finish_confirm/{durationSeconds}"
    const val Analysis = "analysis"
    const val Observation = "observation/{zone}"
    const val Reports = "reports"
    const val ReportDetail = "report_detail"
    const val Profile = "profile"
    const val Magazine = "magazine"
    const val MagazineDetail = "magazine_detail/{articleId}"

    fun guideZone(zone: String) = "guide/$zone"
    fun guideDetail(zone: String, item: Int) = "guide_detail/$zone/$item"
    fun liveInspection(zone: String, startedAt: Long) = "live/$zone/$startedAt"
    fun finishConfirm(durationSeconds: Long) = "finish_confirm/$durationSeconds"
    fun inspectionCountdown(zone: String) = "inspection_countdown/$zone"
    fun observation(zone: String) = "observation/$zone"
    fun magazineDetail(articleId: String) = "magazine_detail/$articleId"
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    nickname: String,
    onNicknameChanged: (String) -> Unit,
) {
    var reportProcessing by remember { mutableStateOf(false) }
    val appContext = LocalContext.current
    LaunchedEffect(reportProcessing) {
        if (reportProcessing) {
            delay(8_000)
            reportProcessing = false
            notifyReportReady(appContext)
        }
    }

    fun goToTab(tab: String) {
        if (navController.currentBackStackEntry?.destination?.route == tab) return

        navController.navigate(tab) {
            launchSingleTop = true
            popUpTo(Route.Home)
        }
    }

    NavHost(
        navController = navController,
        startDestination = Route.Loading,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
            )
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
            )
        },
    ) {
        composable(Route.Loading) {
            LoadingScreen {
                navController.navigate(Route.Login) {
                    popUpTo(Route.Loading) { inclusive = true }
                }
            }
        }
        composable(Route.Login) {
            Login { destination ->
                navController.navigate(when (destination) { "signup" -> Route.SignUp; else -> Route.Welcome })
            }
        }
        composable(Route.SignUp) {
            SignUp(
                back = navController::popBackStack,
                next = { newNickname ->
                    onNicknameChanged(newNickname)
                    navController.navigate(Route.SignupOnboarding)
                },
            )
        }
        composable(Route.SignupOnboarding) {
            Welcome(
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
                processing = reportProcessing,
                onOpenProperties = { navController.navigate(Route.PropertyList) },
                onOpenReports = { navController.navigate(Route.Reports) },
                onOpenRecentReport = { navController.navigate(Route.ReportDetail) },
                onOpenMagazine = { navController.navigate(Route.Magazine) },
                onOpenMagazineArticle = { articleId -> navController.navigate(Route.magazineDetail(articleId)) },
                onStartInspection = { navController.navigate(Route.PropertySelect) },
                onOpenChecklist = { navController.navigate(Route.ChecklistOverview) },
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
        composable(Route.ChecklistOverview) {
            ChecklistOverviewScreen(
                onBack = navController::popBackStack,
                onOpenZone = { zoneId -> navController.navigate(Route.guideZone(zoneId)) },
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
            val selectedAddress by it.savedStateHandle.getStateFlow("addressSummary", "").collectAsState()
            PropertyFormScreen(
                onBack = navController::popBackStack,
                onSaved = { navController.navigate(Route.PropertyList) },
                onOpenAddressPicker = { navController.navigate(Route.AddressPicker) },
                selectedAddress = selectedAddress,
            )
        }
        composable(Route.AddressPicker) {
            AddressPickerScreen(
                onBack = navController::popBackStack,
                onConfirmed = { address ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("addressSummary", address)
                    navController.popBackStack()
                },
            )
        }
        composable(Route.PropertyDetail) {
            PropertyDetailScreen(
                onBack = navController::popBackStack,
                onStartInspection = { navController.navigate(Route.PropertySelect) },
                onOpenReport = { navController.navigate(Route.ReportDetail) },
                onOpenBasicInfo = { navController.navigate(Route.PropertyInfo) },
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
        composable(Route.PropertyInfo) {
            PropertyInfoScreen(onBack = navController::popBackStack)
        }
        composable(Route.PropertySelect) {
            PropertySelectScreen(
                onBack = navController::popBackStack,
                onSelected = { navController.navigate(Route.InspectionPrep) },
                onAddProperty = { navController.navigate(Route.PropertyForm) },
            )
        }
        composable(Route.InspectionPrep) {
            InspectionPrepScreen(
                onBack = navController::popBackStack,
                onStartInspection = { navController.navigate(Route.InspectionPermissionWarning) },
                onSelectProperty = { navController.navigate(Route.PropertySelect) },
            )
        }
        composable(Route.InspectionPermissionWarning) {
            InspectionPermissionWarningScreen(
                onBack = navController::popBackStack,
                onContinue = { navController.navigate(Route.inspectionCountdown("entry")) },
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
                onStart = { navController.navigate(Route.Home) },
            )
        }
        composable(
            route = Route.InspectionCountdown,
            arguments = listOf(navArgument("zone") { type = NavType.StringType }),
        ) {
            val zone = it.arguments?.getString("zone") ?: "entry"
            InspectionCountdownScreen(
                onFinished = {
                    navController.navigate(Route.liveInspection(zone, android.os.SystemClock.elapsedRealtime())) {
                        popUpTo(Route.inspectionCountdown(zone)) { inclusive = true }
                    }
                },
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
            arguments = listOf(
                navArgument("zone") { type = NavType.StringType },
                navArgument("startedAt") { type = NavType.LongType },
            ),
        ) {
            val zone = it.arguments?.getString("zone") ?: "entry"
            val startedAt = it.arguments?.getLong("startedAt") ?: android.os.SystemClock.elapsedRealtime()
            LiveInspectionScreen(
                zoneId = zone,
                startedAt = startedAt,
                onBack = navController::popBackStack,
                onOpenGuide = { item -> navController.navigate(Route.guideDetail(zone, item)) },
                onNextZone = { nextZone -> navController.navigate(Route.liveInspection(nextZone, startedAt)) },
                onFinish = { durationSeconds -> navController.navigate(Route.finishConfirm(durationSeconds)) },
            )
        }
        composable(
            route = Route.FinishConfirm,
            arguments = listOf(navArgument("durationSeconds") { type = NavType.LongType }),
        ) {
            val durationSeconds = it.arguments?.getLong("durationSeconds") ?: 0L
            FinishConfirmScreen(
                onBack = navController::popBackStack,
                durationSeconds = durationSeconds,
                onConfirm = {
                    reportProcessing = true
                    navController.navigate(Route.Analysis)
                },
            )
        }
        composable(Route.Analysis) {
            AnalysisProgressScreen(
                onBackToHome = {
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
                nickname = nickname,
                onBack = navController::popBackStack,
                onOpenProperty = { navController.navigate(Route.PropertyDetail) },
            )
        }
        composable(Route.Profile) {
            ProfileScreen(
                nickname = nickname,
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
                onOpenArticle = { articleId -> navController.navigate(Route.magazineDetail(articleId)) },
            )
        }
        composable(
            route = Route.MagazineDetail,
            arguments = listOf(navArgument("articleId") { type = NavType.StringType }),
        ) {
            val articleId = it.arguments?.getString("articleId") ?: "first_essentials"
            MagazineDetailScreen(articleId = articleId, onBack = navController::popBackStack)
        }
    }
}

private fun notifyReportReady(context: Context) {
    val channelId = "report_ready"
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        manager.createNotificationChannel(NotificationChannel(channelId, "점검 리포트", NotificationManager.IMPORTANCE_DEFAULT))
    }
    if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("점검 리포트가 완성됐어요")
        .setContentText("하자 점검 결과와 리포트를 확인해 보세요.")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .build()
    NotificationManagerCompat.from(context).notify(1001, notification)
}
