package com.aistudio.studyos

import android.os.Bundle
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.aistudio.studyos.data.update.UpdateManager
import com.aistudio.studyos.service.StudyReminderScheduler
import com.aistudio.studyos.ui.components.ActiveSessionMiniBar
import com.aistudio.studyos.ui.components.UpdateDialog
import com.aistudio.studyos.ui.screens.ExamPlannerScreen
import com.aistudio.studyos.ui.screens.FocusScreen
import com.aistudio.studyos.ui.screens.HomeScreen
import com.aistudio.studyos.ui.screens.HistoryScreen
import com.aistudio.studyos.ui.screens.ProfileScreen
import com.aistudio.studyos.ui.screens.ProgressScreen
import com.aistudio.studyos.ui.screens.StudyPlanBuilderScreen
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
    object History : Screen("history", "Study History")
    object Profile : Screen("profile", "Profile", Icons.Default.Person)

    // Full screen sub-destinations
    object Focus : Screen("focus", "Focus Flow")
    object RegularStudy : Screen("regular_study", "Regular Study")
    object StudySetup : Screen("study_setup/{mode}/{subject}/{topics}", "Study Plan Setup")
    object ExamPlanner : Screen("exam_planner", "Exam Planner")
    object SavedSessions : Screen("saved_sessions", "Saved Sessions")
}

class MainActivity : ComponentActivity() {
    private val viewModel: StudyViewModel by viewModels {
        StudyViewModelFactory((application as StudyApplication).repository)
    }

    override fun onResume() {
        super.onResume()
        StudyReminderScheduler.rescheduleAfterPermissionGrant(this)
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
        // Let the first frame settle before the network update check competes for startup resources.
        delay(1200)
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
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(200)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(180))
            ) {
                val focusState by viewModel.focusState.collectAsState()
                Column {
                    AnimatedVisibility(
                        visible = focusState.planId != null,
                        enter = fadeIn(animationSpec = tween(180)),
                        exit = fadeOut(animationSpec = tween(180))
                    ) {
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
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            enterTransition = {
                if (targetState.destination.route in BOTTOM_NAV_ROUTES && initialState.destination.route in BOTTOM_NAV_ROUTES) {
                    fadeIn(animationSpec = tween(180))
                } else {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(180))
                }
            },
            exitTransition = {
                if (targetState.destination.route in BOTTOM_NAV_ROUTES && initialState.destination.route in BOTTOM_NAV_ROUTES) {
                    fadeOut(animationSpec = tween(160))
                } else {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(180))
                }
            },
            popEnterTransition = {
                if (targetState.destination.route in BOTTOM_NAV_ROUTES && initialState.destination.route in BOTTOM_NAV_ROUTES) {
                    fadeIn(animationSpec = tween(180))
                } else {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(180))
                }
            },
            popExitTransition = {
                if (targetState.destination.route in BOTTOM_NAV_ROUTES && initialState.destination.route in BOTTOM_NAV_ROUTES) {
                    fadeOut(animationSpec = tween(160))
                } else {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(180))
                }
            }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onOpenFocus = { navController.navigate(Screen.Focus.route) },
                    onOpenStudy = { navController.navigate("study_setup/study/" + Uri.encode("Mathematics") + "/" + Uri.encode("New Topic")) },
                    onOpenQuickFocus = {
                        viewModel.startNewPlan(
                            title = "Quick Focus",
                            subject = "Quick Focus",
                            chapter = "Pomodoro",
                            mode = "quick",
                            totalBlocks = 1,
                            blockMinutes = 25,
                            breakMinutes = 5,
                            autoStart = true,
                            items = listOf(com.aistudio.studyos.data.local.entity.StudyPlanItem("Quick Focus", "Pomodoro", 25)),
                            expectedTotalMinutes = 25,
                            onReady = { navController.navigate(Screen.Focus.route) }
                        )
                    },
                    onOpenExamPlanner = { navController.navigate(Screen.ExamPlanner.route) },
                    onOpenSavedSessions = { navController.navigate(Screen.SavedSessions.route) },
                    onOpenHistory = { navController.navigate(Screen.History.route) }
                )
            }
            composable(Screen.StudyHub.route) {
                StudyHubScreen(
                    viewModel = viewModel,
                    onOpenStudy = { navController.navigate("study_setup/study/" + Uri.encode("Mathematics") + "/" + Uri.encode("New Topic")) },
                    onOpenQuickFocus = {
                        viewModel.startNewPlan(
                            title = "Quick Focus",
                            subject = "Quick Focus",
                            chapter = "Pomodoro",
                            mode = "quick",
                            totalBlocks = 1,
                            blockMinutes = 25,
                            breakMinutes = 5,
                            autoStart = true,
                            items = listOf(com.aistudio.studyos.data.local.entity.StudyPlanItem("Quick Focus", "Pomodoro", 25)),
                            expectedTotalMinutes = 25,
                            onReady = { navController.navigate(Screen.Focus.route) }
                        )
                    },
                    onOpenSavedSessions = { navController.navigate(Screen.SavedSessions.route) },
                    onOpenFocus = { navController.navigate(Screen.Focus.route) }
                )
            }
            composable(Screen.Progress.route) {
                ProgressScreen(viewModel = viewModel, onOpenHistory = { navController.navigate(Screen.History.route) })
            }
            composable(Screen.History.route) {
                HistoryScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
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
            composable(
                route = Screen.StudySetup.route,
                arguments = listOf(
                    navArgument("mode") { type = NavType.StringType },
                    navArgument("subject") { type = NavType.StringType },
                    navArgument("topics") { type = NavType.StringType }
                )
            ) { entry ->
                StudyPlanBuilderScreen(
                    viewModel = viewModel,
                    mode = entry.arguments?.getString("mode").orEmpty(),
                    initialSubject = Uri.decode(entry.arguments?.getString("subject").orEmpty()),
                    initialTopics = Uri.decode(entry.arguments?.getString("topics").orEmpty()),
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
                    onStartStudySetup = { exam ->
                        navController.navigate(
                            "study_setup/study/" + Uri.encode(exam.subject) + "/" +
                                Uri.encode(exam.syllabusTopics.ifBlank { "Core Exam Revision" })
                        )
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
