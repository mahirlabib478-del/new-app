package com.aistudio.studyos.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.studyos.ui.viewmodel.StudyViewModel

private val PRESET_SUBJECTS = listOf("Mathematics", "Physics", "Computer Science", "Biology", "Chemistry", "Literature")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegularStudyScreen(
    viewModel: StudyViewModel,
    onBack: () -> Unit,
    onStartFocus: () -> Unit
) {
    var subject by remember { mutableStateOf("Mathematics") }
    var chapter by remember { mutableStateOf("Calculus & Derivatives") }
    var selectedDuration by remember { mutableIntStateOf(25) }
    var selectedBreak by remember { mutableIntStateOf(5) }
    var totalBlocks by remember { mutableIntStateOf(4) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Regular Study Setup", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Select or Enter Subject",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_study_subject"),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PRESET_SUBJECTS.take(3).forEach { sub ->
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { subject = sub },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (subject == sub)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = sub,
                                    fontSize = 12.sp,
                                    fontWeight = if (subject == sub) FontWeight.Bold else FontWeight.Normal,
                                    color = if (subject == sub) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Chapter or Focus Topic",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = chapter,
                    onValueChange = { chapter = it },
                    label = { Text("Chapter / Focus Details") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_study_chapter"),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
            }

            // Block Duration Selection
            item {
                Text(
                    text = "Focus Block Duration",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        15 to "Quick (15m)",
                        25 to "Pomodoro (25m)",
                        50 to "Deep (50m)"
                    ).forEach { (mins, label) ->
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedDuration = mins },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedDuration == mins)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${mins}m",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = if (selectedDuration == mins)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = label.split(" ")[0],
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Number of Blocks Selection
            item {
                Text(
                    text = "Total Focus Blocks",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(1, 2, 4, 6).forEach { blocks ->
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { totalBlocks = blocks },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (totalBlocks == blocks)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$blocks Blocks",
                                    fontWeight = if (totalBlocks == blocks) FontWeight.ExtraBold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = if (totalBlocks == blocks)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Action Buttons
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val finalSubject = subject.ifBlank { "General Study" }
                        val finalChapter = chapter.ifBlank { "Chapter Review" }
                        viewModel.startNewPlan(
                            title = "$finalSubject: $finalChapter",
                            subject = finalSubject,
                            chapter = finalChapter,
                            mode = "regular",
                            totalBlocks = totalBlocks,
                            blockMinutes = selectedDuration,
                            breakMinutes = selectedBreak
                        )
                        onStartFocus()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_start_study_session"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start Session (${selectedDuration * totalBlocks} mins total)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        val finalSubject = subject.ifBlank { "General Study" }
                        val finalChapter = chapter.ifBlank { "Chapter Review" }
                        viewModel.saveDraftPlan(
                            title = "$finalSubject: $finalChapter (Draft)",
                            subject = finalSubject,
                            chapter = finalChapter,
                            mode = "regular",
                            totalBlocks = totalBlocks,
                            blockMinutes = selectedDuration,
                            breakMinutes = selectedBreak
                        )
                        onBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_save_study_draft"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save to Draft Sessions", fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
