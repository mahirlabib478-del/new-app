package com.aistudio.studyos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aistudio.studyos.data.repository.FirebaseAccountRepository
import com.aistudio.studyos.ui.components.tactile3DButton

@Composable
fun ResetPasswordScreen(onBack: () -> Unit) {
    val repository = remember { FirebaseAccountRepository() }
    var email by remember { mutableStateOf(repository.currentUser?.email.orEmpty()) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().tactile3DButton(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f), 18.dp, 2.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(48.dp).tactile3DButton(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.outline.copy(alpha = 0.30f), 24.dp, 3.dp).testTag("btn_reset_back")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to login",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "Account & Security",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        val horizontalContentPadding = if (LocalConfiguration.current.screenWidthDp < 360) 12.dp else 22.dp
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = horizontalContentPadding, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Spacer(Modifier.height(18.dp))
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        ) {
            Icon(
                Icons.Default.MarkEmailRead,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(76.dp).tactile3DButton(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary.copy(alpha = 0.42f), 38.dp, 4.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Reset your password",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "We'll help you get back into your StudyOS account.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(28.dp))
        Card(
            Modifier.fillMaxWidth().tactile3DButton(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), 28.dp, 4.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Password recovery", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Text(
                    "Enter the email address linked to your StudyOS account. If it is registered, we'll send you a secure reset link.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; message = null },
                    modifier = Modifier.fillMaxWidth().testTag("password_reset_email"),
                    label = { Text("Email address") },
                    placeholder = { Text("you@example.com") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                Button(
                    onClick = {
                        busy = true
                        message = null
                        repository.sendPasswordResetEmail(email.trim()) { error ->
                            busy = false
                            message = if (error == null) {
                                "If this email is registered, a reset link has been sent. Check your inbox and Spam/Junk folder."
                            } else {
                                "Could not send the reset email. Check the address and your connection, then try again."
                            }
                        }
                    },
                    enabled = email.contains("@") && !busy,
                    modifier = Modifier.fillMaxWidth().height(56.dp).tactile3DButton(
                        backgroundColor = if (email.contains("@") && !busy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        bottomEdgeColor = if (email.contains("@") && !busy) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.38f),
                        cornerRadius = 18.dp,
                        depth = if (email.contains("@") && !busy) 5.dp else 2.dp
                    ).testTag("btn_send_password_reset"),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(19.dp), strokeWidth = 2.dp) else Text("Send reset link")
                }
                message?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (message != null) {
                    Spacer(Modifier.height(2.dp))
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth().height(54.dp).tactile3DButton(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), 18.dp, 4.dp).testTag("btn_back_to_login"),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text("Back to login")
                    }
                }
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    "Didn't receive the email? Check Spam/Junk, confirm the address is correct, and wait a few minutes before trying again.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Why might it be in Spam? Email providers sometimes filter automated messages, especially when they look unfamiliar, are sent repeatedly, or the sender's domain has limited reputation. This does not necessarily mean the email is unsafe.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            }
        }
    }
}
