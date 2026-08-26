package com.seipseip.app.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.seipseip.app.feature.common.AppTab
import com.seipseip.app.feature.common.AppBottomNavigation
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
import com.seipseip.app.feature.inspection.voice.VoiceSummaryScreen
import com.seipseip.app.feature.magazine.MagazineDetailScreen
import com.seipseip.app.feature.magazine.MagazineScreen
import com.seipseip.app.feature.profile.ProfileScreen
import com.seipseip.app.feature.property.PropertyDetailScreen
import com.seipseip.app.feature.property.PropertyInfoScreen
import com.seipseip.app.feature.property.PropertyFormScreen
import com.seipseip.app.feature.property.location.AddressPickerScreen
import com.seipseip.app.feature.property.location.LocationPickerActivity
import com.seipseip.app.feature.property.PropertyListScreen
import com.seipseip.app.feature.property.PropertySelectScreen
import com.seipseip.app.feature.state.EmptyPropertyScreen
import com.seipseip.app.feature.state.HomeProcessingScreen
import com.seipseip.app.feature.state.LoadingScreen
import com.seipseip.app.integration.InspectionFinishApiRoute
import com.seipseip.app.integration.InspectionPrepApiRoute
import com.seipseip.app.integration.LiveInspectionApiRoute
import com.seipseip.app.integration.MediaUploadApiRoute
import com.seipseip.app.integration.PropertyDetailApiRoute
import com.seipseip.app.integration.PropertyFormApiRoute
import com.seipseip.app.integration.PropertyInfoApiRoute
import com.seipseip.app.integration.PropertyListApiRoute
import com.seipseip.app.integration.PropertyMapApiRoute
import com.seipseip.app.integration.PropertySelectApiRoute
import com.seipseip.app.integration.ReportApiRoute
import com.seipseip.app.integration.ReportListApiUiState
import com.seipseip.app.integration.ReportListApiRoute
import com.seipseip.app.integration.ReportListApiViewModel
import com.seipseip.app.integration.latestReportItem

object Route {
    const val Loading = "loading"
    const val Login = "login"
    const val SignUp = "signup"
    const val SignupOnboarding = "signup_onboarding"
    const val SignupConsent = "signup_consent"
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
    const val PropertyMap = "property_map"
    const val PropertyForm = "property_form"
    const val AddressPicker = "address_picker"
    const val PropertyDetail = "property_detail/{propertyId}"
    const val PropertyEdit = "property_edit/{propertyId}"
    const val PropertyInfo = "property_info/{propertyId}"
    const val VoiceSummary = "voice_summary/{propertyId}"
    const val PropertySelect = "property_select"
    const val PropertyEmpty = "property_empty"
    const val InspectionPrep = "inspection_prep/{propertyId}"
    const val InspectionPermissionWarning = "inspection_permission_warning/{inspectionId}"
    const val InspectionCountdown = "inspection_countdown/{inspectionId}/{zone}"
    const val Tutorial = "tutorial"
    const val TutorialChecklist = "tutorial_checklist"
    const val GuideZone = "guide/{zone}"
    const val GuideDetail = "guide_detail/{zone}/{item}"
    const val LiveInspection = "live/{inspectionId}/{zone}/{startedAt}"
    const val FinishConfirm = "finish_confirm/{inspectionId}/{durationSeconds}"
    const val Analysis = "analysis/{inspectionId}"
    const val Observation = "observation/{zone}"
    const val Reports = "reports"
    const val InspectionReport = "inspection_report/{inspectionId}"
    const val Profile = "profile"
    const val Magazine = "magazine"
    const val MagazineDetail = "magazine_detail/{articleId}"

