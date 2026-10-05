package com.aistudio.studyos.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.studyos.data.local.entity.SessionLogEntity
import com.aistudio.studyos.ui.viewmodel.StudyViewModel
import com.aistudio.studyos.ui.components.AnimatedCounter
import com.aistudio.studyos.ui.components.AnimatedReveal
import com.aistudio.studyos.data.repository.ProgressAnalyticsCalculator
import com.aistudio.studyos.data.repository.LevelMissionCalculator
import com.aistudio.studyos.data.repository.LevelMissionProgress
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

@Composable
private fun MissionProgressRow(
    icon: String,
    title: String,
    valueText: String,
    progress: Float,
    complete: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 17.sp)
            Spacer(Modifier.width(8.dp))
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Text(
                if (complete) "✓" else valueText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (complete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
            color = if (complete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.78f),
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        )
    }
}

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

private fun calculateMonthlyActivity(logs: List<SessionLogEntity>): List<DayActivityData> {
    val today = Calendar.getInstance()
    val month = today.get(Calendar.MONTH)
    val year = today.get(Calendar.YEAR)
    val daysInMonth = today.getActualMaximum(Calendar.DAY_OF_MONTH)
    val minutesByDay = IntArray(daysInMonth)
    logs.forEach { log ->
        val c = Calendar.getInstance().apply { timeInMillis = log.timestamp }
        if (c.get(Calendar.YEAR) == year && c.get(Calendar.MONTH) == month) {
            minutesByDay[c.get(Calendar.DAY_OF_MONTH) - 1] += log.durationMinutes.coerceAtLeast(0)
        }
    }
    return minutesByDay.mapIndexed { index, minutes ->
        DayActivityData("", index + 1, minutes, index + 1 == today.get(Calendar.DAY_OF_MONTH))
    }
}

private data class WeekDay(val label: String, val minutes: Int, val isToday: Boolean)

private fun calculateCurrentWeek(logs: List<SessionLogEntity>): List<WeekDay> {
    val today = Calendar.getInstance()
    val monday = Calendar.getInstance().apply {
        timeInMillis = today.timeInMillis
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        val day = get(Calendar.DAY_OF_WEEK)
        add(Calendar.DAY_OF_YEAR, -(if (day == Calendar.SUNDAY) 6 else day - Calendar.MONDAY))
    }
    return (0 until 7).map { offset ->
        val start = Calendar.getInstance().apply { timeInMillis = monday.timeInMillis; add(Calendar.DAY_OF_YEAR, offset) }
        val end = Calendar.getInstance().apply { timeInMillis = start.timeInMillis; add(Calendar.DAY_OF_YEAR, 1) }
        val minutes = logs.filter { it.timestamp in start.timeInMillis until end.timeInMillis }
            .sumOf { it.durationMinutes.coerceAtLeast(0) }
        WeekDay(
            SimpleDateFormat("EEE", Locale.getDefault()).format(start.time),
            minutes,
            start.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                start.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
        )
    }
}

