package com.aistudio.studyos.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.studyos.data.repository.TodayRecommendationCalculator
import com.aistudio.studyos.ui.components.XPShopBottomSheet
import com.aistudio.studyos.ui.viewmodel.StudyViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val THEME_PRESET_LIST = listOf(
    Triple("pitch_black", "Pitch Black", Color(0xFF00E5FF)),
    Triple("obsidian_gold", "Obsidian Gold", Color(0xFFFFD60A)),
    Triple("light", "Light", Color(0xFF2563EB)),
    Triple("sky_night", "Sky Night", Color(0xFF38BDF8)),
    Triple("learning_green", "Learning Green", Color(0xFF4CAF00)),
    Triple("sunrise", "Sunrise Orange", Color(0xFFEA580C)),
    Triple("cyberpunk", "Cyberpunk / Synthwave 80s", Color(0xFFFF2A85)),
    Triple("cyber_runner", "Mirror's Edge / Cyber Runner", Color(0xFFEF4444)),
)

private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = (now - timestamp).coerceAtLeast(0)
    val minutes = diff / (60 * 1000)
    val hours = minutes / 60
    val days = hours / 24

    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

    val nowCal = Calendar.getInstance().apply { timeInMillis = now }
    val logCal = Calendar.getInstance().apply { timeInMillis = timestamp }

    val isToday = nowCal.get(Calendar.YEAR) == logCal.get(Calendar.YEAR) &&
            nowCal.get(Calendar.DAY_OF_YEAR) == logCal.get(Calendar.DAY_OF_YEAR)

    val yesterdayCal = Calendar.getInstance().apply {
        timeInMillis = now
        add(Calendar.DAY_OF_YEAR, -1)
    }
    val isYesterday = yesterdayCal.get(Calendar.YEAR) == logCal.get(Calendar.YEAR) &&
            yesterdayCal.get(Calendar.DAY_OF_YEAR) == logCal.get(Calendar.DAY_OF_YEAR)

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        isToday -> "Today, ${timeFormat.format(Date(timestamp))}"
        isYesterday -> "Yesterday, ${timeFormat.format(Date(timestamp))}"
        days < 7 -> "${days}d ago"
        else -> dateFormat.format(Date(timestamp))
    }
}

