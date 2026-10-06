package com.aistudio.studyos.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.studyos.data.repository.TodayRecommendationCalculator
import com.aistudio.studyos.ui.viewmodel.StudyViewModel
import com.aistudio.studyos.ui.components.AnimatedReveal
import com.aistudio.studyos.ui.components.tactile3DButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: StudyViewModel,
    onOpenFocus: () -> Unit,
    onOpenStudy: () -> Unit,
    onOpenQuickFocus: () -> Unit,
    onOpenExamPlanner: () -> Unit,
    onOpenSavedSessions: () -> Unit,
    onOpenHistory: () -> Unit = {}
) {
    val profile by viewModel.userProfile.collectAsState()
    val upcomingExams by viewModel.upcomingExams.collectAsState()
    val allLogs by viewModel.allLogs.collectAsState()
    val activePlan by viewModel.activePlan.collectAsState()
    val todayMinutes by viewModel.todayMinutes.collectAsState()
    val focusState by viewModel.focusState.collectAsState()
    val shieldSavedNotice by viewModel.shieldSavedNotice.collectAsState()

    val streak = profile?.streakDays ?: 0
    val level = profile?.currentLevel ?: 1
    val dailyGoal = profile?.dailyGoalMinutes ?: 60
    val progressFraction = if (dailyGoal > 0) {
        (todayMinutes.toFloat() / dailyGoal).coerceIn(0f, 1f)
    } else 0f
    val goalComplete = dailyGoal > 0 && todayMinutes >= dailyGoal

    val todayRecommendation = TodayRecommendationCalculator.calculate(
        activePlan = activePlan,
        upcomingExams = upcomingExams,
        todayMinutes = todayMinutes,
        dailyGoalMinutes = dailyGoal
    )

    val horizontalPadding = if (LocalConfiguration.current.screenWidthDp < 360) 12.dp else 20.dp
    val bottomPadding = 24.dp

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = horizontalPadding,
            end = horizontalPadding,
            top = 16.dp,
            bottom = bottomPadding
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AnimatedReveal(index = 0) {
                HomeHeader(streak = streak, level = level, totalXP = profile?.totalXP ?: 0)
            }
        }

        if (shieldSavedNotice != null) {
            item {
                AnimatedReveal(index = 1) {
                    ShieldSavedBanner(
                        streak = streak,
                        onDismiss = { viewModel.dismissShieldNotice() }
                    )
                }
            }
        }

        item {
            AnimatedReveal(index = 1) {
                TodayFocusCard(
                activePlan = activePlan,
                todayMinutes = todayMinutes,
                dailyGoal = dailyGoal,
                progressFraction = progressFraction,
                goalComplete = goalComplete,
                recommendationTitle = todayRecommendation.title,
                recommendationDetail = todayRecommendation.detail,
                actionLabel = if (activePlan != null) {
                    "Continue Studying"
                } else if (goalComplete) {
                    "Study More"
                } else {
                    "Start Studying"
                },
                onAction = {
                    if (activePlan != null) {
                        viewModel.continueActiveSession(activePlan!!)
                        onOpenFocus()
                    } else {
                        onOpenStudy()
                    }
                }
                )
            }
        }

        item {
            AnimatedReveal(index = 2) {
                Text(
                text = "Start studying",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        item {
            AnimatedReveal(index = 3) {
                Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    icon = { Icon(Icons.Default.School, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary) },
                    title = "Study Plan",
                    subtitle = "Build a session",
                    testTag = "btn_study_plan",
                    onClick = onOpenStudy
                )
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    icon = { Icon(Icons.Default.Bolt, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.tertiary) },
                    title = "Quick Focus",
                    subtitle = "Start instantly",
                    testTag = "btn_quick_focus",
                    onClick = onOpenQuickFocus
                )
            }
                }
            }

        item {
            AnimatedReveal(index = 4) {
                StudyJourneyCard(
                    allLogs = allLogs,
                    activePlan = activePlan,
                    onOpenFocus = onOpenFocus,
                    onOpenStudy = onOpenStudy,
                    onOpenHistory = onOpenHistory
                )
            }
        }

        val nextExam = upcomingExams.firstOrNull()
        if (nextExam != null) {
            item {
                AnimatedReveal(index = 5) {
                    NextExamCard(
                        subject = nextExam.subject,
                        daysRemaining = nextExam.daysRemaining,
                        examDate = nextExam.examDate,
                        topics = nextExam.syllabusTopics,
                        onClick = onOpenExamPlanner
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HomeHeader(
    streak: Int,
    level: Int,
    totalXP: Int,
) {
    val streakPulse by rememberInfiniteTransition(label = "home_streak_pulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "home_streak_pulse_value"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = greetingForCurrentTime(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Let's make today count",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatPill(
                modifier = Modifier.weight(1f),
                icon = {
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        null,
                        Modifier
                            .size(17.dp)
                            .graphicsLayer {
                                scaleX = streakPulse
                                scaleY = streakPulse
                            },
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                },
                text = "$streak day streak"
            )
            StatPill(
                modifier = Modifier.weight(1f),
                icon = {
                    Text(
                        text = "Lv",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                text = level.toString()
            )
            StatPill(
                modifier = Modifier.weight(1f),
                icon = {
                    Icon(
                        Icons.Default.Bolt,
                        null,
                        Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                },
                text = "$totalXP XP"
            )
        }
    }
}

@Composable
private fun StatPill(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    text: String
) {
    Row(
        modifier = modifier
            .height(36.dp)
            .tactile3DButton(
                backgroundColor = MaterialTheme.colorScheme.surface,
                bottomEdgeColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                cornerRadius = 14.dp,
                depth = 4.dp
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        icon()
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun greetingForCurrentTime(): String {
    return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..23 -> "Good evening"
        else -> "Good night"
    }
}

@Composable
private fun ShieldSavedBanner(
    streak: Int,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shield_saved_streak_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Default.Shield,
                contentDescription = "Streak shield",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Streak saved",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Your " + streak + "-day streak was protected. Study today to keep it going.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, "Dismiss", tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun TodayFocusCard(
    activePlan: com.aistudio.studyos.data.local.entity.StudyPlanEntity?,
    todayMinutes: Int,
    dailyGoal: Int,
    progressFraction: Float,
    goalComplete: Boolean,
    recommendationTitle: String,
    recommendationDetail: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .tactile3DButton(
                backgroundColor = MaterialTheme.colorScheme.surface,
                bottomEdgeColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                cornerRadius = 24.dp,
                depth = 6.dp
            )
            .animateContentSize(animationSpec = tween(300))
            .testTag("today_engine_hero_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (goalComplete && activePlan == null) Icons.Default.CheckCircle else Icons.Default.TrackChanges,
                        null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (activePlan != null) "Continue studying" else "Today's focus",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = recommendationTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (activePlan != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "Block " + (activePlan.currentBlockIndex + 1) + "/" + activePlan.totalBlocks,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = recommendationDetail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Today's goal",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                AnimatedContent(
                    targetState = todayMinutes,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(160)) togetherWith
                            fadeOut(animationSpec = tween(100))
                    },
                    label = "home_today_minutes"
                ) { minutes ->
                    Text(
                        text = minutes.toString() + " / " + dailyGoal + " min",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (goalComplete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            val animatedGoalProgress by animateFloatAsState(
                targetValue = progressFraction,
                animationSpec = tween(700, easing = FastOutSlowInEasing),
                label = "daily_goal_progress"
            )
            LinearProgressIndicator(
                progress = { animatedGoalProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
            )

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .tactile3DButton(
                        backgroundColor = MaterialTheme.colorScheme.primary,
                        bottomEdgeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.48f),
                        cornerRadius = 15.dp,
                        depth = 5.dp
                    )
                    .testTag("start_study_button"),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                AnimatedContent(
                    targetState = goalComplete,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(160)) togetherWith
                            fadeOut(animationSpec = tween(100))
                    },
                    label = "home_action_icon"
                ) { complete ->
                    Icon(
                        if (complete) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                        null,
                        Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                AnimatedContent(
                    targetState = actionLabel,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(160)) togetherWith
                            fadeOut(animationSpec = tween(100))
                    },
                    label = "home_action_label"
                ) { label ->
                    Text(label, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StudyJourneyCard(
    allLogs: List<com.aistudio.studyos.data.local.entity.SessionLogEntity>,
    activePlan: com.aistudio.studyos.data.local.entity.StudyPlanEntity?,
    onOpenFocus: () -> Unit,
    onOpenStudy: () -> Unit,
    onOpenHistory: () -> Unit
) {
    val journeyDays = remember(allLogs) {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        (-4..4).map { offset ->
            val day = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
            val nextDay = (day.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
            val start = day.timeInMillis
            val end = nextDay.timeInMillis
            val minutes = if (offset <= 0) {
                allLogs.filter { it.timestamp >= start && it.timestamp < end }.sumOf { it.durationMinutes }
            } else 0
            JourneyDay(day, minutes, offset == 0, offset > 0)
        }
    }
    var selectedDay by remember { mutableStateOf<JourneyDay?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(280))
            .tactile3DButton(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.outline.copy(alpha = 0.58f), 24.dp, 8.dp)
            .testTag("study_journey_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Your Study Journey",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Keep moving forward, one day at a time",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "View all",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenHistory)
                        .padding(6.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // A broad S-curve layout keeps the journey playful without relying on
            // connector lines. Each node gently drifts left/right instead of zig-zagging.
            val journeyPositions = listOf(
                0.50f, 0.62f, 0.70f, 0.62f, 0.50f,
                0.38f, 0.30f, 0.38f, 0.50f
            )
            val journeyStep = 132.dp

            androidx.compose.foundation.layout.BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((journeyStep.value * journeyDays.size + 44).dp)
            ) {
                // Floating study objects live in the open pockets of the curve.
                JourneyFloatingDecoration(
                    icon = Icons.Default.School,
                    label = "LEARN",
                    modifier = Modifier.offset(
                        x = maxWidth * 0.12f,
                        y = 30.dp
                    )
                )
                JourneyFloatingDecoration(
                    icon = Icons.Default.TrackChanges,
                    label = "FOCUS",
                    modifier = Modifier.offset(
                        x = maxWidth * 0.73f,
                        y = 138.dp
                    )
                )
                JourneyFloatingDecoration(
                    icon = Icons.Default.Bolt,
                    label = "XP",
                    modifier = Modifier.offset(
                        x = maxWidth * 0.10f,
                        y = 354.dp
                    )
                )
                JourneyFloatingDecoration(
                    icon = Icons.Default.Timer,
                    label = "TIME",
                    modifier = Modifier.offset(
                        x = maxWidth * 0.74f,
                        y = 570.dp
                    )
                )

                journeyDays.forEachIndexed { index, day ->
                    val isCompleted = !day.isFuture && day.minutes > 0
                    val nodeSize = if (day.isToday) 86.dp else 72.dp
                    JourneyNode(
                        day = day,
                        isCompleted = isCompleted,
                        size = nodeSize,
                        onClick = { selectedDay = day },
                        modifier = Modifier.offset(
                            x = maxWidth * journeyPositions[index] - nodeSize / 2,
                            y = (index * journeyStep.value).dp
                        )
                    )
                }
            }
        }
    }

    selectedDay?.let { day ->
        val isCompleted = !day.isFuture && day.minutes > 0
        AlertDialog(
            onDismissRequest = { selectedDay = null },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = if (day.isToday) {
                        "Today • " + SimpleDateFormat("MMM d", Locale.getDefault()).format(day.date.time)
                    } else {
                        SimpleDateFormat("MMM d", Locale.getDefault()).format(day.date.time)
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                AnimatedReveal(index = 1) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().tactile3DButton(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.outline.copy(alpha = 0.36f), 14.dp, 3.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when {
                        day.isToday && activePlan != null -> {
                            Text("Your active study plan is ready.")
                            Text(
                                "Continue your current session or open the Study Plan.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        day.isToday -> {
                            Text(
                                if (isCompleted) {
                                    "You have already studied " + day.minutes + " minutes today."
                                } else {
                                    "You haven't studied yet today."
                                }
                            )
                            Text(
                                "Ready to make today count?",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        day.isFuture -> {
                            Icon(Icons.Default.Event, null, tint = MaterialTheme.colorScheme.primary)
                            Text("This day hasn't arrived yet.")
                        }
                        isCompleted -> {
                            Text(day.minutes.toString() + " minutes studied on this day.")
                            Text(
                                "You can review the activity from History.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        else -> {
                            Text("No study was recorded on this day.")
                            Text(
                                "Every day is a fresh start. Study today to keep moving forward.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    }
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (day.isToday || (!day.isFuture && isCompleted)) Arrangement.End else Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { selectedDay = null },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier
                            .height(40.dp)
                            .tactile3DButton(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.outline.copy(alpha = 0.34f), 12.dp, 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close", maxLines = 1, softWrap = false, textAlign = TextAlign.Center)
                    }

                    if (day.isToday || (!day.isFuture && isCompleted)) {
                        Spacer(Modifier.width(8.dp))
                        if (day.isToday) {
                            Button(
                                onClick = {
                                    selectedDay = null
                                    if (activePlan != null) onOpenFocus() else onOpenStudy()
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .height(40.dp)
                                    .tactile3DButton(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.52f), 12.dp, 4.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (activePlan != null) "Continue Session" else "Start a Session", maxLines = 1, softWrap = false, textAlign = TextAlign.Center)
                            }
                        } else {
                            Button(
                                onClick = { selectedDay = null; onOpenHistory() },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .height(40.dp)
                                    .tactile3DButton(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.52f), 12.dp, 4.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("View Activity", maxLines = 1, softWrap = false, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }        )
    }
}

private data class JourneyDay(
    val date: Calendar,
    val minutes: Int,
    val isToday: Boolean,
    val isFuture: Boolean
)

@Composable
private fun JourneyNode(
    day: JourneyDay,
    isCompleted: Boolean,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateLabel = SimpleDateFormat("MMM d", Locale.getDefault()).format(day.date.time)
    val primary = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.onSurfaceVariant
    val nodeScale by animateFloatAsState(
        targetValue = if (isCompleted || day.isToday) 1f else 0.94f,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "journey_node_scale"
    )
    val todayPulse = if (day.isToday) {
        val transition = rememberInfiniteTransition(label = "journey_today_pulse")
        transition.animateFloat(
            initialValue = 0.985f,
            targetValue = 1.045f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "journey_today_pulse_scale"
        ).value
    } else 1f

    val icon = when {
        day.isToday -> "★"
        isCompleted -> "✓"
        day.isFuture -> "○"
        else -> "·"
    }

    Column(
        modifier = modifier
            .graphicsLayer {
                val nodeScaleValue = nodeScale * todayPulse
                scaleX = nodeScaleValue
                scaleY = nodeScaleValue
            }
            .clickable(onClick = onClick)
            .testTag(
                if (day.isToday) "journey_today_node"
                else "journey_day_" + dateLabel.replace(" ", "_")
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(size + 12.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            // Thick lower plate = the physical depth of the coin/button.
            Box(
                modifier = Modifier
                    .size(size)
                    .offset(y = 7.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            day.isToday -> primary.copy(alpha = 0.48f)
                            isCompleted -> primary.copy(alpha = 0.42f)
                            day.isFuture -> MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)
                            else -> Color.Black.copy(alpha = 0.28f)
                        }
                    )
            )

            if (day.isToday) {
                Box(
                    modifier = Modifier
                        .size(size + 10.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            color = primary.copy(alpha = 0.22f),
                            shape = CircleShape
                        )
                )
            }

            Box(
                modifier = Modifier
                    .size(size)
                    .shadow(
                        elevation = if (day.isToday) 12.dp else 7.dp,
                        shape = CircleShape,
                        clip = false
                    )
                    .clip(CircleShape)
                    .background(
                        when {
                            day.isToday -> primary
                            isCompleted -> primary.copy(alpha = 0.90f)
                            day.isFuture -> MaterialTheme.colorScheme.surface
                            else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                        }
                    )
                    .border(
                        width = if (day.isToday) 2.dp else 1.dp,
                        color = when {
                            day.isToday -> primary.copy(alpha = 0.90f)
                            isCompleted -> primary.copy(alpha = 0.45f)
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    icon,
                    fontSize = if (day.isToday) 25.sp else 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = when {
                        day.isToday || isCompleted -> MaterialTheme.colorScheme.onPrimary
                        else -> inactive.copy(alpha = if (day.isFuture) 0.62f else 0.72f)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(1.dp))
        Text(
            if (day.isToday) "TODAY" else dateLabel,
            fontSize = 10.sp,
            fontWeight = if (day.isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (day.isToday) primary else inactive
        )

        when {
            isCompleted -> {
                Text(
                    day.minutes.toString() + "m studied",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = inactive.copy(alpha = 0.82f)
                )
            }
            day.isToday -> {
                Text(
                    "Your next step",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = primary.copy(alpha = 0.92f)
                )
            }
            day.isFuture -> {
                Text(
                    "Locked",
                    fontSize = 10.sp,
                    color = inactive.copy(alpha = 0.58f)
                )
            }
        }
    }
}

@Composable
private fun JourneyFloatingDecoration(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "journey_decor_$label")
    val bob by transition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "journey_decor_bob_$label"
    )
    val tilt by transition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "journey_decor_tilt_$label"
    )
    val glow by transition.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "journey_decor_glow_$label"
    )

    Column(
        modifier = modifier
            .size(84.dp)
            .graphicsLayer {
                translationY = bob.dp.toPx()
                rotationZ = tilt
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .shadow(12.dp, RoundedCornerShape(16.dp), clip = false)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = glow),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
                modifier = Modifier.size(29.dp)
            )
        }
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
        )
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier,
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .tactile3DButton(
                backgroundColor = MaterialTheme.colorScheme.surface,
                bottomEdgeColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                cornerRadius = 18.dp,
                depth = 5.dp
            )
            .animateContentSize(animationSpec = tween(220))
            .testTag(testTag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            icon()
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun NextExamCard(
    subject: String,
    daysRemaining: Int,
    examDate: String,
    topics: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .tactile3DButton(
                backgroundColor = MaterialTheme.colorScheme.surface,
                bottomEdgeColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                cornerRadius = 20.dp,
                depth = 5.dp
            )
            .testTag("spotlight_upcoming_exam_card")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(17.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Timer,
                        null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Next exam",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = subject,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = if (daysRemaining > 0) daysRemaining.toString() + "d left" else examDate,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (topics.isNotBlank()) {
                Spacer(modifier = Modifier.height(9.dp))
                Text(
                    text = topics,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "View exam plan",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.ArrowForward,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

