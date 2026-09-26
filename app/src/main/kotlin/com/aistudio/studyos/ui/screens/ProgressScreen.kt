package com.aistudio.studyos.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.studyos.data.local.entity.SessionLogEntity
import com.aistudio.studyos.ui.viewmodel.StudyViewModel
import com.aistudio.studyos.data.repository.ProgressAnalyticsCalculator
import com.aistudio.studyos.data.repository.GamificationCalculator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DayActivityData(
    val dayName: String,
    val dayNumber: Int,
    val minutes: Int,
    val isToday: Boolean
)

fun getRankTitle(level: Int): String = when {
    level <= 1 -> "Novice Scholar"
    level == 2 -> "Apprentice Scholar"
    level == 3 -> "Junior Scholar"
    level == 4 -> "Adept Scholar"
    level == 5 -> "Honor Fellow"
    level == 6 -> "Master Academic"
    level == 7 -> "Doctoral Fellow"
    level == 8 -> "Distinguished Professor"
    else -> "Grandmaster Polymath"
}

fun formatLogTimestamp(timestamp: Long): String {
    val now = Calendar.getInstance()
    val logCal = Calendar.getInstance().apply { timeInMillis = timestamp }

    val isToday = now.get(Calendar.YEAR) == logCal.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == logCal.get(Calendar.DAY_OF_YEAR)

    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val isYesterday = yesterday.get(Calendar.YEAR) == logCal.get(Calendar.YEAR) &&
            yesterday.get(Calendar.DAY_OF_YEAR) == logCal.get(Calendar.DAY_OF_YEAR)

    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

    return when {
        isToday -> "Today, ${timeFormat.format(Date(timestamp))}"
        isYesterday -> "Yesterday, ${timeFormat.format(Date(timestamp))}"
        else -> dateFormat.format(Date(timestamp))
    }
}

fun getModeBadgeLabel(mode: String): String {
    return when (mode.lowercase()) {
        "focus" -> "🎯 Focus Block"
        "exam" -> "📝 Exam Prep"
        "cram" -> "⚡ Cram Session"
        "early_finish" -> "⏱️ Quick Session"
        "regular" -> "📖 Regular Study"
        else -> "📚 Study Session"
    }
}

private fun calculateWeeklyActivity(logs: List<SessionLogEntity>): List<DayActivityData> {
    val now = Calendar.getInstance()
    val todayYear = now.get(Calendar.YEAR)
    val todayDayOfYear = now.get(Calendar.DAY_OF_YEAR)

    // Rewind to Monday of the current week
    val cal = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
    }
    while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
        cal.add(Calendar.DAY_OF_MONTH, -1)
    }
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)

    val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val result = mutableListOf<DayActivityData>()

    for (i in 0 until 7) {
        val startOfDay = cal.timeInMillis
        val dayYear = cal.get(Calendar.YEAR)
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        val dayNumber = cal.get(Calendar.DAY_OF_MONTH)

        cal.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = cal.timeInMillis

        val isToday = (dayYear == todayYear && dayOfYear == todayDayOfYear)

        val minutes = logs.filter { it.timestamp in startOfDay until endOfDay }
            .sumOf { it.durationMinutes }

        result.add(
            DayActivityData(
                dayName = dayNames[i],
                dayNumber = dayNumber,
                minutes = minutes,
                isToday = isToday
            )
        )
    }
    return result
}

