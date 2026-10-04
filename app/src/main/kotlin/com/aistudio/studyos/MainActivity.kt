package com.aistudio.studyos

import android.os.Bundle
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
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
import com.aistudio.studyos.ui.screens.AccountScreen
import com.aistudio.studyos.ui.screens.ResetPasswordScreen
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
    object Account : Screen("account", "Account")
    object ResetPassword : Screen("reset_password", "Reset Password")

    // Full screen sub-destinations
    object Focus : Screen("focus", "Focus Flow")
    object RegularStudy : Screen("regular_study", "Regular Study")
    object StudySetup : Screen("study_setup/{mode}/{subject}/{topics}", "Study Plan Setup")
    object ExamPlanner : Screen("exam_planner", "Exam Planner")
    object SavedSessions : Screen("saved_sessions", "Saved Sessions")
}

class MainActivity : ComponentActivity() {
    override fun onResume() {
        super.onResume()
        StudyReminderScheduler.rescheduleAfterPermissionGrant(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val auth = remember { FirebaseAuth.getInstance() }
            var currentUser by remember { mutableStateOf(auth.currentUser) }
            var showResetPassword by remember { mutableStateOf(false) }
            // Show Welcome on every fresh launch until the user signs in or chooses guest mode.
            // This state intentionally resets when the Activity is recreated for a new app launch.
            var hasStartedWelcome by remember { mutableStateOf(false) }
            var authRefresh by remember { mutableStateOf(0) }
            val guestPrefs = remember { getSharedPreferences("study_os_guest_session", MODE_PRIVATE) }
            var guestMode by remember { mutableStateOf(guestPrefs.getBoolean("guest_mode", false)) }
            val pendingAuthPrefs = remember { getSharedPreferences("study_os_auth_navigation", MODE_PRIVATE) }
            var returnToProfileAfterAuth by remember {
                mutableStateOf(pendingAuthPrefs.getBoolean("return_to_profile", false))
            }

            DisposableEffect(auth) {
                val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                    currentUser = firebaseAuth.currentUser
                    if (firebaseAuth.currentUser?.isEmailVerified == true) {
                        guestPrefs.edit().putBoolean("guest_mode", false).apply()
                        guestMode = false
                    }
                }
                auth.addAuthStateListener(listener)
                onDispose { auth.removeAuthStateListener(listener) }
            }

            val verifiedUid = remember(currentUser, authRefresh) {
                auth.currentUser?.takeIf { it.isEmailVerified }?.uid
            }
            val canContinueAsGuest = guestMode && auth.currentUser == null
            if (verifiedUid != null || canContinueAsGuest) {
                if (verifiedUid != null) {
                    LaunchedEffect(verifiedUid) {
                        (application as StudyApplication).activateCloudSync(verifiedUid)
                    }
                }
                val accountViewModel: StudyViewModel = viewModel(
                    key = if (verifiedUid != null) "study-account-$verifiedUid" else "study-guest",
                    factory = StudyViewModelFactory((application as StudyApplication).repositoryFor(verifiedUid))
                )
                val themePreset by accountViewModel.currentTheme.collectAsState()
                StudyOSTheme(preset = themePreset) {
                    val navController = rememberNavController()
                    MainApp(
                        viewModel = accountViewModel,
                        navController = navController,
                        startDestination = if (returnToProfileAfterAuth) Screen.Profile.route else Screen.Home.route,
                        onStartDestinationConsumed = {
                            if (returnToProfileAfterAuth) {
                                pendingAuthPrefs.edit().putBoolean("return_to_profile", false).apply()
                            }
                        },
                        onAccountVerified = {
                            // Refresh the root auth state and return a guest to Profile after
                            // a successful verified sign-in, rather than leaving them on Account.
                            pendingAuthPrefs.edit().putBoolean("return_to_profile", true).apply()
                            returnToProfileAfterAuth = true
                            guestPrefs.edit().putBoolean("guest_mode", false).apply()
                            guestMode = false
                            currentUser = auth.currentUser
                            authRefresh += 1
                        }
                    )
                }
            } else {
                StudyOSTheme(preset = "pitch_black") {
                    if (!hasStartedWelcome) {
                        WelcomeScreen(onGetStarted = { hasStartedWelcome = true })
                    } else if (showResetPassword) {
                        ResetPasswordScreen(onBack = { showResetPassword = false })
                    } else {
                        AccountScreen(
                            showBackButton = false,
                            onBack = {
                                if (auth.currentUser != null) auth.signOut()
                                guestPrefs.edit().putBoolean("guest_mode", true).apply()
                                guestMode = true
                                showResetPassword = false
                            },
                            onForgotPassword = { showResetPassword = true },
                            onVerified = {
                                currentUser = auth.currentUser
                                authRefresh += 1
                                guestPrefs.edit().putBoolean("guest_mode", false).apply()
                                guestMode = false
                                showResetPassword = false
                            },
                            onContinueAsGuest = {
                                guestPrefs.edit().putBoolean("guest_mode", true).apply()
                                guestMode = true
                                showResetPassword = false
                            }
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun WelcomeScreen(onGetStarted: () -> Unit) {
    val pitchBlack = Color(0xFF000000)
    val green = Color(0xFF00E5FF)
    val softGreen = Color(0xFF80F5FF)
    val mutedText = Color(0xFFA0A0A0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pitchBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Color(0xFF101010))
                    .border(1.dp, Color(0xFF16434A), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = softGreen,
                    modifier = Modifier.size(23.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = "Study OS",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "YOUR PERSONAL STUDY SPACE",
                    color = mutedText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.1.sp
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.7f))

        // A clean, black-on-black focus orb: no grey panel or bulky illustration card.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(238.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(224.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF050B0C))
                    .border(1.dp, Color(0xFF123238), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF080F10))
                        .border(1.dp, Color(0xFF174047), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(132.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0B2024))
                            .border(1.dp, Color(0xFF17606A), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Focus timer",
                            tint = softGreen,
                            modifier = Modifier.size(68.dp)
                        )
                    }
                }
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 32.dp, bottom = 15.dp),
                shape = RoundedCornerShape(50),
                color = Color(0xFF0B2024),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF17606A))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(green)
                    )
                    Text(
                        text = "FOCUS MODE",
                        color = softGreen,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.7.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Make Every",
            color = Color.White,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Minute Count.",
            color = green,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Focus better. Build habits.\nReach your goals — one session at a time.",
            color = mutedText,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onGetStarted,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = green,
                contentColor = Color.Black
            )
        ) {
            Text(
                text = "Get Started",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.size(10.dp))
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(19.dp)
            )
        }

        Spacer(modifier = Modifier.height(13.dp))
        Text(
            text = "YOUR PERSONAL SPACE TO GROW",
            color = Color(0xFF707070),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            letterSpacing = 1.05.sp
        )
        Spacer(modifier = Modifier.height(7.dp))
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
    navController: NavHostController,
    startDestination: String = Screen.Home.route,
    onStartDestinationConsumed: () -> Unit = {},
    onAccountVerified: () -> Unit = {}
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
        viewModel.refreshTodayStats()
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

    var isNavigatingToFocus by remember { mutableStateOf(false) }

    LaunchedEffect(currentRoute) {
        if (currentRoute in BOTTOM_NAV_ROUTES) {
            isNavigatingToFocus = false
        }
    }

    val isAtBottomNav = currentRoute in BOTTOM_NAV_ROUTES
    val showBottomBar = !isNavigatingToFocus && (currentRoute == null || isAtBottomNav)
    val focusState by viewModel.focusState.collectAsState()
    val showMiniBar = !isNavigatingToFocus && isAtBottomNav && currentRoute != Screen.Focus.route && focusState.planId != null

    LaunchedEffect(startDestination) {
        if (startDestination == Screen.Profile.route) {
            onStartDestinationConsumed()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing)),
                exit = fadeOut(animationSpec = tween(140, easing = FastOutSlowInEasing))
            ) {
                Column {
                    if (showMiniBar) {
                        ActiveSessionMiniBar(
                            focusState = focusState,
                            onOpenFocus = {
                                isNavigatingToFocus = true
                                navController.navigate(Screen.Focus.route)
                            },
                            onToggleTimer = { viewModel.toggleTimer() }
                        )
                    }

                    NavigationBar(
                        modifier = Modifier.testTag("bottom_nav_bar"),
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.onBackground,
                        tonalElevation = 0.dp
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
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            enterTransition = {
                fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(160, easing = FastOutSlowInEasing))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(160, easing = FastOutSlowInEasing))
            }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onOpenFocus = {
                        isNavigatingToFocus = true
                        navController.navigate(Screen.Focus.route)
                    },
                    onOpenStudy = { navController.navigate("study_setup/study/" + Uri.encode("Mathematics") + "/" + Uri.encode("New Topic")) },
                    onOpenQuickFocus = {
                        isNavigatingToFocus = true
                        navController.navigate(Screen.Focus.route) {
                            launchSingleTop = true
                        }
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
                            expectedTotalMinutes = 25
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
                        isNavigatingToFocus = true
                        navController.navigate(Screen.Focus.route) {
                            launchSingleTop = true
                        }
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
                            expectedTotalMinutes = 25
                        )
                    },
                    onOpenSavedSessions = { navController.navigate(Screen.SavedSessions.route) },
                    onOpenFocus = {
                        isNavigatingToFocus = true
                        navController.navigate(Screen.Focus.route)
                    }
                )
            }
            composable(Screen.Progress.route) {
                ProgressScreen(viewModel = viewModel, onOpenHistory = { navController.navigate(Screen.History.route) })
            }
            composable(
                route = Screen.History.route,
                enterTransition = {
                    fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(150, easing = FastOutSlowInEasing))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(150, easing = FastOutSlowInEasing))
                }
            ) {
                HistoryScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
            composable(Screen.Profile.route) {
                ProfileScreen(viewModel = viewModel, onOpenAccount = { navController.navigate(Screen.Account.route) })
            }
            composable(Screen.Account.route) {
                AccountScreen(
                    onBack = { navController.popBackStack() },
                    onForgotPassword = { navController.navigate(Screen.ResetPassword.route) },
                    onVerified = {
                        onAccountVerified()
                        // This NavHost is already running in guest mode, so changing its
                        // startDestination alone does not move its existing back stack.
                        navController.navigate(Screen.Profile.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onContinueAsGuest = {},
                    showBackButton = true,
                    showContinueAsGuest = false
                )
            }
            composable(Screen.ResetPassword.route) {
                ResetPasswordScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Screen.Focus.route,
                enterTransition = {
                    fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(150, easing = FastOutSlowInEasing))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(150, easing = FastOutSlowInEasing))
                }
            ) {
                DisposableEffect(Unit) {
                    onDispose {
                        isNavigatingToFocus = false
                    }
                }
                FocusScreen(
                    viewModel = viewModel,
                    onBack = {
                        isNavigatingToFocus = false
                        navController.popBackStack()
                    }
                )
            }
            composable(
                route = Screen.StudySetup.route,
                arguments = listOf(
                    navArgument("mode") { type = NavType.StringType },
                    navArgument("subject") { type = NavType.StringType },
                    navArgument("topics") { type = NavType.StringType }
                ),
                enterTransition = {
                    fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(150, easing = FastOutSlowInEasing))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(150, easing = FastOutSlowInEasing))
                }
            ) { entry ->
                StudyPlanBuilderScreen(
                    viewModel = viewModel,
                    mode = entry.arguments?.getString("mode").orEmpty(),
                    initialSubject = Uri.decode(entry.arguments?.getString("subject").orEmpty()),
                    initialTopics = Uri.decode(entry.arguments?.getString("topics").orEmpty()),
                    onBack = { navController.popBackStack() },
                    onStartFocus = {
                        isNavigatingToFocus = true
                        navController.navigate(Screen.Focus.route) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                )
            }
            composable(
                route = Screen.ExamPlanner.route,
                enterTransition = {
                    fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(150, easing = FastOutSlowInEasing))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(150, easing = FastOutSlowInEasing))
                }
            ) {
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
            composable(
                route = Screen.SavedSessions.route,
                enterTransition = {
                    fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(150, easing = FastOutSlowInEasing))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(150, easing = FastOutSlowInEasing))
                }
            ) {
                SavedSessionsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onResumeSession = {
                        isNavigatingToFocus = true
                        navController.navigate(Screen.Focus.route) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                )
            }
        }
    }
}
