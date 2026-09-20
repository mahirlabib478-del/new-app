package com.aistudio.studyos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.studyos.data.local.entity.SessionLogEntity
import com.aistudio.studyos.ui.viewmodel.StudyViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HistoryDay(val key: String, val label: String, val minutes: Int, val sessions: Int, val logs: List<SessionLogEntity>)

fun buildHistoryDays(logs: List<SessionLogEntity>, locale: Locale = Locale.getDefault()): List<HistoryDay> {
    val keyFormat = SimpleDateFormat("yyyy-MM-dd", locale)
    val labelFormat = SimpleDateFormat("EEEE, MMM d", locale)
    return logs.sortedByDescending { it.timestamp }.groupBy { keyFormat.format(Date(it.timestamp)) }.map { (key, dayLogs) ->
        HistoryDay(key, labelFormat.format(Date(dayLogs.maxOf { it.timestamp })), dayLogs.sumOf { it.durationMinutes }, dayLogs.size, dayLogs.sortedByDescending { it.timestamp })
    }
}

@Composable
fun HistoryScreen(viewModel: StudyViewModel, onBack: () -> Unit) {
    val allLogs by viewModel.allLogs.collectAsState()
    val days = buildHistoryDays(allLogs)
    val totalMinutes = allLogs.sumOf { it.durationMinutes }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("history_back")) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            Column(Modifier.weight(1f)) {
                Text("Study History", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text(if (days.isEmpty()) "No sessions yet" else "${days.size} days • $totalMinutes min total", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(28.dp))
        }
        if (days.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(52.dp))
                Spacer(Modifier.height(12.dp))
                Text("Your study timeline is empty", fontWeight = FontWeight.Bold)
                Text("Complete a study session and it will appear here grouped by day.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(days, key = { it.key }) { day ->
                    Card(Modifier.fillMaxWidth().testTag("history_day_${day.key}"), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column { Text(day.label, fontWeight = FontWeight.Bold); Text("${day.sessions} ${if (day.sessions == 1) "session" else "sessions"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                Text("${day.minutes} min", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            }
                            day.logs.forEach { log ->
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(log.subject.ifBlank { "Study Session" }, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(log.chapter.ifBlank { "General Practice" }, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Surface(shape = RoundedCornerShape(7.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = .10f)) { Text("${log.durationMinutes}m", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)) }
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(70.dp)) }
            }
        }
    }
}