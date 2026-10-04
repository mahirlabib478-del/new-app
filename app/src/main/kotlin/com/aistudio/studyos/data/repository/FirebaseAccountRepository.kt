package com.aistudio.studyos.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Email/password account operations. Local progress is intentionally not touched here.
 * New accounts must verify their email before the app grants access to study data.
 */
class FirebaseAccountRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    suspend fun createAccount(email: String, password: String): FirebaseUser {
        val user = awaitUser { auth.createUserWithEmailAndPassword(email.trim(), password) }
        try {
            user.sendEmailVerification().awaitCompletion()
        } catch (error: Exception) {
            // Do not pretend verification was sent. Keep the account so the user can retry.
            throw IllegalStateException(
                "Account created, but verification email could not be sent. Please resend it.",
                error
            )
        }
        return user
    }

    suspend fun signIn(email: String, password: String): FirebaseUser {
        val user = awaitUser { auth.signInWithEmailAndPassword(email.trim(), password) }
        user.reload().awaitCompletion()
        val refreshedUser = auth.currentUser
            ?: throw IllegalStateException("Firebase session is unavailable. Please sign in again.")
        if (!refreshedUser.isEmailVerified) {
            throw EmailNotVerifiedException()
        }
        // Refresh the ID token so Firestore security rules see the email_verified claim.
        refreshedUser.getIdToken(true).awaitCompletion()
        return refreshedUser
    }

    suspend fun refreshCurrentUser(): FirebaseUser? {
        val user = auth.currentUser ?: return null
        user.reload().awaitCompletion()
        val refreshedUser = auth.currentUser
        if (refreshedUser?.isEmailVerified == true) {
            // A verified profile alone is not enough if the cached ID token still has old claims.
            refreshedUser.getIdToken(true).awaitCompletion()
        }
        return auth.currentUser
    }

    suspend fun resendVerificationEmail() {
        val user = auth.currentUser ?: throw IllegalStateException("Sign in to resend the verification email.")
        user.reload().awaitCompletion()
        val refreshed = auth.currentUser ?: throw IllegalStateException("Firebase session is unavailable.")
        if (refreshed.isEmailVerified) return
        refreshed.sendEmailVerification().awaitCompletion()
    }

    fun sendPasswordResetEmail(email: String, onComplete: (Exception?) -> Unit) {
        auth.sendPasswordResetEmail(email.trim())
            .addOnCompleteListener { task ->
                onComplete(if (task.isSuccessful) null else task.exception
                    ?: IllegalStateException("Password reset email failed."))
            }
    }

    fun signOut() = auth.signOut()

    private suspend fun awaitUser(
        operation: () -> com.google.android.gms.tasks.Task<com.google.firebase.auth.AuthResult>
    ): FirebaseUser = suspendCancellableCoroutine { continuation ->
        operation().addOnCompleteListener { task ->
            if (!continuation.isActive) return@addOnCompleteListener
            if (task.isSuccessful) {
                val user = task.result?.user
                if (user != null) continuation.resume(user)
                else continuation.resumeWithException(IllegalStateException("Firebase returned no user."))
            } else {
                continuation.resumeWithException(
                    task.exception ?: IllegalStateException("Firebase authentication failed.")
                )
            }
        }
    }
}

class EmailNotVerifiedException : IllegalStateException(
    "Your email is not verified yet. Open the verification email, then tap “I've verified my email”."
)

private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitCompletion(): T =
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (!continuation.isActive) return@addOnCompleteListener
            if (task.isSuccessful) {
                @Suppress("UNCHECKED_CAST")
                continuation.resume(task.result as T)
            } else {
                continuation.resumeWithException(
                    task.exception ?: IllegalStateException("The requested Firebase action failed.")
                )
            }
        }
    }
