package com.aistudio.studyos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.aistudio.studyos.data.repository.EmailNotVerifiedException
import com.aistudio.studyos.data.repository.FirebaseAccountRepository
import com.aistudio.studyos.data.repository.LegacyProgressImportRepository
import com.aistudio.studyos.StudyApplication
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(
    onBack: () -> Unit,
    onForgotPassword: () -> Unit,
    onVerified: () -> Unit = {},
    onContinueAsGuest: () -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { FirebaseAccountRepository() }
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf(repository.currentUser?.email.orEmpty()) }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var confirmAction by remember { mutableStateOf<String?>(null) }
    var confirmImport by remember { mutableStateOf(false) }
    var showAdvancedSync by remember { mutableStateOf(false) }
    var verificationPending by remember { mutableStateOf(repository.currentUser?.isEmailVerified == false) }
    val user = repository.currentUser
    val verified = user?.isEmailVerified == true
    val cloudSync = remember(user?.uid) {
        user?.uid?.let { (context.applicationContext as StudyApplication).cloudSyncFor(it) }
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!verificationPending && user == null) {
            Spacer(Modifier.height(18.dp))
            Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
            Spacer(Modifier.height(10.dp))
            Text("StudyOS", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
            Text("Your personal study space", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(if (verified) "Your StudyOS account" else "Verify your email", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(if (verified) "Your study data is ready and private to your account." else "Verify once to keep your study progress linked to your account.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(28.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        when {
                            verified -> "Account verified"
                            verificationPending || user != null -> "Email verification required"
                            else -> "Welcome to StudyOS"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (verified) {
                    Text(user?.email.orEmpty(), style = MaterialTheme.typography.bodyLarge)
                    Text("You're all set. Your personal study space is ready.", style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(
                        onClick = { confirmImport = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy
                    ) { Text("Import old local progress") }
                    Text(
                        "Only import if these records belong to you. Nothing is copied unless you confirm.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = { showAdvancedSync = !showAdvancedSync }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.CloudDone, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (showAdvancedSync) "Hide backup options" else "Backup & restore options")
                    }
                    if (showAdvancedSync) {
                    Text(
                        "Create the first backup once to enable automatic cloud uploads on this device. After that, changes to study plans, exams, sessions and profile progress are uploaded automatically; restore is only for moving to an empty device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = {
                            busy = true
                            message = null
                            scope.launch {
                                try {
                                    val sync = cloudSync ?: throw IllegalStateException("Account sync is unavailable.")
                                    message = sync.restoreIfLocalEmpty()
                                } catch (e: Exception) {
                                    message = e.localizedMessage ?: "Cloud restore failed."
                                } finally { busy = false }
                            }
                        },
                        enabled = !busy && verified,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Restore cloud backup") }
                    Button(
                        onClick = {
                            busy = true
                            message = null
                            scope.launch {
                                try {
                                    val sync = cloudSync ?: throw IllegalStateException("Account sync is unavailable.")
                                    message = sync.createInitialCloudBackup()
                                } catch (e: Exception) {
                                    message = e.localizedMessage ?: "Cloud backup failed."
                                } finally { busy = false }
                            }
                        },
                        enabled = !busy && verified,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Create initial cloud backup") }
                    }
                    OutlinedButton(
                        onClick = { repository.signOut(); message = "Signed out." },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Sign out") }
                    LaunchedEffect(user?.uid) { onVerified() }
                } else if (verificationPending || user != null) {
                    Text(user?.email ?: email, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Open the verification email from Firebase and tap its verification link. Then return here and refresh the status. Your study data will not be imported from old local storage automatically.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = {
                            busy = true
                            message = null
                            scope.launch {
                                try {
                                    val refreshed = repository.refreshCurrentUser()
                                    if (refreshed?.isEmailVerified == true) {
                                        verificationPending = false
                                        message = "Email verified."
                                        onVerified()
                                    } else {
                                        verificationPending = true
                                        message = "Email is not verified yet. Check your inbox and spam folder."
                                    }
                                } catch (e: Exception) {
                                    message = e.localizedMessage ?: "Could not refresh verification status."
                                } finally { busy = false }
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("I've verified my email — Refresh") }
                    OutlinedButton(
                        onClick = {
                            busy = true
                            message = null
                            scope.launch {
                                try {
                                    repository.resendVerificationEmail()
                                    message = "Verification email sent. Check your inbox and spam folder."
                                } catch (e: Exception) {
                                    message = e.localizedMessage ?: "Could not resend verification email."
                                } finally { busy = false }
                            }
                        },
                        enabled = !busy && user != null && !verified,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Resend verification email") }
                    TextButton(
                        onClick = { repository.signOut(); verificationPending = false; password = ""; message = "Signed out." },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Sign out") }
                } else {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; message = null },
                        modifier = Modifier.fillMaxWidth().testTag("account_email"),
                        label = { Text("Email address") },
                        placeholder = { Text("you@example.com") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; message = null },
                        modifier = Modifier.fillMaxWidth().testTag("account_password"),
                        label = { Text("Password") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { confirmAction = "login" },
                            enabled = !busy && email.contains("@") && password.isNotEmpty(),
                            modifier = Modifier.weight(1f).height(50.dp).testTag("btn_login"),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Log In") }
                        OutlinedButton(
                            onClick = { confirmAction = "create" },
                            enabled = !busy && email.contains("@") && password.length >= 6,
                            modifier = Modifier.weight(1f).height(50.dp).testTag("btn_create_account"),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Create Account") }
                    }
                    Text("Password must be at least 6 characters for a new account.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(
                        onClick = onContinueAsGuest,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Continue as guest") }
                    Text(
                        "Start studying without an account. You can create one later and choose whether to import this device's progress.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (busy) CircularProgressIndicator(Modifier.size(22.dp))
                message?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
            }
        }
        if (user == null && !verificationPending) {
            TextButton(onClick = onForgotPassword, modifier = Modifier.fillMaxWidth().testTag("btn_forgot_password")) {
                Icon(Icons.Default.Security, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Forgot password?")
            }
        }
    }

    if (confirmImport) {
        AlertDialog(
            onDismissRequest = { confirmImport = false },
            title = { Text("Import old local progress?") },
            text = {
                Text("This copies existing study plans, exams, study sessions and progress totals from this device into the currently signed-in account. The old local records will not be deleted. Import is cancelled if this account already contains study records.")
            },
            confirmButton = {
                Button(
                    enabled = !busy,
                    onClick = {
                        confirmImport = false
                        busy = true
                        message = null
                        val uid = user?.uid
                        scope.launch {
                            try {
                                if (uid.isNullOrBlank()) {
                                    throw IllegalStateException("Sign in before importing progress.")
                                }
                                val summary = LegacyProgressImportRepository(context, uid).importLegacyProgress()
                                message = if (summary.isEmpty) {
                                    "No existing local progress was found to import."
                                } else {
                                    "Import complete: ${summary.plans} plans, ${summary.exams} exams and ${summary.sessions} study sessions copied. Old local data was kept."
                                }
                            } catch (e: Exception) {
                                message = e.localizedMessage ?: "Import failed. Existing data was not intentionally deleted."
                            } finally { busy = false }
                        }
                    }
                ) { Text("Import progress") }
            },
            dismissButton = {
                TextButton(onClick = { confirmImport = false }) { Text("Cancel") }
            }
        )
    }

    if (confirmAction != null) {
        AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text(if (confirmAction == "create") "Create account" else "Sign in") },
            text = {
                Text(
                    if (confirmAction == "create")
                        "A verification email will be sent. You must verify it before entering StudyOS. Existing local progress will remain untouched and will not be imported without your explicit permission."
                    else
                        "Only verified email accounts can enter StudyOS. Existing local progress will remain untouched."
                )
            },
            confirmButton = {
                Button(onClick = {
                    val action = confirmAction ?: return@Button
                    confirmAction = null
                    busy = true
                    message = null
                    scope.launch {
                        try {
                            if (action == "login") {
                                val signedIn = repository.signIn(email, password)
                                if (signedIn.isEmailVerified) onVerified()
                            } else {
                                val created = repository.createAccount(email, password)
                                verificationPending = true
                                message = "Verification email sent to ${created.email.orEmpty()}. Verify it before continuing."
                            }
                        } catch (e: Exception) {
                            if (e is EmailNotVerifiedException || repository.currentUser?.isEmailVerified == false) {
                                verificationPending = true
                            }
                            message = e.localizedMessage ?: "Account action failed."
                        } finally { busy = false }
                    }
                }) { Text("Continue") }
            },
            dismissButton = { TextButton(onClick = { confirmAction = null }) { Text("Cancel") } }
        )
    }
}