private fun getSessionModeLabel(mode: String): String {
    return when (mode.lowercase()) {
        "focus", "quick" -> "🎯 25m Focus"
        "exam" -> "📝 Exam"
        "cram" -> "⚡ Cram"
        "early_finish" -> "⏱️ Quick Session"
        "regular" -> "📖 Regular"
        else -> "📚 Study"
    }
}

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

    val streak = profile?.streakDays ?: 0
    val totalMinutes = profile?.totalStudyMinutes ?: 0
    val dailyGoal = profile?.dailyGoalMinutes ?: 60

    val todayMinutes by viewModel.todayMinutes.collectAsState()
    val progressFraction = if (dailyGoal > 0) todayMinutes.toFloat() / dailyGoal else 0f
    val todayRecommendation = TodayRecommendationCalculator.calculate(
        activePlan = activePlan,
        upcomingExams = upcomingExams,
        todayMinutes = todayMinutes,
        dailyGoalMinutes = dailyGoal
    )

    var showThemeDialog by remember { mutableStateOf(false) }
    var showXPShop by remember { mutableStateOf(false) }
    val streakShieldCount by viewModel.streakShieldCount.collectAsState()
    val shieldSavedNotice by viewModel.shieldSavedNotice.collectAsState()
    val isBoosterActive by viewModel.isDoubleXpBoosterActive.collectAsState()

    val focusState by viewModel.focusState.collectAsState()
    val bottomListPadding = if (focusState.planId != null) 150.dp else 96.dp

    if (showXPShop) {
        XPShopBottomSheet(
            viewModel = viewModel,
            onDismiss = { showXPShop = false }
        )
    }

    val horizontalContentPadding = if (LocalConfiguration.current.screenWidthDp < 360) 12.dp else 20.dp

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = horizontalContentPadding, end = horizontalContentPadding, top = 12.dp, bottom = bottomListPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))

            // App Header & Gamification Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Study OS",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Offline-first companion",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showThemeDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("btn_quick_theme")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Change Theme",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))

                    // XP Perks / Shop Button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isBoosterActive) Color(0xFFF59E0B).copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { showXPShop = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("btn_open_xp_shop"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (streakShieldCount > 0) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Shields",
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "$streakShieldCount",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Shop",
                            tint = if (isBoosterActive) Color(0xFFF59E0B) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = if (isBoosterActive) "2X XP" else "Shop",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isBoosterActive) Color(0xFFF59E0B) else MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))

                    // Streak & Level Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { showXPShop = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = Color(0xFFF97316),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$streak d",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Lv ${profile?.currentLevel ?: 1}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (shieldSavedNotice != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shield_saved_streak_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A)),
                    border = BorderStroke(1.dp, Color(0xFF60A5FA).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("🛡️", fontSize = 24.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Streak Shield Activated!",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                "You missed studying yesterday, but your Streak Shield saved your $streak-day streak! Study today to keep it burning.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFDBEAFE),
                                lineHeight = 16.sp
                            )
                        }
                        IconButton(
                            onClick = { viewModel.dismissShieldNotice() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.White)
                        }
                    }
                }
            }
        }

        // Hero Today Engine Card
        item {
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
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrackChanges,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Today Engine",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = todayRecommendation.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }

                        if (activePlan != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Block ${activePlan!!.currentBlockIndex + 1}/${activePlan!!.totalBlocks}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = todayRecommendation.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Daily Progress Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Daily Goal Progress",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${todayMinutes} / ${dailyGoal}m",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progressFraction.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (todayRecommendation.shouldOpenFocus && activePlan != null) {
                                viewModel.continueActiveSession(activePlan!!)
                                onOpenFocus()
                            } else {
                                onOpenStudy()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("start_study_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = todayRecommendation.actionLabel,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Fast Action Buttons Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_quick_study")
                        .clickable { onOpenStudy() },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Study",
                            modifier = Modifier.fillMaxWidth(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Plan your study",
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_quick_focus")
                        .clickable { onOpenQuickFocus() },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Quick Focus",
                            modifier = Modifier.fillMaxWidth(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "25-minute Pomodoro",
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Upcoming Exam Spotlight
        item {
            val nextExam = upcomingExams.firstOrNull()
            if (nextExam != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("spotlight_upcoming_exam_card")
                        .clickable { onOpenExamPlanner() },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFEF4444).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = nextExam.subject,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (nextExam.daysRemaining > 0) "${nextExam.daysRemaining}d left" else nextExam.examDate,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFEF4444)
                                        )
                                    }
                            }
                            Text(
                                text = nextExam.syllabusTopics.ifBlank { "Target revision session" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Recent Completed Sessions
           // Compact seven-day activity heatmap with current streak.
        item {
            val activityDays = remember(allLogs) {
                (6 downTo 0).map { daysAgo ->
                    val startCalendar = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        add(Calendar.DAY_OF_YEAR, -daysAgo)
                    }
                    val startMillis = startCalendar.timeInMillis
                    val endMillis = (startCalendar.clone() as Calendar).apply {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }.timeInMillis
                    val dayLogs = allLogs.filter { it.timestamp >= startMillis && it.timestamp < endMillis }
                    Triple(
                        SimpleDateFormat("EEE", Locale.getDefault()).format(Date(startMillis)),
                        dayLogs.sumOf { it.durationMinutes },
                        dayLogs.size
                    )
                }
            }
            var selectedActivityDay by remember { mutableStateOf(6) }
            val selectedDay = activityDays.getOrNull(selectedActivityDay)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("study_activity_heatmap_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Study Activity",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Your last 7 days",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = onOpenHistory,
                            modifier = Modifier.testTag("btn_activity_view_history")
                        ) {
                            Text("History", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        activityDays.forEachIndexed { index, day ->
                            val isSelected = selectedActivityDay == index
                            val cellColor = when {
                                day.second <= 0 -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                                day.second < 15 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                day.second < 30 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.48f)
                                day.second < 60 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
                                else -> MaterialTheme.colorScheme.primary
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedActivityDay = index }
                                    .padding(vertical = 3.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(cellColor)
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = RoundedCornerShape(9.dp)
                                        )
                                )
                                Text(
                                    text = day.first,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    if (selectedDay != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (selectedActivityDay == 6) "Today" else selectedDay.first,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${selectedDay.second} min  •  ${selectedDay.third} session${if (selectedDay.third == 1) "" else "s"}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    androidx.compose.material3.HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onOpenHistory() }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(Color(0x26F97316)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color(0xFFF97316),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "$streak-Day Streak",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tap to explore your study history",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Open study history",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

er = Modifier.height(80.dp))
        }
    }

    if (showThemeDialog) {
        val currentPreset by viewModel.currentTheme.collectAsState()

        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Theme Preset", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    THEME_PRESET_LIST.forEach { (key, label, accentColor) ->
                        val isSelected = currentPreset == key
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setTheme(key)
                                    showThemeDialog = false
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(accentColor)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
