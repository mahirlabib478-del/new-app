package com.aistudio.studyos.ui.screens

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
    val totalXP = profile?.totalXP ?: 0
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
                HomeHeader(streak = streak, level = level, totalXP = totalXP)
            }
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
                modifier = Modifier
                    .weight(1f)
                    .animateContentSize(animationSpec = tween(260, easing = FastOutSlowInEasing)),
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
                Text(
                    text = todayMinutes.toString() + " / " + dailyGoal + " min",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (goalComplete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            val animatedGoalProgress by animateFloatAsState(
                targetValue = progressFraction,
                animationSpec = tween(300, easing = FastOutSlowInEasing),
                label = "today_goal_progress"
            )
            LinearProgressIndicator(
                progress = { animatedGoalProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .testTag("today_goal_progress"),
                color = if (goalComplete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = actionLabel,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

