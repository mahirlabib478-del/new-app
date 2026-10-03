package com.aistudio.studyos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aistudio.studyos.data.repository.FirebaseAccountRepository

@Composable
fun ResetPasswordScreen(onBack: () -> Unit) {
    val repository = remember { FirebaseAccountRepository() }
    var email by remember { mutableStateOf(repository.currentUser?.email.orEmpty()) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    Column(
        Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("Reset Password", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(20.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.Default.MarkEmailRead, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
                Text("Forgot your password?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Enter the email address linked to your StudyOS account. We'll send a password reset link if the account is registered.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; message = null },
                    modifier = Modifier.fillMaxWidth().testTag("password_reset_email"),
                    label = { Text("Email address") },
                    placeholder = { Text("you@example.com") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                Button(
                    onClick = {
                        busy = true
                        message = null
                        repository.sendPasswordResetEmail(email.trim()) { error ->
                            busy = false
                            message = if (error == null) "If this email is registered, a reset link has been sent." else "Could not send reset link. Check the email and try again."
                        }
                    },
                    enabled = email.contains("@") && !busy,
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("btn_send_password_reset"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(19.dp), strokeWidth = 2.dp) else Text("Send Reset Link")
                }
                message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}
