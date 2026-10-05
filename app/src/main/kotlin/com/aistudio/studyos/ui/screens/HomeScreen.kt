package com.aistudio.studyos.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.studyos.data.repository.TodayRecommendationCalculator
import com.aistudio.studyos.ui.viewmodel.StudyViewModel
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
    val bottomPadding = if (focusState.planId != null) 150.dp else 96.dp

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
            HomeHeader(streak = streak, level = level)
        }

        if (shieldSavedNotice != null) {
            item {
                ShieldSavedBanner(
                    streak = streak,
                    onDismiss = { viewModel.dismissShieldNotice() }
                )
            }
        }

        item {
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

        item {
            Text(
                text = "Start studying",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        item {
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
                    icon = { Icon(Icons.Default.Bolt, null, Modifier.size(28.dp), tint = Color(0xFFF59E0B)) },
                    title = "Quick Focus",
                    subtitle = "Start instantly",
                    testTag = "btn_quick_focus",
                    onClick = onOpenQuickFocus
                )
            }
        }

        item {
            StudyJourneyCard(
                allLogs = allLogs,
                activePlan = activePlan,
                onOpenFocus = onOpenFocus,
                onOpenStudy = onOpenStudy,
                onOpenHistory = onOpenHistory
            )
        }

        val nextExam = upcomingExams.firstOrNull()
        if (nextExam != null) {
            item {
                NextExamCard(
                    subject = nextExam.subject,
                    daysRemaining = nextExam.daysRemaining,
                    examDate = nextExam.examDate,
                    topics = nextExam.syllabusTopics,
                    onClick = onOpenExamPlanner
                )
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
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
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

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StatPill(
                icon = {
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        null,
                        Modifier.size(17.dp),
                        tint = Color(0xFFF97316)
                    )
                },
                text = streak.toString() + " day streak"
            )
            StatPill(
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
        }
    }
}

@Composable
private fun StatPill(
    icon: @Composable () -> Unit,
    text: String
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        icon()
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A))
    ) {
        Row(
            modifier = Modifier.padding(13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Default.Shield,
                contentDescription = "Streak shield",
                tint = Color(0xFF93C5FD),
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Streak saved",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
                Text(
                    text = "Your " + streak + "-day streak was protected. Study today to keep it going.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFDBEAFE)
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, "Dismiss", tint = Color.White)
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
            .testTag("today_engine_hero_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                Text(
                    text = todayMinutes.toString() + " / " + dailyGoal + " min",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (goalComplete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progressFraction },
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
                    .testTag("start_study_button"),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Default.PlayArrow, null, Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(actionLabel, fontWeight = FontWeight.Bold)
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
        modifier = Modifier.fillMaxWidth().testTag("study_journey_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Your Study Journey", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Your study, day by day", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    "View all",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onOpenHistory).padding(6.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            journeyDays.forEachIndexed { index, day ->
                val isCompleted = !day.isFuture && day.minutes > 0
                val nodeSize = if (day.isToday) 54.dp else 42.dp
                val rowAlignment = if (index % 2 == 0) Alignment.CenterStart else Alignment.CenterEnd
                val horizontalArrangement = if (index % 2 == 0) Arrangement.Start else Arrangement.End
                Box(modifier = Modifier.fillMaxWidth().height(if (index == journeyDays.lastIndex) 62.dp else 72.dp)) {
                    if (index < journeyDays.lastIndex) {
                        Box(
                            modifier = Modifier.width(2.dp).height(42.dp).align(Alignment.Center)
                                .background(
                                    if (isCompleted || day.isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                )
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().align(rowAlignment),
                        horizontalArrangement = horizontalArrangement,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        JourneyNode(day, isCompleted, nodeSize) { selectedDay = day }
                    }
                }
            }
        }
    }

    selectedDay?.let { day ->
        val isCompleted = !day.isFuture && day.minutes > 0
        AlertDialog(
            onDismissRequest = { selectedDay = null },
            title = {
                Text(
                    text = if (day.isToday) "Today • " + SimpleDateFormat("MMM d", Locale.getDefault()).format(day.date.time)
                    else SimpleDateFormat("MMM d", Locale.getDefault()).format(day.date.time),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when {
                        day.isToday && activePlan != null -> {
                            Text("Your active study plan is ready.")
                            Text("Continue your current session or open the Study Plan.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        day.isToday -> {
                            Text(if (isCompleted) "You have already studied " + day.minutes + " minutes today." else "You haven't studied yet today.")
                            Text("Ready to make today count?", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        day.isFuture -> {
                            Icon(Icons.Default.Event, null, tint = MaterialTheme.colorScheme.primary)
                            Text("This day hasn't arrived yet.")
                        }
                        isCompleted -> {
                            Text(day.minutes.toString() + " minutes studied on this day.")
                            Text("You can review the activity from History.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        else -> {
                            Text("No study was recorded on this day.")
                            Text("Every day is a fresh start. Study today to keep moving forward.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                if (day.isToday) {
                    Button(onClick = {
                        selectedDay = null
                        if (activePlan != null) onOpenFocus() else onOpenStudy()
                    }) {
                        Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (activePlan != null) "Continue Session" else "Start a Session")
                    }
                } else if (!day.isFuture && isCompleted) {
                    Button(onClick = { selectedDay = null; onOpenHistory() }) { Text("View Activity") }
                }
            },
            dismissButton = { TextButton(onClick = { selectedDay = null }) { Text("Close") } }
        )
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
    onClick: () -> Unit
) {
    val dateLabel = SimpleDateFormat("MMM d", Locale.getDefault()).format(day.date.time)
    val fill = when {
        day.isToday -> MaterialTheme.colorScheme.primary
        isCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.88f)
        day.isFuture -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    }
    val icon = when {
        day.isToday -> "★"
        isCompleted -> "✓"
        day.isFuture -> "○"
        else -> "·"
    }

    Column(
        modifier = Modifier.clickable(onClick = onClick).testTag(
            if (day.isToday) "journey_today_node" else "journey_day_" + dateLabel.replace(" ", "_")
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(size).clip(CircleShape).background(fill).border(
                width = if (day.isToday) 3.dp else 1.dp,
                color = if (day.isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
                shape = CircleShape
            ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                icon,
                fontSize = if (day.isToday) 22.sp else 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (day.isToday || isCompleted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            if (day.isToday) "TODAY" else dateLabel,
            fontSize = 10.sp,
            fontWeight = if (day.isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (isCompleted) Text(day.minutes.toString() + "m", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            .testTag(testTag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
            .testTag("spotlight_upcoming_exam_card")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                        .background(Color(0xFFEF4444).copy(alpha = 0.13f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Timer,
                        null,
                        tint = Color(0xFFEF4444),
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
                    color = Color(0xFFEF4444)
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

