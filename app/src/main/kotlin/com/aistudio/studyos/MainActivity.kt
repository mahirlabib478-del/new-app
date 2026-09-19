package com.aistudio.studyos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aistudio.studyos.data.update.UpdateManager
import com.aistudio.studyos.ui.components.ActiveSessionMiniBar
import com.aistudio.studyos.ui.components.UpdateDialog
import com.aistudio.studyos.ui.screens.ExamPlannerScreen
import com.aistudio.studyos.ui.screens.FocusScreen
import com.aistudio.studyos.ui.screens.HomeScreen
import com.aistudio.studyos.ui.screens.ProfileScreen
import com.aistudio.studyos.ui.screens.ProgressScreen
import com.aistudio.studyos.ui.screens.RegularStudyScreen
import com.aistudio.studyos.ui.screens.SavedSessionsScreen
import com.aistudio.studyos.ui.screens.StudyHubScreen
import com.aistudio.studyos.ui.theme.StudyOSTheme
import com.aistudio.studyos.ui.viewmodel.StudyViewModel
import com.aistudio.studyos.ui.viewmodel.StudyViewModelFactory
import kotlinx.coroutines.delay

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object StudyHub : Screen("study_hub", "Study", Icons.Default.School)
    object Progress : Screen("progress", "Progress", Icons.Default.BarChart)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)

    // Full screen sub-destinations
    object Focus : Screen("focus", "Focus Flow")
    object RegularStudy : Screen("regular_study", "Regular Study")
    object ExamPlanner : Screen("exam_planner", "Exam Planner")
    object SavedSessions : Screen("saved_sessions", "Saved Sessions")
}

class MainActivity : ComponentActivity() {
    private val viewModel: StudyViewModel by viewModels {
        StudyViewModelFactory((application as StudyApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themePreset by viewModel.currentTheme.collectAsState()

            StudyOSTheme(preset = themePreset) {
                val navController = rememberNavController()
                MainApp(viewModel = viewModel, navController = navController)
            }
        }
    }
}

private val BOTTOM_NAV_SCREENS = listOf(
    Screen.Home,
    Screen.StudyHub,
    Screen.Progress,
    Screen.Profile
)

private val BOTTOM_NAV_ROUTES = setOf(
    Screen.Home.route,
    Screen.StudyHub.route,
    Screen.Progress.route,
    Screen.Profile.route
)

@Composable
fun MainApp(
    viewModel: StudyViewModel,
    navController: NavHostController
) {
    val context = LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val availableUpdate by viewModel.availableUpdate.collectAsState()
    val (currentVersionName, _) = remember { UpdateManager.getCurrentVersionInfo(context) }

    // Check on every app launch without blocking the UI thread.
    // forceCheck bypasses the 2-hour auto-check throttle so a newly published
    // version can show its update dialog on every fresh app launch.
    LaunchedEffect(Unit) {
        delay(300)
        viewModel.checkAppUpdate(context, isManual = false, forceCheck = true)
    }

    if (availableUpdate != null) {
        UpdateDialog(
            currentVersion = currentVersionName,
            updateInfo = availableUpdate!!,
            onUpdateClick = { targetUrl ->
                UpdateManager.openUpdateLink(context, targetUrl)
            },
            onDismiss = {
                viewModel.dismissUpdateDialog()
            }
        )
    }

    val showBottomBar = currentRoute in BOTTOM_NAV_ROUTES

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                val focusState by viewModel.focusState.collectAsState()
                Column {
                    if (focusState.planId != null) {
                        ActiveSessionMiniBar(
                            focusState = focusState,
                            onOpenFocus = { navController.navigate(Screen.Focus.route) },
                            onToggleTimer = { viewModel.toggleTimer() }
                        )
                    }

                    NavigationBar(
                        modifier = Modifier.testTag("bottom_nav_bar")
                    ) {
                        BOTTOM_NAV_SCREENS.forEach { screen ->
                            val isSelected = currentRoute == screen.route
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    screen.icon?.let {
                                        Icon(imageVector = it, contentDescription = screen.title)
                                    }
                                },
                                label = { Text(screen.title) },
                                modifier = Modifier.testTag("nav_item_${screen.route}")
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            // Bottom-tab navigation should feel immediate; avoid a 150 ms
            // fade on every tap that makes the UI feel slower than it is.
            enterTransition = { null },
            exitTransition = { null },
            popEnterTransition = { null },
            popExitTransition = { null }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onOpenFocus = { navController.navigate(Screen.Focus.route) },
                    onOpenRegularStudy = { navController.navigate(Screen.RegularStudy.route) },
                    onOpenExamPlanner = { navController.navigate(Screen.ExamPlanner.route) },
                    onOpenSavedSessions = { navController.navigate(Screen.SavedSessions.route) }
                )
            }
            composable(Screen.StudyHub.route) {
                StudyHubScreen(
                    viewModel = viewModel,
                    onOpenRegularStudy = { navController.navigate(Screen.RegularStudy.route) },
                    onOpenExamPlanner = { navController.navigate(Screen.ExamPlanner.route) },
                    onOpenSavedSessions = { navController.navigate(Screen.SavedSessions.route) },
                    onOpenFocus = { navController.navigate(Screen.Focus.route) }
                )
            }
            composable(Screen.Progress.route) {
                ProgressScreen(viewModel = viewModel)
            }
            composable(Screen.Profile.route) {
                ProfileScreen(viewModel = viewModel)
            }
            composable(Screen.Focus.route) {
                FocusScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.RegularStudy.route) {
                RegularStudyScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onStartFocus = {
                        navController.navigate(Screen.Focus.route) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                )
            }
            composable(Screen.ExamPlanner.route) {
                ExamPlannerScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onStartFocus = {
                        navController.navigate(Screen.Focus.route) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                )
            }
            composable(Screen.SavedSessions.route) {
                SavedSessionsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onResumeSession = {
                        navController.navigate(Screen.Focus.route) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                )
            }
        }
    }
}