    fun guideZone(zone: String) = "guide/$zone"
    fun guideDetail(zone: String, item: Int) = "guide_detail/$zone/$item"
    fun propertyDetail(propertyId: String) = "property_detail/$propertyId"
    fun propertyEdit(propertyId: String) = "property_edit/$propertyId"
    fun propertyInfo(propertyId: String) = "property_info/$propertyId"
    fun voiceSummary(propertyId: String) = "voice_summary/$propertyId"
    fun inspectionPrep(propertyId: String) = "inspection_prep/$propertyId"
    fun inspectionPermission(inspectionId: String) = "inspection_permission_warning/$inspectionId"
    fun liveInspection(inspectionId: String, zone: String, startedAt: Long) = "live/$inspectionId/$zone/$startedAt"
    fun finishConfirm(inspectionId: String, durationSeconds: Long) = "finish_confirm/$inspectionId/$durationSeconds"
    fun inspectionCountdown(inspectionId: String, zone: String) = "inspection_countdown/$inspectionId/$zone"
    fun analysis(inspectionId: String) = "analysis/$inspectionId"
    fun inspectionReport(inspectionId: String) = "inspection_report/$inspectionId"
    fun observation(zone: String) = "observation/$zone"
    fun magazineDetail(articleId: String) = "magazine_detail/$articleId"
}

private const val SESSION_PREFERENCES = "tenant_leaf_session"
private const val KEY_LOGGED_IN = "logged_in"
private const val KEY_TUTORIAL_COMPLETED = "tutorial_completed"
private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"