@Composable
fun ProgressScreen(
    viewModel: StudyViewModel,
    onOpenHistory: () -> Unit = {}
) {
    val profile by viewModel.userProfile.collectAsState()
    val recentLogs by viewModel.recentLogs.collectAsState()
    val allLogs by viewModel.allLogs.collectAsState()
    val isAllLogsLoaded by viewModel.isAllLogsLoaded.collectAsState()
    val todayMinutes by viewModel.todayMinutes.collectAsState()
    val weeklyData = remember(allLogs) { calculateWeeklyActivity(allLogs) }
    val totalWeekMinutes = remember(weeklyData) { weeklyData.sumOf { it.minutes } }
    val activePlan by viewModel.activePlan.collectAsState()
    val latestCompletedPlan by viewModel.latestCompletedPlan.collectAsState()
    val analytics = remember(allLogs, activePlan, latestCompletedPlan) {
        ProgressAnalyticsCalculator.calculate(allLogs, activePlan, latestCompletedPlan = latestCompletedPlan)
    }

    var showAllLogs by remember { mutableStateOf(false) }
    var logToDelete by remember { mutableStateOf<SessionLogEntity?>(null) }
    val focusState by viewModel.focusState.collectAsState()
    val bottomListPadding = if (focusState.planId != null) 150.dp else 96.dp

    // Total Time uses the same session-log source as Today/Consistency.
    // This keeps partial/skip time visible everywhere instead of depending on a
    // separately maintained profile counter.
    val totalMins = allLogs.sumOf { it.durationMinutes.coerceAtLeast(0) }
    val totalHours = totalMins / 60
    val remainingMins = totalMins % 60
    val totalXP = profile?.totalXP ?: 0
    val currentLevel = profile?.currentLevel ?: 1
    val currentRankTitle = remember(currentLevel) { getRankTitle(currentLevel) }
    val nextRankTitle = remember(currentLevel) { getRankTitle(currentLevel + 1) }

    val xpInCurrentLevel = totalXP % 200
    val xpNeededForNext = 200 - xpInCurrentLevel
    val levelProgress = (xpInCurrentLevel.toFloat() / 200f).coerceIn(0f, 1f)
    val levelPercentage = (levelProgress * 100).toInt()
    // Keep tiny progress visible without changing the displayed percentage/value.
    val visibleLevelProgress = if (levelProgress > 0f) maxOf(levelProgress, 0.04f) else 0f

    val streak = profile?.streakDays ?: 0
    val streakText = if (streak == 1) "1 Day" else "$streak Days"
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val todayDateStr = remember { sdf.format(Date()) }
    val isStreakDoneToday = profile?.lastActiveDate == todayDateStr && streak > 0
    val achievements = remember(totalMins, streak, allLogs.size) { GamificationCalculator.achievements(totalMins, streak, allLogs.size) }

    val displayedLogs = if (showAllLogs) allLogs else recentLogs
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    // Delete Log Confirmation Dialog
    if (logToDelete != null) {
        val targetLog = logToDelete!!
        AlertDialog(
            onDismissRequest = { logToDelete = null },
            title = {
                Text(
                    text = "Delete Session Log",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove this record for '${targetLog.subject}'?\n\nThis will deduct ${targetLog.durationMinutes} minutes and ${targetLog.xpEarned} XP from your totals.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSessionLog(targetLog)
                        logToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { logToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = bottomListPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Progress & Analytics",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Track your learning consistency, rankings, and daily habits",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Tab Selector: Overview vs History
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .testTag("progress_tab_row"),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Insights, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Overview", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                        }
                    },
                    modifier = Modifier.testTag("tab_overview")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(
                                if (allLogs.isNotEmpty()) "History & Logs (${allLogs.size})" else "History & Logs",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    },
                    modifier = Modifier.testTag("tab_history")
                )
            }
        }

        if (selectedTab == 0) {
            // Summary Metric Cards: Streak & Total Time
            item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Streak Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("streak_metric_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Streak Fire",
                                tint = Color(0xFFF97316),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Streak",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = streakText,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when {
                                isStreakDoneToday -> "🔥 Maintained today"
                                streak > 0 -> "⚡ Study today to keep"
                                else -> "🌱 Start streak today"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isStreakDoneToday) Color(0xFFF97316) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Total Study Time Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("total_time_metric_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Total Time",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Total Time",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (totalHours > 0) "${totalHours}h ${remainingMins}m" else "${remainingMins}m",
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${allLogs.size} completed sessions",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            val dailyGoal = profile?.dailyGoalMinutes ?: 60
            val todayProgress = (todayMinutes.toFloat() / dailyGoal.coerceAtLeast(1)).coerceIn(0f, 1f)
            val remaining = (dailyGoal - todayMinutes).coerceAtLeast(0)
            Card(
                modifier = Modifier.fillMaxWidth().testTag("today_goal_analytics_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Today", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                if (remaining > 0) "${todayMinutes}m studied • ${remaining}m remaining" else "${todayMinutes}m studied • Daily target reached",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .78f)
                            )
                        }
                        Text(
                            "${(todayProgress * 100).toInt()}%",
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { todayProgress },
                        modifier = Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(5.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface.copy(alpha = .35f)
                    )
                    Text(
                        "Daily target: " + if (dailyGoal >= 60) (dailyGoal / 60).toString() + "h " + (dailyGoal % 60).toString() + "m" else dailyGoal.toString() + "m",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .75f)
                    )
                }
            }
        }

        item {
            val subjectTotals = allLogs.groupBy { it.subject.ifBlank { "Other" } }
                .mapValues { (_, logs) -> logs.sumOf { it.durationMinutes } }
                .entries.sortedByDescending { it.value }.take(4)
            if (subjectTotals.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("subject_analytics_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Subject Breakdown", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        subjectTotals.forEach { entry ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(entry.key, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                                Text(
                                    if (entry.value >= 60) "${entry.value / 60}h ${entry.value % 60}m" else "${entry.value}m",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("plan_analytics_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Plan & Consistency",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    // 1. Study Plan Progress Section
                    if (analytics.activePlanTitle != null && analytics.plannedMinutes > 0) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        analytics.activePlanTitle,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "${analytics.actualMinutes}m completed • ${analytics.activePlanRemainingMinutes}m remaining",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    "${analytics.planCompletionPercent}%",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            LinearProgressIndicator(
                                progress = { analytics.planCompletionPercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "No active study plan • Focus blocks still fuel your consistency!",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )

                    // 2. Consistency Section (7-day Habit Tracking)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "${analytics.consistencyDays}/7 Days Active",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "studied in the last 7 days",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${analytics.averageMinutesOnStudyDays}m",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "avg on study days",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // 7-day Visual Circles Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            weeklyData.forEach { day ->
                                val hasStudied = day.minutes > 0
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    hasStudied -> MaterialTheme.colorScheme.primary
                                                    day.isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                    else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                                                }
                                            )
                                            .border(
                                                width = if (day.isToday && !hasStudied) 1.5.dp else 0.dp,
                                                color = if (day.isToday && !hasStudied) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (hasStudied) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Studied",
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (day.isToday) MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                                    )
                                            )
                                        }
                                    }
                                    Text(
                                        text = day.dayName.take(1),
                                        fontSize = 11.sp,
                                        fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium,
                                        color = if (day.isToday) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("achievement_milestones_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Study Milestones",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                "Keep studying to unlock new achievements",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                "${achievements.count { it.unlocked }}/${achievements.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }

                    achievements.forEach { achievement ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (achievement.unlocked) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (achievement.unlocked) {
                                        Icons.Default.CheckCircle
                                    } else {
                                        Icons.Default.Schedule
                                    },
                                    contentDescription = null,
                                    tint = if (achievement.unlocked) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(Modifier.weight(1f)) {
                                Text(
                                    achievement.title,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    achievement.description,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(7.dp),
                                color = if (achievement.unlocked) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                                }
                            ) {
                                Text(
                                    if (achievement.unlocked) "Unlocked" else "Locked",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (achievement.unlocked) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Gamification, Level & Next Rank Progress Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gamification_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF59E0B).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = "Trophy",
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = currentRankTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "$totalXP Total XP Earned",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Level $currentLevel",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Next Rank Progress Breakdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Next Rank: ",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                text = nextRankTitle,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = "$xpInCurrentLevel / 200 XP ($levelPercentage%)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { visibleLevelProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "$xpNeededForNext XP needed to unlock Level ${currentLevel + 1}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    )
                }
            }
        }

        // Weekly Activity Visualizer
        item {
            val weekHours = totalWeekMinutes / 60
            val weekRemMins = totalWeekMinutes % 60
            val maxDayMinutes = weeklyData.maxOfOrNull { it.minutes } ?: 0
            val ceilingMinutes = maxOf(maxDayMinutes, profile?.dailyGoalMinutes ?: 60)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("weekly_activity_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Insights,
                                contentDescription = "Weekly Insights",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Weekly Activity Pattern",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (totalWeekMinutes > 0) {
                                        if (weekHours > 0) "$weekHours hrs $weekRemMins mins logged this week"
                                        else "$weekRemMins mins logged this week"
                                    } else {
                                        "No study time recorded this week yet"
                                    },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (totalWeekMinutes > 0) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = if (weekHours > 0) "${weekHours}h ${weekRemMins}m" else "${weekRemMins}m",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 7-day Bar Chart
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        weeklyData.forEach { day ->
                            val heightFraction = if (ceilingMinutes > 0) {
                                (day.minutes.toFloat() / ceilingMinutes).coerceIn(0f, 1f)
                            } else 0f
                            val barHeightDp = if (day.minutes > 0) {
                                (14f + (70f * heightFraction)).dp
                            } else {
                                4.dp
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Minute tag above bar
                                if (day.minutes > 0) {
                                    val minLabel = if (day.minutes >= 60) {
                                        "${day.minutes / 60}h"
                                    } else {
                                        "${day.minutes}m"
                                    }
                                    Text(
                                        text = minLabel,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (day.isToday) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                } else {
                                    Text(
                                        text = "-",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Visual Bar
                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .height(barHeightDp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            when {
                                                day.isToday && day.minutes > 0 -> MaterialTheme.colorScheme.primary
                                                day.isToday && day.minutes == 0 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                                day.minutes > 0 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                            }
                                        )
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // Day Name
                                Text(
                                    text = day.dayName,
                                    fontSize = 11.sp,
                                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium,
                                    color = if (day.isToday) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // Date Number
                                Text(
                                    text = "${day.dayNumber}",
                                    fontSize = 10.sp,
                                    color = if (day.isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }

                    if (totalWeekMinutes == 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Start a focus or regular session to track your daily pattern.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }
        } else {
            // Session History Log Header & Filter
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Session History Log",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (allLogs.isNotEmpty()) "Showing ${displayedLogs.size} of ${allLogs.size} sessions" else "No logged sessions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (allLogs.size > 10) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = !showAllLogs,
                                onClick = { showAllLogs = false },
                                label = { Text("Recent", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                            FilterChip(
                                selected = showAllLogs,
                                onClick = { showAllLogs = true },
                                label = { Text("All (${allLogs.size})", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }

        if (displayedLogs.isEmpty()) {
            if (isAllLogsLoaded) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No study sessions recorded yet",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Complete a study session or focus block to start building your knowledge log.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        } else {
            items(
                items = displayedLogs,
                key = { it.id }
            ) { log ->
                val modeLabel = remember(log.mode) { getModeBadgeLabel(log.mode) }
                val modeColor = MaterialTheme.colorScheme.primary
                val timeString = remember(log.timestamp) { formatLogTimestamp(log.timestamp) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("session_log_item_${log.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = modeColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = log.subject,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = log.chapter.ifBlank { "General Practice" },
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "+${log.xpEarned} XP",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                IconButton(
                                    onClick = { logToDelete = log },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete log entry",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Bottom Metadata Row: Mode Chip + Duration + Formatted Time
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mode Chip
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = modeColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = modeLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = modeColor,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${log.durationMinutes}m • $timeString",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
