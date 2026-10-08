package com.aistudio.studyos.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aistudio.studyos.ui.components.tactile3DButton
import androidx.compose.ui.unit.sp
import com.aistudio.studyos.data.local.entity.StudyPlanEntity
import com.aistudio.studyos.ui.viewmodel.StudyViewModel

@Composable
fun StudyHubScreen(
    viewModel: StudyViewModel,
    onOpenStudy: () -> Unit,
    onOpenQuickFocus: () -> Unit,
    onOpenSavedSessions: () -> Unit,
    onOpenFocus: () -> Unit
) {
    val savedPlans by viewModel.savedPlans.collectAsState()
    val activePlan by viewModel.activePlan.collectAsState()
    val focusState by viewModel.focusState.collectAsState()
    val horizontal = if (LocalConfiguration.current.screenWidthDp < 360) 12.dp else 20.dp
    val bottomPadding = 24.dp

    val inProgressPlans = savedPlans.filter { !it.isDraft && !it.isCompleted && !it.isArchived }
    val draftPlans = savedPlans.filter { it.isDraft && !it.isArchived }
    val completedPlans = savedPlans.filter { it.isCompleted && !it.isArchived }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal, 14.dp, horizontal, bottomPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(Modifier.height(4.dp))
            Text("Study", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
            Text(
                "Plan your session, focus, and track what you actually studied.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            if (activePlan != null) {
                ActivePlanCard(
                    plan = activePlan!!,
                    onContinue = {
                        viewModel.continueActiveSession(activePlan!!)
                        onOpenFocus()
                    }
                )
            } else {
                ReadyCard(onCreate = onOpenStudy, onQuickFocus = onOpenQuickFocus)
            }
        }

        item {
            SectionTitle("Start studying")
        }

        item {
            ActionCard(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                title = "Create Study Plan",
                subtitle = "Choose subjects, set your time, balance topics, and start.",
                testTag = "mode_study",
                onClick = onOpenStudy
            )
        }

        item {
            ActionCard(
                icon = Icons.Default.Bolt,
                title = "Quick Focus",
                subtitle = "Start a 25-minute focus session instantly.",
                testTag = "mode_quick_focus",
                onClick = onOpenQuickFocus
            )
        }

        item {
            SectionTitle("Your plans")
        }

        item {
            Card(
                Modifier.fillMaxWidth().testTag("saved_sessions_card").clickable { onOpenSavedSessions() }.tactile3DButton(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.outline.copy(alpha = 0.48f), 20.dp, 5.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(17.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Bookmark, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Saved Sessions", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(
                            when {
                                draftPlans.isNotEmpty() -> "${draftPlans.size} draft${if (draftPlans.size == 1) "" else "s"} ready"
                                inProgressPlans.isNotEmpty() -> "${inProgressPlans.size} plan${if (inProgressPlans.size == 1) "" else "s"} in progress"
                                else -> "View and manage your saved plans"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (inProgressPlans.isNotEmpty()) {
            item { SectionTitle("Continue") }
            items(inProgressPlans.take(3).size) { index ->
                PlanPreviewCard(
                    plan = inProgressPlans[index],
                    status = "In Progress",
                    onClick = {
                        viewModel.resumeSavedPlan(inProgressPlans[index], onOpenFocus)
                    }
                )
            }
        }

        if (draftPlans.isNotEmpty()) {
            item { SectionTitle("Drafts") }
            items(draftPlans.take(3).size) { index ->
                PlanPreviewCard(
                    plan = draftPlans[index],
                    status = "Draft",
                    onClick = onOpenSavedSessions
                )
            }
        }

        if (completedPlans.isNotEmpty()) {
            item { SectionTitle("Recently completed") }
            items(completedPlans.take(2).size) { index ->
                PlanPreviewCard(
                    plan = completedPlans[index],
                    status = "Completed",
                    onClick = onOpenSavedSessions
                )
            }
        }
    }
}

@Composable
private fun ActivePlanCard(plan: StudyPlanEntity, onContinue: () -> Unit) {
    val total = plan.totalDurationMinutes.coerceAtLeast(1)
    val studied = plan.accumulatedBillableMinutes.coerceIn(0, total)
    val progress = studied.toFloat() / total.toFloat()
    val remaining = (total - studied).coerceAtLeast(0)

    Card(
        Modifier.fillMaxWidth().clickable(onClick = onContinue).tactile3DButton(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary.copy(alpha = 0.52f), 24.dp, 5.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text("Continue studying", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                    Text(plan.title, style = MaterialTheme.typography.bodySmall)
                }
                Text("${(progress * 100).toInt()}%", fontWeight = FontWeight.ExtraBold)
            }
            LinearProgressIndicator(
                progress = { progress },
                Modifier.fillMaxWidth().height(8.dp)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Block ${(plan.currentBlockIndex + 1).coerceAtMost(plan.totalBlocks)}/${plan.totalBlocks}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text("${formatPlanMinutes(remaining)} remaining", style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_continue_current_plan")
                    .tactile3DButton(
                        backgroundColor = MaterialTheme.colorScheme.primary,
                        bottomEdgeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.62f),
                        cornerRadius = 16.dp,
                        depth = 5.dp
                    ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Continue")
            }
        }
    }
}

@Composable
private fun ReadyCard(onCreate: () -> Unit, onQuickFocus: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().tactile3DButton(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.outline.copy(alpha = 0.48f), 24.dp, 6.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Ready to study?", fontWeight = FontWeight.ExtraBold, fontSize = 21.sp)
            Text("Create a plan or jump straight into focus.", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onCreate, modifier = Modifier
                    .weight(1f)
                    .tactile3DButton(
                        backgroundColor = MaterialTheme.colorScheme.primary,
                        bottomEdgeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.62f),
                        cornerRadius = 14.dp,
                        depth = 4.dp
                    ), shape = RoundedCornerShape(14.dp)) {
                    Text("Create Plan")
                }
                OutlinedButton(
                    onClick = onQuickFocus,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Quick Focus")
                }
            }
        }
    }
}

@Composable
private fun ActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth().testTag(testTag).clickable(onClick = onClick).tactile3DButton(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.outline.copy(alpha = 0.48f), 20.dp, 5.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Spacer(Modifier.height(3.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PlanPreviewCard(plan: StudyPlanEntity, status: String, onClick: () -> Unit) {
    val total = plan.totalDurationMinutes.coerceAtLeast(1)
    val progress = if (plan.isCompleted) 1f else (plan.accumulatedBillableMinutes.toFloat() / total).coerceIn(0f, 1f)
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick).tactile3DButton(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.outline.copy(alpha = 0.42f), 18.dp, 4.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(plan.title, fontWeight = FontWeight.Bold)
                    Text(
                        "${formatPlanMinutes(total)} • ${plan.totalBlocks} blocks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    status,
                    color = if (status == "Completed") MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                Modifier.fillMaxWidth().height(6.dp)
            )
            Text(
                when {
                    status == "Completed" -> "✓ Completed"
                    progress > 0f -> "${(progress * 100).toInt()}% complete • ${formatPlanMinutes((total * (1f - progress)).toInt())} remaining"
                    else -> "Not started"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
}

private fun formatPlanMinutes(minutes: Int): String {
    val safe = minutes.coerceAtLeast(0)
    val hours = safe / 60
    val mins = safe % 60
    return when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        else -> "${mins}m"
    }
}
