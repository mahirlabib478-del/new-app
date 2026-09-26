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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.studyos.data.local.entity.SessionLogEntity
import com.aistudio.studyos.ui.viewmodel.StudyViewModel
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
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
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
    val now = Calendar.getInstance()
    val todayYear = now.get(Calendar.YEAR)
    val todayDay = now.get(Calendar.DAY_OF_YEAR)
    val cal = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -29)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val result = mutableListOf<DayActivityData>()
    repeat(30) {
        val start = cal.timeInMillis
        val dayYear = cal.get(Calendar.YEAR)
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val end = cal.timeInMillis
        val minutes = logs.filter { it.timestamp in start until end }.sumOf { it.durationMinutes.coerceAtLeast(0) }
        result += DayActivityData(
            dayName = dayFormat.format(Date(start)),
            dayNumber = Calendar.getInstance().apply { timeInMillis = start }.get(Calendar.DAY_OF_MONTH),
            minutes = minutes,
            isToday = dayYear == todayYear && dayOfYear == todayDay
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
    val monthlyData = remember(allLogs) { calculateMonthlyActivity(allLogs) }
    val totalMonthMinutes = remember(monthlyData) { monthlyData.sumOf { it.minutes } }
    val activeMonthDays = remember(monthlyData) { monthlyData.count { it.minutes > 0 } }
    val peakMonthDay = remember(monthlyData) { monthlyData.maxByOrNull { it.minutes } }
    val currentYearMinutes by viewModel.currentYearMinutes.collectAsState()
    val currentYearSessionCount by viewModel.currentYearSessionCount.collectAsState()
    val topicCount by viewModel.distinctStudyTopicCount.collectAsState()
    val peakDailyFocusMinutes by viewModel.peakDailyFocusMinutes.collectAsState()
    val activePlan by viewModel.activePlan.collectAsState()
    val latestCompletedPlan by viewModel.latestCompletedPlan.collectAsState()
    val analytics = remember(allLogs, activePlan, latestCompletedPlan) {
        ProgressAnalyticsCalculator.calculate(allLogs, activePlan, latestCompletedPlan = latestCompletedPlan)
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

    val streak = profile?.streakDays ?: 0
    val streakText = if (streak == 1) "1 Day" else "$streak Days"
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val todayDateStr = remember { sdf.format(Date()) }
    val isStreakDoneToday = profile?.lastActiveDate == todayDateStr && streak > 0

    val displayedLogs = allLogs
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

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
                                text = "Total Time (" + Calendar.getInstance().get(Calendar.YEAR) + ")",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (currentYearMinutes / 60 > 0) (currentYearMinutes / 60).toString() + "h " + (currentYearMinutes % 60) + "m" else currentYearMinutes.toString() + "m",
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Jan 1 – Present • " + currentYearSessionCount + " Sessions in " + Calendar.getInstance().get(Calendar.YEAR),
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

                                        // 2. Monthly consistency snapshot
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    activeMonthDays.toString() + "/30 Active Days",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "study activity in the last 30 days",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    if (totalMonthMinutes >= 60) (totalMonthMinutes / 60).toString() + "h " + (totalMonthMinutes % 60) + "m" else totalMonthMinutes.toString() + "m",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "monthly total",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            "Peak day: " + (peakMonthDay?.let { it.dayName + " " + it.dayNumber + " • " + it.minutes + "m" } ?: "No study yet"),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    }
                }
            }
        }
        item {
            val mission = missionProgress
            if (mission != null) {
                val rank = LevelMissionCalculator.rankTitle(currentLevel)
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("current_level_missions_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Level " + currentLevel + " Promotion Quests", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(rank + " • " + mission.completedCount + "/5 missions complete", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                                Text("Lv " + currentLevel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
                            }
                        }
                        MissionProgressRow("📘", "Total Focus", mission.studyMinutes.toString() + "m / " + mission.targets.studyMinutesRequired + "m", mission.studyMinutes.toFloat() / mission.targets.studyMinutesRequired, mission.studyTimeComplete)
                        MissionProgressRow("⭐", "Total XP", mission.xpEarned.toString() + " / " + mission.targets.xpRequired + " XP", mission.xpEarned.toFloat() / mission.targets.xpRequired, mission.xpComplete)
                        MissionProgressRow("📚", "Topic Breadth", mission.topicCount.toString() + " / " + mission.targets.topicCountRequired + " Topics Studied", mission.topicCount.toFloat() / mission.targets.topicCountRequired, mission.topicBreadthComplete)
                        MissionProgressRow("🔥", "Day Peak Focus", mission.peakFocusMinutes.toString() + "m / " + mission.targets.peakFocusMinutesRequired + "m", mission.peakFocusMinutes.toFloat() / mission.targets.peakFocusMinutesRequired, mission.peakFocusComplete)
                        MissionProgressRow("🛍️", "Shop Investment", mission.xpSpent.toString() + " / " + mission.targets.xpSpentRequired + " XP Spent", mission.xpSpent.toFloat() / mission.targets.xpSpentRequired, mission.shopInvestmentComplete)
                        if (mission.allComplete) {
                            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primary) {
                                Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Level " + (currentLevel + 1) + " unlocked!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("gamification_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = "Level", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Level " + currentLevel + " • " + currentRankTitle, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            (totalXP + (profile?.totalXpSpent ?: 0)).toString() + " lifetime XP earned • " + (profile?.totalXpSpent ?: 0) + " XP invested",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }

// Monthly Activity Visualizer
        item {
            val maxMinutes = monthlyData.maxOfOrNull { it.minutes } ?: 0
            val monthHours = totalMonthMinutes / 60
            val monthMinutes = totalMonthMinutes % 60
            val cells: List<DayActivityData?> = List(5) { null }.take(35 - monthlyData.size) + monthlyData
            Card(
                modifier = Modifier.fillMaxWidth().testTag("monthly_activity_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Monthly Activity Pattern", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                if (monthHours > 0) monthHours.toString() + " hrs " + monthMinutes + " mins • " + activeMonthDays + "/30 active days"
                                else activeMonthDays.toString() + "/30 active days • Start your monthly streak",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                            Text(activeMonthDays.toString() + "/30", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        cells.chunked(7).forEach { week ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                week.forEach { day ->
                                    if (day == null) {
                                        Box(Modifier.weight(1f).size(26.dp))
                                    } else {
                                        val intensity = if (maxMinutes > 0 && day.minutes > 0) (day.minutes.toFloat() / maxMinutes).coerceIn(0.15f, 1f) else 0f
                                        Box(
                                            Modifier.weight(1f).size(26.dp).clip(RoundedCornerShape(6.dp))
                                                .background(if (intensity > 0f) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f + 0.70f * intensity) else MaterialTheme.colorScheme.surface)
                                                .border(
                                                    width = if (day.isToday) 2.dp else 1.dp,
                                                    color = if (day.isToday) MaterialTheme.colorScheme.primary else if (intensity > 0f) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(day.dayNumber.toString(), fontSize = 8.sp, fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal, color = if (intensity > 0.55f) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "Peak: " + (peakMonthDay?.let { if (it.minutes >= 60) (it.minutes / 60).toString() + "h " + (it.minutes % 60) + "m" else it.minutes.toString() + "m" } ?: "0m"),
                            fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("Today highlighted", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
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
