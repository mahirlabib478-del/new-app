package com.aistudio.studyos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.aistudio.studyos.StudyApplication
import com.aistudio.studyos.data.repository.FirebaseAccountRepository
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(onBack: () -> Unit, onForgotPassword: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as StudyApplication
    val repository = remember { FirebaseAccountRepository() }
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf(repository.currentUser?.email.orEmpty()) }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var confirmAction by remember { mutableStateOf<String?>(null) }
    var confirmBackup by remember { mutableStateOf(false) }
    val user = repository.currentUser

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Column {
                Text("StudyOS Account", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Sign in, create account or recover access", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(22.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(if (user == null) "Welcome back" else "Account connected", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                if (user != null) {
                    Text(user.email.orEmpty(), style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = { confirmBackup = true }, enabled = !busy, modifier = Modifier.fillMaxWidth().height(50.dp).testTag("btn_link_local_progress")) { Text("Back Up This Device") }
                    OutlinedButton(onClick = { repository.signOut(); message = "Signed out."; password = "" }, modifier = Modifier.fillMaxWidth()) { Text("Sign Out") }
                } else {
                    OutlinedTextField(email, { email = it; message = null }, Modifier.fillMaxWidth().testTag("account_email"), label = { Text("Email address") }, placeholder = { Text("you@example.com") }, singleLine = true, shape = RoundedCornerShape(14.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                    OutlinedTextField(password, { password = it; message = null }, Modifier.fillMaxWidth().testTag("account_password"), label = { Text("Password") }, singleLine = true, shape = RoundedCornerShape(14.dp), visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { confirmAction = "login" }, enabled = !busy && email.contains("@") && password.isNotEmpty(), modifier = Modifier.weight(1f).height(50.dp).testTag("btn_login"), shape = RoundedCornerShape(14.dp)) { Text("Log In") }
                        OutlinedButton(onClick = { confirmAction = "create" }, enabled = !busy && email.contains("@") && password.length >= 6, modifier = Modifier.weight(1f).height(50.dp).testTag("btn_create_account"), shape = RoundedCornerShape(14.dp)) { Text("Create Account") }
                    }
                    Text("New account password must contain at least 6 characters.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (busy) CircularProgressIndicator(Modifier.size(22.dp))
                message?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
            }
        }
        TextButton(onClick = onForgotPassword, modifier = Modifier.fillMaxWidth().testTag("btn_forgot_password")) {
            Icon(Icons.Default.LockReset, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Forgot password?")
        }
    }
    if (confirmAction != null) AlertDialog(onDismissRequest = { confirmAction = null }, title = { Text("Local study data notice") }, text = { Text("Study records are stored locally on this device and are not yet separated by account. Continuing may leave existing records visible after sign-in.") }, confirmButton = {
        Button(onClick = {
            val action = confirmAction ?: return@Button
            confirmAction = null; busy = true; message = null
            scope.launch {
                try {
                    if (action == "login") repository.signIn(email, password) else repository.createAccount(email, password)
                    message = (if (action == "login") "Signed in. " else "Account created. ") + app.cloudProgressSync.restoreIfLocalEmpty()
                } catch (e: Exception) { message = e.localizedMessage ?: "Account action failed." }
                finally { busy = false }
            }
        }) { Text("Continue") }
    }, dismissButton = { TextButton(onClick = { confirmAction = null }) { Text("Cancel") } })
    if (confirmBackup) AlertDialog(onDismissRequest = { confirmBackup = false }, title = { Text("Back up this device?") }, text = { Text("Upload the study records currently stored on this device to the signed-in account's private cloud backup?") }, confirmButton = {
        Button(onClick = {
            confirmBackup = false; busy = true
            scope.launch {
                try { app.cloudProgressSync.linkLocalProgressToCurrentAccount(); message = "Device progress backed up and linked." }
                catch (e: Exception) { message = e.localizedMessage ?: "Backup failed." }
                finally { busy = false }
            }
        }) { Text("Upload & Link") }
    }, dismissButton = { TextButton(onClick = { confirmBackup = false }) { Text("Cancel") } })
}
