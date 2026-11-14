package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FamilyRestroom
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.AppLanguage
import com.example.data.local.HayatiDatabase
import com.example.data.local.HayatiRepository
import com.example.data.local.TaskStatus
import com.example.data.local.ThemeMode
import com.example.data.local.UserPreferencesRepository
import com.example.domain.ai.LocalPreviewAiRepository
import com.example.ui.MainViewModel
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EducationScreen
import com.example.ui.screens.FamilyHomeScreen
import com.example.ui.screens.FinanceScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WorkScreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.HayatiTheme
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavyMid
import com.example.ui.theme.TurquoiseBright
import com.example.ui.theme.TurquoisePrimary
import com.example.util.DateTimeUtils
import com.example.util.LocalHayatiStrings
import com.example.util.S
import com.example.util.stringsFor
import kotlinx.coroutines.launch

object HayatiRoutes {
    const val DASHBOARD = "dashboard"
    const val WORK = "work"
    const val FINANCE = "finance"
    const val FAMILY = "family"
    const val EDUCATION = "education"
    const val ASSISTANT = "assistant"
    const val CALENDAR = "calendar"
    const val NOTIFICATIONS = "notifications"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"
}

private data class NavDest(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = applicationContext
            val viewModel: MainViewModel = viewModel(
                factory = MainViewModel.Factory(
                    repo = HayatiRepository(HayatiDatabase.getInstance(context)),
                    prefsRepo = UserPreferencesRepository(context),
                    aiRepo = LocalPreviewAiRepository(),
                )
            )
            HayatiAppRoot(viewModel = viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HayatiAppRoot(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pendingAiAction by viewModel.pendingAiAction.collectAsStateWithLifecycle()
    val aiBusy by viewModel.aiBusy.collectAsStateWithLifecycle()
    val dailyPlanSlots by viewModel.dailyPlanSlots.collectAsStateWithLifecycle()
    val workAiInsight by viewModel.workAiInsight.collectAsStateWithLifecycle()
    val savingTips by viewModel.savingTips.collectAsStateWithLifecycle()
    val tutorResult by viewModel.tutorResult.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    val isAr = state.prefs.language == AppLanguage.ARABIC
    val strings = remember(isAr) { stringsFor(isAr) }
    val darkTheme = when (state.prefs.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides if (isAr) LayoutDirection.Rtl else LayoutDirection.Ltr,
        LocalHayatiStrings provides strings,
    ) {
        HayatiTheme(darkTheme = darkTheme) {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route ?: HayatiRoutes.DASHBOARD
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val scope = rememberCoroutineScope()
            val s = S()

            val primaryDests = remember(s) {
                listOf(
                    NavDest(HayatiRoutes.DASHBOARD, s.navDashboard, Icons.Outlined.Dashboard, Icons.Filled.Dashboard),
                    NavDest(HayatiRoutes.WORK, s.navWork, Icons.Outlined.WorkOutline, Icons.Filled.Work),
                    NavDest(HayatiRoutes.FINANCE, s.navFinance, Icons.Outlined.AccountBalanceWallet, Icons.Filled.AccountBalanceWallet),
                    NavDest(HayatiRoutes.FAMILY, s.navFamily, Icons.Outlined.FamilyRestroom, Icons.Filled.FamilyRestroom),
                    NavDest(HayatiRoutes.EDUCATION, s.navEducation, Icons.Outlined.School, Icons.Filled.School),
                )
            }
            val secondaryDests = remember(s) {
                listOf(
                    NavDest(HayatiRoutes.ASSISTANT, s.navAssistant, Icons.Outlined.AutoAwesome),
                    NavDest(HayatiRoutes.CALENDAR, s.navCalendar, Icons.Outlined.CalendarMonth),
                    NavDest(HayatiRoutes.NOTIFICATIONS, s.navNotifications, Icons.Outlined.Notifications),
                    NavDest(HayatiRoutes.REPORTS, s.navReports, Icons.Outlined.Assessment),
                    NavDest(HayatiRoutes.SETTINGS, s.navSettings, Icons.Outlined.Settings),
                )
            }

            fun navigateTo(route: String) {
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }

            val now = System.currentTimeMillis()
            val alertBadgeCount = remember(state.tasks, state.bills, state.prefs.timeZoneId) {
                val overdue = state.tasks.count {
                    DateTimeUtils.isOverdue(it.dueDateMillis, it.status == TaskStatus.COMPLETED.name, now, state.prefs.timeZoneId)
                }
                val unpaid = state.bills.count { !it.isPaid }
                overdue + unpaid
            }

            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(
                                        Brush.linearGradient(listOf(NavyDeep, NavyMid))
                                    )
                                    .padding(16.dp),
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (isAr) "حياتي" else "HAYATI",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = GoldAccent.copy(alpha = 0.22f),
                                        ) {
                                            Text(
                                                text = "AI",
                                                color = GoldAccent,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = s.tagline,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.85f),
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = state.prefs.userName,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TurquoiseBright,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            (primaryDests + secondaryDests).forEach { dest ->
                                NavigationDrawerItem(
                                    label = { Text(dest.label, fontWeight = if (currentRoute == dest.route) FontWeight.Bold else FontWeight.Medium) },
                                    icon = {
                                        Icon(
                                            if (currentRoute == dest.route) dest.selectedIcon else dest.icon,
                                            contentDescription = null,
                                        )
                                    },
                                    selected = currentRoute == dest.route,
                                    colors = NavigationDrawerItemDefaults.colors(
                                        selectedContainerColor = TurquoisePrimary.copy(alpha = 0.14f),
                                        selectedIconColor = TurquoisePrimary,
                                        selectedTextColor = TurquoisePrimary,
                                    ),
                                    onClick = {
                                        navigateTo(dest.route)
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.testTag("drawer_${dest.route}"),
                                )
                            }
                        }
                    }
                },
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val useRail = maxWidth >= 600.dp
                    Scaffold(
                        contentWindowInsets = WindowInsets.safeDrawing,
                        snackbarHost = {
                            SnackbarHost(
                                hostState = snackbarHostState,
                                modifier = Modifier.padding(bottom = 76.dp),
                            ) { data ->
                                Snackbar(
                                    snackbarData = data,
                                    shape = RoundedCornerShape(14.dp),
                                    containerColor = NavyDeep,
                                    contentColor = Color.White,
                                    actionColor = GoldAccent,
                                )
                            }
                        },
                        topBar = {
                            TopAppBar(
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = NavyDeep,
                                    titleContentColor = Color.White,
                                    navigationIconContentColor = Color.White,
                                    actionIconContentColor = Color.White,
                                ),
                                navigationIcon = {
                                    IconButton(
                                        onClick = { scope.launch { drawerState.open() } },
                                        modifier = Modifier.testTag("open_drawer_btn"),
                                    ) {
                                        Icon(Icons.Outlined.Menu, contentDescription = s.moreSections)
                                    }
                                },
                                title = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (isAr) "حياتي" else "HAYATI",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "AI",
                                            color = GoldAccent,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                },
                                actions = {
                                    IconButton(
                                        onClick = { navigateTo(HayatiRoutes.ASSISTANT) },
                                        modifier = Modifier.testTag("topbar_ai_btn"),
                                    ) {
                                        Icon(Icons.Outlined.AutoAwesome, contentDescription = s.navAssistant, tint = GoldAccent)
                                    }
                                    IconButton(
                                        onClick = { navigateTo(HayatiRoutes.CALENDAR) },
                                        modifier = Modifier.testTag("topbar_calendar_btn"),
                                    ) {
                                        Icon(Icons.Outlined.CalendarMonth, contentDescription = s.navCalendar)
                                    }
                                    IconButton(
                                        onClick = { navigateTo(HayatiRoutes.NOTIFICATIONS) },
                                        modifier = Modifier.testTag("topbar_notifications_btn"),
                                    ) {
                                        BadgedBox(
                                            badge = {
                                                if (alertBadgeCount > 0) {
                                                    Badge { Text(alertBadgeCount.toString()) }
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Outlined.Notifications, contentDescription = s.navNotifications)
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.setThemeMode(
                                                if (darkTheme) ThemeMode.LIGHT else ThemeMode.DARK
                                            )
                                        },
                                        modifier = Modifier.testTag("topbar_theme_toggle"),
                                    ) {
                                        Icon(
                                            imageVector = if (darkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                                            contentDescription = if (isAr) "تبديل المظهر" else "Toggle Theme",
                                        )
                                    }
                                    TextButton(
                                        onClick = {
                                            viewModel.setLanguage(
                                                if (isAr) AppLanguage.ENGLISH else AppLanguage.ARABIC
                                            )
                                        },
                                        modifier = Modifier.testTag("topbar_lang_toggle"),
                                    ) {
                                        Text(if (isAr) "EN" else "AR", color = TurquoiseBright, fontWeight = FontWeight.Bold)
                                    }
                                },
                            )
                        },
                        bottomBar = {
                            if (!useRail) {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 2.dp,
                                    modifier = Modifier.testTag("bottom_nav_bar"),
                                ) {
                                    primaryDests.forEach { dest ->
                                        val selected = currentRoute == dest.route
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { navigateTo(dest.route) },
                                            icon = {
                                                Icon(
                                                    if (selected) dest.selectedIcon else dest.icon,
                                                    contentDescription = dest.label,
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = dest.label,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = TurquoisePrimary,
                                                selectedTextColor = TurquoisePrimary,
                                                indicatorColor = TurquoisePrimary.copy(alpha = 0.14f),
                                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            ),
                                            modifier = Modifier.testTag("bottom_nav_${dest.route}"),
                                        )
                                    }
                                }
                            }
                        },
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                        ) {
                            if (useRail) {
                                NavigationRail(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .verticalScroll(rememberScrollState())
                                        .testTag("navigation_rail"),
                                ) {
                                    (primaryDests + secondaryDests).forEach { dest ->
                                        val selected = currentRoute == dest.route
                                        NavigationRailItem(
                                            selected = selected,
                                            onClick = { navigateTo(dest.route) },
                                            icon = {
                                                Icon(
                                                    if (selected) dest.selectedIcon else dest.icon,
                                                    contentDescription = dest.label,
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = dest.label,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            },
                                            colors = NavigationRailItemDefaults.colors(
                                                selectedIconColor = TurquoisePrimary,
                                                selectedTextColor = TurquoisePrimary,
                                                indicatorColor = TurquoisePrimary.copy(alpha = 0.14f),
                                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            ),
                                            modifier = Modifier.testTag("rail_nav_${dest.route}"),
                                        )
                                    }
                                }
                                VerticalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize(),
                                contentAlignment = Alignment.TopCenter,
                            ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .widthIn(max = 920.dp),
                            ) {
                                if (state.isLoading || state.isSaving) {
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("global_progress_indicator"),
                                        color = TurquoisePrimary,
                                    )
                                }
                                state.lastError?.let { errorMsg ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("global_error_banner"),
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.ErrorOutline,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp),
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = errorMsg,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.weight(1f),
                                            )
                                            IconButton(onClick = { viewModel.clearLastError() }) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Close,
                                                    contentDescription = s.close,
                                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                                )
                                            }
                                        }
                                    }
                                }
                                NavHost(
                                    navController = navController,
                                    startDestination = HayatiRoutes.DASHBOARD,
                                    modifier = Modifier.weight(1f).fillMaxSize(),
                                ) {
                                composable(HayatiRoutes.DASHBOARD) {
                                    DashboardScreen(
                                        state = state,
                                        dailyPlanSlots = dailyPlanSlots,
                                        viewModel = viewModel,
                                        onNavigateToRoute = ::navigateTo,
                                    )
                                }
                                composable(HayatiRoutes.WORK) {
                                    WorkScreen(
                                        state = state,
                                        workAiInsight = workAiInsight,
                                        viewModel = viewModel,
                                    )
                                }
                                composable(HayatiRoutes.FINANCE) {
                                    FinanceScreen(
                                        state = state,
                                        savingTips = savingTips,
                                        viewModel = viewModel,
                                    )
                                }
                                composable(HayatiRoutes.FAMILY) {
                                    FamilyHomeScreen(
                                        state = state,
                                        viewModel = viewModel,
                                    )
                                }
                                composable(HayatiRoutes.EDUCATION) {
                                    EducationScreen(
                                        state = state,
                                        tutorResult = tutorResult,
                                        viewModel = viewModel,
                                    )
                                }
                                composable(HayatiRoutes.ASSISTANT) {
                                    AssistantScreen(
                                        state = state,
                                        pendingAction = pendingAiAction,
                                        aiBusy = aiBusy,
                                        viewModel = viewModel,
                                    )
                                }
                                composable(HayatiRoutes.CALENDAR) {
                                    CalendarScreen(
                                        state = state,
                                        viewModel = viewModel,
                                    )
                                }
                                composable(HayatiRoutes.NOTIFICATIONS) {
                                    NotificationsScreen(
                                        state = state,
                                        viewModel = viewModel,
                                    )
                                }
                                composable(HayatiRoutes.REPORTS) {
                                    ReportsScreen(state = state)
                                }
                                composable(HayatiRoutes.SETTINGS) {
                                    SettingsScreen(
                                        state = state,
                                        viewModel = viewModel,
                                    )
                                }
                            }
                            }
                            }
                        }
                    }
                }
            }
        }
    }
}
