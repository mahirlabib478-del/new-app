package com.aistudio.studyos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onContinueAsGuest: () -> Unit = {},
    showBackButton: Boolean = true,
    showContinueAsGuest: Boolean = true
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
    var confirmSignOutAnyway by remember { mutableStateOf(false) }
    var confirmProgressMerge by remember { mutableStateOf(false) }
    var signOutFailureMessage by remember { mutableStateOf<String?>(null) }
    var pendingVerifiedNavigation by remember { mutableStateOf(false) }
    var showAdvancedSync by remember { mutableStateOf(false) }
    var verificationPending by remember { mutableStateOf(repository.currentUser?.isEmailVerified == false) }
    val user = repository.currentUser
    val verified = user?.isEmailVerified == true
    val cloudSync = remember(user?.uid) {
        user?.uid?.let { (context.applicationContext as StudyApplication).cloudSyncFor(it) }
    }
    val syncStatusState = cloudSync?.syncStatus?.collectAsState(initial = "Checking cloud sync…")
    val syncStatus = syncStatusState?.value ?: "Cloud sync unavailable"

    val horizontalContentPadding = if (LocalConfiguration.current.screenWidthDp < 360) 12.dp else 20.dp

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = horizontalContentPadding)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (showBackButton) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    "Account & Security",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(25.dp)
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        "StudyOS",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "SIGN IN OR CREATE AN ACCOUNT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        val guestAccountPage = !showContinueAsGuest && !verificationPending && user == null
        if (!verificationPending && user == null) {
            Spacer(Modifier.height(if (guestAccountPage) 12.dp else 20.dp))
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.size(76.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = if (guestAccountPage) "Guest avatar" else "Account avatar",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                if (guestAccountPage) "Guest profile" else "Welcome to StudyOS",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                if (guestAccountPage) "Your study progress stays on this device" else "Sign in or create an account to continue your study journey.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (guestAccountPage) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        "Create an account to access your study progress on supported devices. You can choose whether to import this device's progress after email verification.",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Spacer(Modifier.height(16.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.size(58.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                (user?.email ?: email).firstOrNull()?.uppercaseChar()?.toString() ?: "S",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            if (verified) "Account connected" else "Verify your email",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            user?.email ?: email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            if (verified) "Cloud sync status: $syncStatus" else "Verify your email to finish setup",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (verified && syncStatus.startsWith("Synced")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(if (guestAccountPage) 18.dp else 32.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
            Column(
                Modifier.padding(if (guestAccountPage) 20.dp else 24.dp),
                verticalArrangement = Arrangement.spacedBy(if (guestAccountPage) 14.dp else 18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        when {
                            verified -> "Account verified"
                            verificationPending || user != null -> "Email verification required"
                            !showContinueAsGuest -> "Manage your account"
                            else -> "Welcome to StudyOS"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (verified) {
                    Text(user?.email.orEmpty(), style = MaterialTheme.typography.bodyLarge)
                    Text("You're all set. Your personal study space is ready.", style = MaterialTheme.typography.bodyMedium)
                    Text("Cloud sync: " + syncStatus, style = MaterialTheme.typography.bodySmall, color = if (syncStatus.startsWith("Synced")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(
                        onClick = { showAdvancedSync = !showAdvancedSync },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (showAdvancedSync) "Hide progress sync options" else "Progress sync options")
                    }
                    if (showAdvancedSync) {
                        Text(
                            "If this device and the cloud both have progress, merge adds missing cloud records without replacing existing local rows. This device's theme and settings are kept. The merged progress is then uploaded to your account.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = { confirmProgressMerge = true },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Review & merge progress", maxLines = 1, softWrap = false)
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            if (!busy) {
                                busy = true
                                message = null
                                signOutFailureMessage = null
                                scope.launch {
                                    try {
                                        val sync = cloudSync
                                            ?: throw IllegalStateException("Cloud sync is unavailable. Your latest progress could not be confirmed as backed up.")
                                        sync.syncNowBeforeSignOut()
                                        repository.signOut()
                                        message = "Progress synced. Signed out safely."
                                    } catch (e: Exception) {
                                        signOutFailureMessage = e.localizedMessage
                                            ?: "Cloud sync could not be completed."
                                        confirmSignOutAnyway = true
                                    } finally {
                                        busy = false
                                    }
                                }
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (busy) "Syncing before sign out…" else "Sign out", maxLines = 1, softWrap = false)
                    }
                } else if (verificationPending || user != null) {
                    Text(user?.email ?: email, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Open the verification email and tap its verification link. If it isn't in your inbox, check Spam/Junk and wait a few minutes. Email providers sometimes filter automated messages when the sender is unfamiliar or the message resembles bulk mail; this does not automatically mean it is unsafe. Then return here and refresh the status.",
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
                                        val importer = LegacyProgressImportRepository(context, refreshed.uid)
                                        if (importer.hasLegacyProgress()) {
                                            // Ask first; do not leave the account screen until the user chooses.
                                            pendingVerifiedNavigation = true
                                            verificationPending = false
                                            message = "Email verified. Choose whether to import your guest progress."
                                            confirmImport = true
                                        } else {
                                            verificationPending = false
                                            message = "Email verified."
                                            onVerified()
                                        }
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
                    ) {
                        Text("I've verified — Refresh status", maxLines = 1, softWrap = false, fontSize = 13.sp)
                    }
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
                    ) {
                        Text("Resend verification email", maxLines = 1, softWrap = false, fontSize = 14.sp)
                    }
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
                    // Full-width actions avoid label wrapping on narrow phones and large font settings.
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { confirmAction = "login" },
                            enabled = !busy && email.contains("@") && password.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("btn_login"),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text("Log In", maxLines = 1, softWrap = false)
                        }
                        OutlinedButton(
                            onClick = { confirmAction = "create" },
                            enabled = !busy && email.contains("@") && password.length >= 6,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("btn_create_account"),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text("Create Account", maxLines = 1, softWrap = false)
                        }
                    }
                    Text("Password must be at least 6 characters for a new account.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (showContinueAsGuest) {
                        OutlinedButton(
                            onClick = onContinueAsGuest,
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Continue as guest", maxLines = 1, softWrap = false)
                        }
                        Text(
                            "Start studying without an account. You can create one later and choose whether to import this device's progress.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (busy) CircularProgressIndicator(Modifier.size(22.dp))
                message?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
            }
        }
        if (user == null && !verificationPending) {
            TextButton(
                onClick = onForgotPassword,
                modifier = Modifier.fillMaxWidth().testTag("btn_forgot_password")
            ) {
                Text("Forgot password?")
            }
        }

    }

    if (confirmImport) {
        AlertDialog(
            onDismissRequest = {
                confirmImport = false
                if (pendingVerifiedNavigation) {
                    pendingVerifiedNavigation = false
                    onVerified()
                }
            },
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
                                    "No existing guest progress was found to import."
                                } else {
                                    "Import complete: ${summary.plans} plans, ${summary.exams} exams and ${summary.sessions} study sessions copied. Old guest data was kept."
                                }
                                if (pendingVerifiedNavigation) {
                                    pendingVerifiedNavigation = false
                                    onVerified()
                                }
                            } catch (e: Exception) {
                                message = e.localizedMessage ?: "Import failed. Existing data was not intentionally deleted."
                                // Keep the decision available so the user can retry or skip safely.
                                if (pendingVerifiedNavigation) confirmImport = true
                            } finally { busy = false }
                        }
                    }
                ) { Text("Import progress", maxLines = 1, softWrap = false) }
            },
            dismissButton = {
                TextButton(onClick = {
                    confirmImport = false
                    if (pendingVerifiedNavigation) {
                        pendingVerifiedNavigation = false
                        onVerified()
                    }
                }) { Text("Skip for now", maxLines = 1, softWrap = false, fontSize = 12.sp) }
            }
        )
    }

    if (confirmProgressMerge) {
        AlertDialog(
            onDismissRequest = { if (!busy) confirmProgressMerge = false },
            title = { Text("Merge local and cloud progress?") },
            text = {
                Text(
                    "StudyOS will add cloud study plans, exams and sessions that are not already on this device, reconcile progress totals, and upload the merged result. Existing local records will not be replaced. This device's theme and settings will be kept. If upload fails after the local merge, your merged local data remains and you can retry sync."
                )
            },
            confirmButton = {
                Button(
                    enabled = !busy,
                    onClick = {
                        confirmProgressMerge = false
                        busy = true
                        message = null
                        scope.launch {
                            try {
                                val sync = cloudSync
                                    ?: throw IllegalStateException("Cloud sync is unavailable. Please retry later.")
                                message = sync.mergeCloudIntoLocalAndUpload()
                            } catch (e: Exception) {
                                message = e.localizedMessage
                                    ?: "Merge could not be completed. Your local progress has not been intentionally deleted."
                            } finally {
                                busy = false
                            }
                        }
                    }
                ) { Text(if (busy) "Merging…" else "Merge & upload") }
            },
            dismissButton = {
                TextButton(
                    enabled = !busy,
                    onClick = { confirmProgressMerge = false }
                ) { Text("Cancel") }
            }
        )
    }

    if (confirmSignOutAnyway) {
        AlertDialog(
            onDismissRequest = { if (!busy) confirmSignOutAnyway = false },
            title = { Text("Cloud backup not confirmed") },
            text = {
                Text(
                    (signOutFailureMessage ?: "The latest progress could not be synced.") +
                        "\n\nYou can stay signed in and retry. If you sign out anyway, the progress already stored on this device is not intentionally deleted, but changes not backed up to the cloud may be lost if app data is cleared or the app is reinstalled."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        confirmSignOutAnyway = false
                        repository.signOut()
                        message = "Signed out without a confirmed cloud backup. Local progress was not intentionally deleted; unbacked changes may not survive reinstall."
                    }
                ) { Text("Sign out anyway") }
            },
            dismissButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        confirmSignOutAnyway = false
                        signOutFailureMessage = null
                        message = "Still signed in. Check the connection or cloud sync status, then retry."
                    }
                ) { Text("Stay signed in") }
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
                        "A verification email will be sent. Check your inbox and Spam/Junk folder if it doesn't arrive; automated messages can sometimes be filtered when the sender is unfamiliar. Verify before entering StudyOS. Existing local progress will remain untouched and will not be imported without your explicit permission."
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
                                message = "Verification email sent to ${created.email.orEmpty()}. Check your inbox and Spam/Junk folder, then verify before continuing."
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