private fun formatMinutes(minutes: Int): String {
    val safe = minutes.coerceAtLeast(0)
    return if (safe >= 60) "${safe / 60}h ${safe % 60}m" else "${safe}m"
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    val valueScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(350),
        label = "progress_stat_scale"
    )
    Surface(
        modifier = modifier.animateContentSize(animationSpec = tween(220)),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 11.dp)) {
            Text(
                value,
                fontSize = 17.sp,
                modifier = Modifier.scale(valueScale),
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ProgressScreen(
    viewModel: StudyViewModel,
    onOpenHistory: () -> Unit = {}
) {
    val profile by viewModel.userProfile.collectAsState()
    val recentLogs by viewModel.recentLogs.collectAsState()
    val allLogs by viewModel.allLogs.collectAsState()
    val currentMonthLogs by viewModel.currentMonthLogs.collectAsState()
    val streakShieldCount by viewModel.streakShieldCount.collectAsState()
    val isAllLogsLoaded by viewModel.isAllLogsLoaded.collectAsState()
    val todayMinutes by viewModel.todayMinutes.collectAsState()
    val monthlyData = remember(currentMonthLogs) { calculateMonthlyActivity(currentMonthLogs) }
    val currentYearMinutes by viewModel.currentYearMinutes.collectAsState()
    val currentYearSessionCount by viewModel.currentYearSessionCount.collectAsState()
    val topicCount by viewModel.levelMissionTopicCount.collectAsState()
    val peakDailyFocusMinutes by viewModel.levelMissionPeakFocusMinutes.collectAsState()
    val activePlan by viewModel.activePlan.collectAsState()
    val latestCompletedPlan by viewModel.latestCompletedPlan.collectAsState()
    val weeklyData = remember(allLogs) { calculateCurrentWeek(allLogs) }
    val totalMonthMinutes = monthlyData.sumOf { it.minutes }
    val activeMonthDays = monthlyData.count { it.minutes > 0 }
    val peakMonthDay = monthlyData.maxByOrNull { it.minutes }
    val weeklyTotal = weeklyData.sumOf { it.minutes }
    val maxWeekMinutes = weeklyData.maxOfOrNull { it.minutes }?.coerceAtLeast(1) ?: 1
    val bestRecentSession = allLogs.maxOfOrNull { it.durationMinutes.coerceAtLeast(0) } ?: 0
    val analytics = remember(allLogs, activePlan, latestCompletedPlan) {
        ProgressAnalyticsCalculator.calculate(allLogs, activePlan, latestCompletedPlan = latestCompletedPlan)
    }
    val subjectTotals = remember(allLogs) {
        allLogs
            .groupBy { it.subject.ifBlank { "General" } }
            .mapValues { (_, logs) -> logs.sumOf { it.durationMinutes.coerceAtLeast(0) } }
            .entries
            .sortedByDescending { it.value }
    }

    val focusState by viewModel.focusState.collectAsState()
    val bottomListPadding = if (focusState.planId != null) 150.dp else 96.dp

    // Total Time uses the same session-log source as Today/Consistency.
    // This keeps partial/skip time visible everywhere instead of depending on a
    // separately maintained profile counter.
    val totalXP = profile?.totalXP ?: 0
    val currentLevel = (profile?.currentLevel ?: 1).coerceIn(1, 100)
    val currentRankTitle = remember(currentLevel) { LevelMissionCalculator.rankTitle(currentLevel) }
    val missionProgress: LevelMissionProgress? = remember(profile, currentLevel, topicCount, peakDailyFocusMinutes) {
        profile?.let {
            LevelMissionCalculator.calculate(currentLevel, it, topicCount, peakDailyFocusMinutes)
        }
    }

    val streak = profile?.streakDays?.coerceAtLeast(0) ?: 0
    val streakText = if (streak == 1) "1 Day" else "$streak Days"
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val todayDateStr = remember { sdf.format(Date()) }
    val isStreakDoneToday = profile?.lastActiveDate == todayDateStr && streak > 0

    val displayedLogs = allLogs
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var selectedMonthDay by rememberSaveable {
        mutableIntStateOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
    }

    val horizontalContentPadding = if (LocalConfiguration.current.screenWidthDp < 360) 12.dp else 20.dp

    AnimatedReveal(index = 0) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = horizontalContentPadding, end = horizontalContentPadding, top = 12.dp, bottom = bottomListPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Your Progress",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "See how your study is building up.",
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
                                "History",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    },
                    modifier = Modifier.testTag("tab_history")
                )
            }
        }

        if (selectedTab == 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("streak_metric_card"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalFireDepartment, "Streak", Modifier.size(28.dp), tint = Color(0xFFFF6D00))
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f)) {
                                Text("STREAK", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(if (streak == 1) "1 Day" else "${streak} Days", fontSize = 28.sp, fontWeight = FontWeight.Black)
                            }
                            if (streakShieldCount > 0) {
                                Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                                    Text("🛡️ ${streakShieldCount}", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp))
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            weeklyData.forEach { day ->
                                val active = day.minutes > 0
                                Surface(
                                    Modifier.weight(1f).height(44.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                        Text(day.label.take(1), fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                            color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(if (active) "✓" else if (day.isToday) "•" else "○", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                            color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                        Text(
                            when {
                                isStreakDoneToday -> "🔥 Streak maintained today"
                                streak > 0 -> "Study today to keep your streak alive"
                                else -> "Complete a study session today to start your streak"
                            },
                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            color = if (isStreakDoneToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                val dailyGoal = (profile?.dailyGoalMinutes ?: 60).coerceAtLeast(1)
                val progress = (todayMinutes.toFloat() / dailyGoal).coerceIn(0f, 1f)
                val animatedDailyProgress by animateFloatAsState(
                    targetValue = progress,
                    animationSpec = tween(750, easing = FastOutSlowInEasing),
                    label = "daily_goal_progress"
                )
                val remaining = (dailyGoal - todayMinutes).coerceAtLeast(0)
                Card(Modifier.fillMaxWidth().testTag("today_goal_analytics_card"), shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text("TODAY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${formatMinutes(todayMinutes)} / ${formatMinutes(dailyGoal)}", fontSize = 22.sp, fontWeight = FontWeight.Black)
                            }
                            Text("${(progress * 100).toInt()}%", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                        LinearProgressIndicator(progress = { animatedDailyProgress }, modifier = Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(5.dp)))
                        Text(if (remaining > 0) "${remaining} min remaining" else "Daily target reached 🎉", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                val maxMinutes = monthlyData.maxOfOrNull { it.minutes } ?: 0
                val firstDayOffset = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }.get(Calendar.DAY_OF_WEEK) - 1
                val cells: List<DayActivityData?> = List(firstDayOffset) { null } + monthlyData
                val selectedDay = monthlyData.firstOrNull { it.dayNumber == selectedMonthDay }
                Card(Modifier.fillMaxWidth().testTag("monthly_activity_card"), shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Study Activity", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    "${SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())} • ${activeMonthDays}/${monthlyData.size} active days",
                                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                                Text("${activeMonthDays}/${monthlyData.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            cells.chunked(7).forEach { week ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    week.forEach { day ->
                                        if (day == null) Box(Modifier.weight(1f).size(28.dp))
                                        else {
                                            val intensity = if (maxMinutes > 0 && day.minutes > 0) (day.minutes.toFloat() / maxMinutes).coerceIn(0.15f, 1f) else 0f
                                            Box(
                                                Modifier.weight(1f).size(28.dp).clip(RoundedCornerShape(7.dp)).clickable { selectedMonthDay = day.dayNumber }
                                                    .background(if (intensity > 0f) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f + 0.70f * intensity) else MaterialTheme.colorScheme.surface)
                                                    .border(
                                                        width = if (day.dayNumber == selectedMonthDay || day.isToday) 2.dp else 1.dp,
                                                        color = if (day.dayNumber == selectedMonthDay) MaterialTheme.colorScheme.primary else if (day.isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                                        shape = RoundedCornerShape(7.dp)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(day.dayNumber.toString(), fontSize = 9.sp,
                                                    fontWeight = if (day.dayNumber == selectedMonthDay || day.isToday) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (intensity > 0.55f) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        selectedDay?.let { day ->
                            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)) {
                                Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                                    Text("Day ${day.dayNumber}${if (day.isToday) " • Today" else ""}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("${formatMinutes(day.minutes)} studied", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Month total: ${formatMinutes(totalMonthMinutes)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Peak: ${formatMinutes(peakMonthDay?.minutes ?: 0)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            item {
                Card(Modifier.fillMaxWidth().testTag("weekly_progress_card"), shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Insights, null, Modifier.size(21.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text("This Week", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text("${formatMinutes(weeklyTotal)} total • ${analytics.averageMinutesOnStudyDays} min / active day", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        weeklyData.forEach { day ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(day.label, Modifier.width(34.dp), fontSize = 11.sp, fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal)
                                Box(Modifier.weight(1f).height(9.dp).clip(RoundedCornerShape(5.dp)).background(MaterialTheme.colorScheme.surface)) {
                                    if (day.minutes > 0) Box(Modifier.fillMaxWidth(day.minutes.toFloat() / maxWeekMinutes).height(9.dp).clip(RoundedCornerShape(5.dp)).background(MaterialTheme.colorScheme.primary))
                                }
                                Text(formatMinutes(day.minutes), Modifier.width(48.dp), fontSize = 10.sp, textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            if (subjectTotals.isNotEmpty()) {
                item {
                    val topSubjects = subjectTotals.take(4)
                    val maxSubjectMinutes = topSubjects.maxOfOrNull { it.value }?.coerceAtLeast(1) ?: 1
                    Card(Modifier.fillMaxWidth().testTag("subject_analytics_card"), shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Subjects", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            topSubjects.forEach { entry ->
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text(entry.key, Modifier.width(78.dp), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                                    Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surface)) {
                                        Box(Modifier.fillMaxWidth(entry.value.toFloat() / maxSubjectMinutes).height(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.primary))
                                    }
                                    Text(formatMinutes(entry.value), Modifier.width(48.dp), fontSize = 10.sp, textAlign = TextAlign.End, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }

            if (analytics.plannedMinutes > 0) {
                item {
                    Card(Modifier.fillMaxWidth().testTag("plan_analytics_card"), shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.School, null, Modifier.size(21.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Study Plan", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text(analytics.activePlanTitle ?: "Study plan", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Text("${analytics.planCompletionPercent}%", fontSize = 17.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            }
                            LinearProgressIndicator(progress = { analytics.planCompletionPercent / 100f }, modifier = Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(5.dp)))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${formatMinutes(analytics.actualMinutes)} completed", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${formatMinutes(analytics.activePlanRemainingMinutes)} remaining", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            item {
                Card(Modifier.fillMaxWidth().testTag("gamification_card"), shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EmojiEvents, "Level", Modifier.size(27.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Level ${currentLevel}", fontSize = 19.sp, fontWeight = FontWeight.Black)
                                Text(currentRankTitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            AnimatedCounter(
                                target = profile?.totalXpEarned ?: 0,
                                suffix = " XP",
                                durationMillis = 700,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            )
                        }
                        missionProgress?.let { mission ->
                            Text("${mission.completedCount}/5 promotion quests complete", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            MissionProgressRow("📘", "Focus", "${mission.studyMinutes} / ${mission.targets.studyMinutesRequired}m", mission.studyMinutes.toFloat() / mission.targets.studyMinutesRequired, mission.studyTimeComplete)
                            MissionProgressRow("⭐", "XP", "${mission.xpEarned} / ${mission.targets.xpRequired}", mission.xpEarned.toFloat() / mission.targets.xpRequired, mission.xpComplete)
                            MissionProgressRow("📚", "Topics", "${mission.topicCount} / ${mission.targets.topicCountRequired}", mission.topicCount.toFloat() / mission.targets.topicCountRequired, mission.topicBreadthComplete)
                            MissionProgressRow("🎯", "Peak Focus", "${mission.peakFocusMinutes} / ${mission.targets.peakFocusMinutesRequired}m", mission.peakFocusMinutes.toFloat() / mission.targets.peakFocusMinutesRequired, mission.peakFocusComplete)
                            MissionProgressRow("🛍️", "Shop Investment", "${mission.xpSpent} / ${mission.targets.xpSpentRequired} XP", mission.xpSpent.toFloat() / mission.targets.xpSpentRequired, mission.shopInvestmentComplete)
                            if (mission.allComplete && currentLevel < 100) {
                                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primary) {
                                    Text("Level ${currentLevel + 1} unlocked! 🎉", Modifier.padding(vertical = 10.dp).fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(Modifier.fillMaxWidth().testTag("quick_stats_card"), shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Quick Stats", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatCell("Study Time", formatMinutes(currentYearMinutes), Modifier.weight(1f))
                            StatCell("Sessions", currentYearSessionCount.toString(), Modifier.weight(1f))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatCell("Current Streak", if (streak == 1) "1 day" else "${streak} days", Modifier.weight(1f))
                            StatCell("Best Session", formatMinutes(bestRecentSession), Modifier.weight(1f))
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
                            text = if (displayedLogs.isNotEmpty()) "Showing " + displayedLogs.size + " sessions • Last 30 days" else "No sessions in the last 30 days",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        "Last 30 days",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
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
                                text = "No study sessions in the last 30 days",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Complete a study session or focus block and it will appear here for 30 days.",
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

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
    }
}

}