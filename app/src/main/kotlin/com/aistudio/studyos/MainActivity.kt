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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
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
import com.aistudio.studyos.ui.components.tactile3DButton
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
            var guestImportPendingUid by remember { mutableStateOf<String?>(null) }
            var guestImportBusy by remember { mutableStateOf(false) }
            var guestImportError by remember { mutableStateOf<String?>(null) }
            val guestImportScope = rememberCoroutineScope()

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
                    val uid = verifiedUid
                    LaunchedEffect(uid) {
                        val app = application as StudyApplication
                        try {
                            val shouldOffer = withContext(Dispatchers.IO) {
                                app.shouldOfferGuestImport(uid)
                            }
                            if (shouldOffer) {
                                guestImportError = null
                                guestImportPendingUid = uid
                            } else {
                                app.activateCloudSync(uid)
                            }
                        } catch (error: Exception) {
                            // A preflight failure must never block the account. Continue with
                            // normal cloud bootstrap; it will fail closed if reconciliation is needed.
                            app.activateCloudSync(uid)
                        }
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

                    if (guestImportPendingUid == uid) {
                        AlertDialog(
                            onDismissRequest = {
                                if (!guestImportBusy) {
                                    guestImportPendingUid = null
                                    (application as StudyApplication).activateCloudSync(uid)
                                }
                            },
                            title = { Text("Bring your guest progress?") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        "This device has study progress from Guest mode. You can import it into this verified account and sync it to the cloud."
                                    )
                                    Text(
                                        "Plans, exams, focus history, XP, level, streak, daily target, and saved StudyOS settings will be imported. Existing cloud account progress will never be overwritten.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    guestImportError?.let {
                                        Text(
                                            it,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    enabled = !guestImportBusy,
                                    onClick = {
                                        guestImportBusy = true
                                        guestImportError = null
                                        guestImportScope.launch {
                                            try {
                                                val result = withContext(Dispatchers.IO) {
                                                    (application as StudyApplication).importGuestProgressToAccount(uid)
                                                }
                                                guestImportPendingUid = null
                                                (application as StudyApplication).activateCloudSync(uid)
                                            } catch (error: Exception) {
                                                guestImportError = error.localizedMessage
                                                    ?: "Guest progress could not be imported. Nothing was intentionally deleted."
                                                // Keep normal cloud sync gated only by the migration
                                                // dialog; a failed import remains retryable.
                                            } finally {
                                                guestImportBusy = false
                                            }
                                        }
                                    }
                                ) {
                                    Text(if (guestImportBusy) "Importing…" else "Import & Sync", maxLines = 1, softWrap = false)
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    enabled = !guestImportBusy,
                                    onClick = {
                                        guestImportPendingUid = null
                                        guestImportError = null
                                        (application as StudyApplication).activateCloudSync(uid)
                                    }
                                ) {
                                    Text("Keep Guest Data", maxLines = 1, softWrap = false)
                                }
                            }
                        )
                    }
                }
            } else {
                val systemIsDark = isSystemInDarkTheme()
                StudyOSTheme(preset = if (systemIsDark) "pitch_black" else "sunrise") {
                    if (!hasStartedWelcome) {
                        WelcomeScreen(
                            isDark = systemIsDark,
                            onGetStarted = { hasStartedWelcome = true }
                        )
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
private fun WelcomeScreen(isDark: Boolean, onGetStarted: () -> Unit) {
    val background = if (isDark) Color(0xFF000000) else Color(0xFFF6F1EA)
    val accent = if (isDark) Color(0xFF00E5FF) else Color(0xFFC2410C)
    val accentSoft = if (isDark) Color(0xFF80F5FF) else Color(0xFF9A3412)
    val mutedText = if (isDark) Color(0xFFA0A0A0) else Color(0xFF57534E)
    val primaryText = if (isDark) Color.White else Color(0xFF292524)
    val outerOrb = if (isDark) Color(0xFF050B0C) else Color(0xFFFFE8D1)
    val middleOrb = if (isDark) Color(0xFF080F10) else Color(0xFFFFF0E0)
    val innerOrb = if (isDark) Color(0xFF0B2024) else Color(0xFFFED7AA)
    val outerBorder = if (isDark) Color(0xFF123238) else Color(0xFFE0C4A8)
    val middleBorder = if (isDark) Color(0xFF174047) else Color(0xFFE9B98F)
    val innerBorder = if (isDark) Color(0xFF17606A) else Color(0xFFEA580C)
    val logoSurface = if (isDark) Color(0xFF101010) else Color(0xFFFFF7F1)
    val badgeSurface = if (isDark) Color(0xFF0B2024) else Color(0xFFFFEDD5)
    val badgeBorder = if (isDark) Color(0xFF17606A) else Color(0xFFE0C4A8)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp)
                    .tactile3DButton(logoSurface, outerBorder.copy(alpha = 0.55f), 13.dp, 3.dp)
                    .background(logoSurface)
                    .border(1.dp, outerBorder, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.School, contentDescription = null, tint = accentSoft, modifier = Modifier.size(23.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("Study OS", color = primaryText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "YOUR PERSONAL STUDY SPACE",
                    color = mutedText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.1.sp
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.7f))
        Box(modifier = Modifier.fillMaxWidth().height(238.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier.size(224.dp)
                    .tactile3DButton(outerOrb, outerBorder.copy(alpha = 0.48f), 112.dp, 7.dp)
                    .clip(CircleShape)
                    .background(outerOrb)
                    .border(1.dp, outerBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(180.dp)
                        .tactile3DButton(middleOrb, middleBorder.copy(alpha = 0.42f), 90.dp, 5.dp)
                        .clip(CircleShape)
                        .background(middleOrb)
                        .border(1.dp, middleBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.size(132.dp)
                            .tactile3DButton(innerOrb, innerBorder.copy(alpha = 0.38f), 66.dp, 4.dp)
                            .clip(CircleShape)
                            .background(innerOrb)
                            .border(1.dp, innerBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = "Focus timer", tint = accentSoft, modifier = Modifier.size(68.dp))
                    }
                }
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 32.dp, bottom = 15.dp)
                    .tactile3DButton(badgeSurface, badgeBorder.copy(alpha = 0.55f), 50.dp, 3.dp),
                shape = RoundedCornerShape(50),
                color = badgeSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, badgeBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(accent))
                    Text("FOCUS MODE", color = accentSoft, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text("Make Every", color = primaryText, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Text("Minute Count.", color = accent, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Focus better. Build habits.\nReach your goals — one session at a time.",
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
                .height(58.dp)
                .tactile3DButton(
                    backgroundColor = accent,
                    bottomEdgeColor = accent.copy(alpha = 0.55f),
                    cornerRadius = 18.dp,
                    depth = 5.dp
                ),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accent,
                contentColor = if (isDark) Color.Black else Color.White
            )
        ) {
            Text("Get Started", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.size(10.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(19.dp))
        }

        Spacer(modifier = Modifier.height(13.dp))
        Text("YOUR PERSONAL SPACE TO GROW", color = mutedText, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, letterSpacing = 1.05.sp)
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
    val showMiniBar = !isNavigatingToFocus && isAtBottomNav && currentRoute != Screen.Focus.route && currentRoute != Screen.StudyHub.route && focusState.planId != null

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
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .tactile3DButton(
                                backgroundColor = MaterialTheme.colorScheme.surface,
                                bottomEdgeColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                                cornerRadius = 28.dp,
                                depth = 5.dp
                            )
                            .testTag("bottom_nav_bar"),
                        containerColor = Color.Transparent,
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
                                    val iconScale by animateFloatAsState(
                                        targetValue = if (isSelected) 1.12f else 1f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        ),
                                        label = "bottom_nav_icon_scale"
                                    )
                                    screen.icon?.let {
                                        Icon(
                                            imageVector = it,
                                            contentDescription = screen.title,
                                            modifier = Modifier.graphicsLayer {
                                                scaleX = iconScale
                                                scaleY = iconScale
                                                translationY = if (isSelected) -2.dp.toPx() else 0f
                                            }
                                        )
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
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = if (isAtBottomNav) innerPadding.calculateBottomPadding() else 0.dp
                ),
            enterTransition = {
                fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                    slideInHorizontally(
                        animationSpec = tween(260, easing = FastOutSlowInEasing),
                        initialOffsetX = { fullWidth -> (fullWidth * 0.08f).toInt() }
                    )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                    slideOutHorizontally(
                        animationSpec = tween(220, easing = FastOutSlowInEasing),
                        targetOffsetX = { fullWidth -> -(fullWidth * 0.05f).toInt() }
                    )
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                    slideInHorizontally(
                        animationSpec = tween(260, easing = FastOutSlowInEasing),
                        initialOffsetX = { fullWidth -> -(fullWidth * 0.08f).toInt() }
                    )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                    slideOutHorizontally(
                        animationSpec = tween(220, easing = FastOutSlowInEasing),
                        targetOffsetX = { fullWidth -> (fullWidth * 0.05f).toInt() }
                    )
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