internal fun initialRouteFor(
    isLoggedIn: Boolean,
    isTutorialCompleted: Boolean,
    isOnboardingCompleted: Boolean,
): String = when {
    !isOnboardingCompleted -> Route.Welcome
    isTutorialCompleted && isLoggedIn -> Route.Home
    isLoggedIn -> Route.Consent
    else -> Route.Login
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    nickname: String,
    onNicknameChanged: (String) -> Unit,
) {
    val appContext = LocalContext.current
    val sessionPreferences = remember(appContext) {
        appContext.getSharedPreferences(SESSION_PREFERENCES, Context.MODE_PRIVATE)
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
                val isLoggedIn = sessionPreferences.getBoolean(KEY_LOGGED_IN, false)
                val isTutorialCompleted = sessionPreferences.getBoolean(KEY_TUTORIAL_COMPLETED, false)
                val isOnboardingCompleted = sessionPreferences.getBoolean(KEY_ONBOARDING_COMPLETED, false)
                navController.navigate(initialRouteFor(isLoggedIn, isTutorialCompleted, isOnboardingCompleted)) {
                    popUpTo(Route.Loading) { inclusive = true }
                }
            }
        }
        composable(Route.Login) {
            Login { destination ->
                if (destination == "signup") {
                    navController.navigate(Route.SignUp)
                } else {
                    if (destination == "guest") {
                        onNicknameChanged("게스트")
                    }
                    sessionPreferences.edit().putBoolean(KEY_LOGGED_IN, true).apply()
                    navController.navigate(Route.Consent)
                }
            }
        }
        composable(Route.SignUp) {
            SignUp(
                back = navController::popBackStack,
                next = { newNickname ->
                    onNicknameChanged(newNickname)
                    navController.navigate(Route.SignupConsent)
                },
            )
        }
        composable(Route.SignupConsent) {
            Consent(
                back = navController::popBackStack,
                next = {
                    sessionPreferences.edit().putBoolean(KEY_LOGGED_IN, true).apply()
                    navController.navigate(Route.FirstUse)
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
                next = { navController.navigate(Route.Permissions) },
            )
        }
        composable(Route.Welcome) {
            Welcome(
                back = navController::popBackStack,
                next = {
                    sessionPreferences.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()
                    navController.navigate(Route.Login) {
                        popUpTo(Route.Welcome) { inclusive = true }
                    }
                },
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
            MainTabsPagerScreen(navController = navController, initialTab = AppTab.Home, nickname = nickname, sessionPreferences = sessionPreferences)
        }
        composable(Route.ChecklistOverview) {
            ChecklistOverviewScreen(
                onBack = navController::popBackStack,
                onOpenZone = { zoneId -> navController.navigate(Route.guideZone(zoneId)) },
            )
        }
        composable(Route.HomeProcessing) {
            HomeProcessingScreen(
                onAddProperty = { navController.navigate(Route.PropertyForm) },
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
            MainTabsPagerScreen(navController = navController, initialTab = AppTab.Property, nickname = nickname, sessionPreferences = sessionPreferences)
        }
        composable(Route.PropertyMap) {
            PropertyMapApiRoute(
                onBack = navController::popBackStack,
                onOpenProperty = { navController.navigate(Route.propertyDetail(it)) },
                onAddProperty = { navController.navigate(Route.PropertyForm) },
            )
        }
        composable(Route.PropertyForm) { entry ->
            val context = LocalContext.current
            val selectedAddress by entry.savedStateHandle.getStateFlow("addressSummary", "").collectAsState()
            val locationPicker = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult(),
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.getStringExtra(LocationPickerActivity.EXTRA_ADDRESS)?.let { address ->
                        entry.savedStateHandle["addressSummary"] = address
                    }
                }
            }
            PropertyFormApiRoute(
                onBack = navController::popBackStack,
                onSaved = { propertyId -> navController.navigate(Route.propertyDetail(propertyId)) },
                onOpenAddressPicker = { navController.navigate(Route.AddressPicker) },
                onOpenLocationPicker = {
                    locationPicker.launch(Intent(context, LocationPickerActivity::class.java))
                },
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
        composable(
            route = Route.PropertyEdit,
            arguments = listOf(navArgument("propertyId") { type = NavType.StringType }),
        ) { entry ->
            val context = LocalContext.current
            val selectedAddress by entry.savedStateHandle.getStateFlow("addressSummary", "").collectAsState()
            val locationPicker = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult(),
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.getStringExtra(LocationPickerActivity.EXTRA_ADDRESS)?.let { address ->
                        entry.savedStateHandle["addressSummary"] = address
                    }
                }
            }
            PropertyFormApiRoute(
                onBack = navController::popBackStack,
                onSaved = { _ -> navController.popBackStack() },
                onOpenAddressPicker = { navController.navigate(Route.AddressPicker) },
                onOpenLocationPicker = {
                    locationPicker.launch(Intent(context, LocationPickerActivity::class.java))
                },
                selectedAddress = selectedAddress,
            )
        }
        composable(
            route = Route.PropertyDetail,
            arguments = listOf(navArgument("propertyId") { type = NavType.StringType }),
        ) {
            PropertyDetailApiRoute(
                onBack = navController::popBackStack,
                onStartInspection = { navController.navigate(Route.inspectionPrep(it)) },
                onOpenReport = { navController.navigate(Route.Reports) },
                onOpenBasicInfo = { property -> property?.id?.let { navController.navigate(Route.propertyInfo(it)) } },
                onEditProperty = { propertyId -> navController.navigate(Route.propertyEdit(propertyId)) },
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
        composable(
            route = Route.PropertyInfo,
            arguments = listOf(navArgument("propertyId") { type = NavType.StringType }),
        ) {
            PropertyInfoApiRoute(onBack = navController::popBackStack)
        }
        composable(
            route = Route.VoiceSummary,
            arguments = listOf(navArgument("propertyId") { type = NavType.StringType }),
        ) { entry ->
            VoiceSummaryScreen(
                propertyId = entry.arguments?.getString("propertyId").orEmpty(),
                onBack = navController::popBackStack,
            )
        }
        composable(Route.PropertySelect) {
            PropertySelectApiRoute(
                onBack = navController::popBackStack,
                onSelected = { navController.navigate(Route.inspectionPrep(it)) },
                onAddProperty = { navController.navigate(Route.PropertyForm) },
            )
        }
        composable(
            route = Route.InspectionPrep,
            arguments = listOf(navArgument("propertyId") { type = NavType.StringType }),
        ) {
            InspectionPrepApiRoute(
                onBack = navController::popBackStack,
                onCreated = { navController.navigate(Route.inspectionPermission(it)) },
                onSelectProperty = { navController.navigate(Route.PropertySelect) },
            )
        }
        composable(
            route = Route.InspectionPermissionWarning,
            arguments = listOf(navArgument("inspectionId") { type = NavType.StringType }),
        ) {
            val inspectionId = it.arguments?.getString("inspectionId") ?: return@composable
            InspectionPermissionWarningScreen(
                onBack = navController::popBackStack,
                onContinue = { navController.navigate(Route.inspectionCountdown(inspectionId, "entry")) },
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
                onOpenGuide = {
                    sessionPreferences.edit().putBoolean(KEY_TUTORIAL_COMPLETED, true).apply()
                    navController.navigate(Route.guideZone("entry"))
                },
                onStart = {
                    sessionPreferences.edit().putBoolean(KEY_TUTORIAL_COMPLETED, true).apply()
                    navController.navigate(Route.Home)
                },
            )
        }
        composable(
            route = Route.InspectionCountdown,
            arguments = listOf(
                navArgument("inspectionId") { type = NavType.StringType },
                navArgument("zone") { type = NavType.StringType },
            ),
        ) {
            val inspectionId = it.arguments?.getString("inspectionId") ?: return@composable
            val zone = it.arguments?.getString("zone") ?: "entry"
            InspectionCountdownScreen(
                onFinished = {
                    navController.navigate(Route.liveInspection(inspectionId, zone, android.os.SystemClock.elapsedRealtime())) {
                        popUpTo(Route.inspectionCountdown(inspectionId, zone)) { inclusive = true }
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
                navArgument("inspectionId") { type = NavType.StringType },
                navArgument("zone") { type = NavType.StringType },
                navArgument("startedAt") { type = NavType.LongType },
            ),
        ) {
            val inspectionId = it.arguments?.getString("inspectionId") ?: return@composable
            val zone = it.arguments?.getString("zone") ?: "entry"
            val startedAt = it.arguments?.getLong("startedAt") ?: android.os.SystemClock.elapsedRealtime()
            LiveInspectionApiRoute(
                inspectionId = inspectionId,
                zoneId = zone,
                startedAt = startedAt,
                onCancelled = { navController.navigate(Route.Home) { popUpTo(Route.Home) { inclusive = true } } },
                onOpenGuide = { item -> navController.navigate(Route.guideDetail(zone, item)) },
                onNextZone = { nextZone -> navController.navigate(Route.liveInspection(inspectionId, nextZone, startedAt)) },
                onFinish = { durationSeconds -> navController.navigate(Route.finishConfirm(inspectionId, durationSeconds)) },
            )
        }
        composable(
            route = Route.FinishConfirm,
            arguments = listOf(
                navArgument("inspectionId") { type = NavType.StringType },
                navArgument("durationSeconds") { type = NavType.LongType },
            ),
        ) {
            val inspectionId = it.arguments?.getString("inspectionId") ?: return@composable
            val durationSeconds = it.arguments?.getLong("durationSeconds") ?: 0L
            InspectionFinishApiRoute(
                onBack = navController::popBackStack,
                durationSeconds = durationSeconds,
                onEnded = {
                    navController.navigate(Route.analysis(inspectionId))
                },
            )
        }
        composable(
            route = Route.Analysis,
            arguments = listOf(navArgument("inspectionId") { type = NavType.StringType }),
        ) {
            val inspectionId = it.arguments?.getString("inspectionId") ?: return@composable
            MediaUploadApiRoute(
                onBackToHome = {
                    navController.navigate(Route.Home) {
                        popUpTo(Route.Home) { inclusive = true }
                    }
                },
                onOpenReport = { navController.navigate(Route.inspectionReport(inspectionId)) },
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
                onReturnToProperty = {
                    if (!navController.popBackStack(Route.PropertyDetail, inclusive = false)) {
                        navController.navigate(Route.PropertyList)
                    }
                },
            )
        }
        composable(Route.Reports) {
            MainTabsPagerScreen(navController = navController, initialTab = AppTab.Report, nickname = nickname, sessionPreferences = sessionPreferences)
        }
        composable(
            route = Route.InspectionReport,
            arguments = listOf(navArgument("inspectionId") { type = NavType.StringType }),
        ) {
            ReportApiRoute(
                nickname = nickname,
                onBack = navController::popBackStack,
                onOpenProperty = { propertyId -> navController.navigate(Route.propertyDetail(propertyId)) },
                onOpenVoiceRecord = { propertyId -> navController.navigate(Route.voiceSummary(propertyId)) },
            )
        }
        composable(Route.Profile) {
            MainTabsPagerScreen(navController = navController, initialTab = AppTab.Profile, nickname = nickname, sessionPreferences = sessionPreferences)
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

@Composable
private fun MainTabsPagerScreen(
    navController: NavHostController,
    initialTab: AppTab = AppTab.Home,
    nickname: String,
    sessionPreferences: android.content.SharedPreferences,
) {
    val initialPage = when (initialTab) {
        AppTab.Home -> 0
        AppTab.Property -> 1
        AppTab.Report -> 2
        AppTab.Profile -> 3
    }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()

    val currentTab = when (pagerState.currentPage) {
        0 -> AppTab.Home
        1 -> AppTab.Property
        2 -> AppTab.Report
        else -> AppTab.Profile
    }

    Scaffold(
        bottomBar = {
            AppBottomNavigation(
                selectedTab = currentTab,
                onTabSelected = { tab ->
                    val page = when (tab) {
                        AppTab.Home -> 0
                        AppTab.Property -> 1
                        AppTab.Report -> 2
                        AppTab.Profile -> 3
                    }
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(page)
                    }
                },
            )
        },
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
        ) { page ->
            when (page) {
                0 -> {
                    val reportListViewModel: ReportListApiViewModel = hiltViewModel()
                    val reportListState by reportListViewModel.state.collectAsState()
                    LaunchedEffect(Unit) { reportListViewModel.refresh() }
                    val recentReport = (reportListState as? ReportListApiUiState.Ready)
                        ?.items
                        ?.let(::latestReportItem)
                    HomeScreen(
                        processing = false,
                        onAddProperty = { navController.navigate(Route.PropertyForm) },
                        onOpenReports = { navController.navigate(Route.Reports) },
                        onOpenRecentReport = {
                            recentReport?.inspectionId?.let { navController.navigate(Route.inspectionReport(it)) }
                                ?: navController.navigate(Route.Reports)
                        },
                        recentReportTitle = recentReport?.propertyName,
                        recentReportDate = recentReport?.dateLabel,
                        onOpenMagazine = { navController.navigate(Route.Magazine) },
                        onOpenMagazineArticle = { articleId -> navController.navigate(Route.magazineDetail(articleId)) },
                        onStartInspection = { navController.navigate(Route.PropertySelect) },
                        onOpenChecklist = { navController.navigate(Route.ChecklistOverview) },
                        onTabSelected = {},
                        showBottomBar = false,
                    )
                }
                1 -> {
                    PropertyListApiRoute(
                        onAddProperty = { navController.navigate(Route.PropertyForm) },
                        onOpenProperty = { navController.navigate(Route.propertyDetail(it)) },
                        onOpenMapOverview = { navController.navigate(Route.PropertyMap) },
                        onTabSelected = {},
                        showBottomBar = false,
                    )
                }
                2 -> {
                    ReportListApiRoute(
                        onOpenReport = { inspectionId -> navController.navigate(Route.inspectionReport(inspectionId)) },
                        onTabSelected = {},
                        showBottomBar = false,
                    )
                }
                3 -> {
                    ProfileScreen(
                        nickname = nickname,
                        onLogout = {
                            sessionPreferences.edit().putBoolean(KEY_LOGGED_IN, false).apply()
                            navController.navigate(Route.Login) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onTabSelected = {},
                        showBottomBar = false,
                    )
                }
            }
        }
    }
}
