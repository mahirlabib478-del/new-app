package com.aistudio.studyos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.aistudio.studyos.data.update.AppUpdateInfo
import com.aistudio.studyos.data.update.UpdateDownloadState
import com.aistudio.studyos.data.update.UpdateManager

@Composable
fun UpdateDialog(
    currentVersion: String,
    updateInfo: AppUpdateInfo,
    onUpdateClick: (url: String) -> Unit,
    onDismiss: () -> Unit
) {
    val downloadState by UpdateManager.downloadState.collectAsState()
    AlertDialog(
        onDismissRequest = {
            if (!updateInfo.isMandatory) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !updateInfo.isMandatory,
            dismissOnClickOutside = !updateInfo.isMandatory
        ),
        modifier = Modifier.testTag("update_dialog"),
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header badge icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Update Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "New Update Available!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "A newer version of Study OS was published on GitHub. Update now for improved performance, fixes, and features.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Version comparison pills
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Current Version",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "v$currentVersion",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "New Version",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "v${updateInfo.latestVersion}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                if (updateInfo.releaseNotes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = updateInfo.releaseNotes.take(160) + if (updateInfo.releaseNotes.length > 160) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Primary Update Button
                Button(
                    onClick = {
                        if (downloadState !is UpdateDownloadState.Downloading) {
                            val targetUrl = updateInfo.apkUrl.ifBlank { updateInfo.releaseUrl }
                            onUpdateClick(targetUrl)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("update_now_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (downloadState) {
                            is UpdateDownloadState.Downloading -> {
                                val p = (downloadState as UpdateDownloadState.Downloading).progressPercent
                                if (p >= 0) "Downloading update... $p%" else "Downloading update..."
                            }
                            UpdateDownloadState.ReadyToInstall -> "Opening installer..."
                            is UpdateDownloadState.Error -> "Retry Download"
                            else -> "Download APK / Update Now"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // Secondary GitHub Release Button
                if (updateInfo.releaseUrl.isNotBlank() && updateInfo.releaseUrl != updateInfo.apkUrl) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onUpdateClick(updateInfo.releaseUrl) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("view_release_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "View Release Notes on GitHub",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Dismiss / Later Button
                if (!updateInfo.isMandatory) {
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .testTag("update_later_btn")
                    ) {
                        Text(
                            text = "Later",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}
