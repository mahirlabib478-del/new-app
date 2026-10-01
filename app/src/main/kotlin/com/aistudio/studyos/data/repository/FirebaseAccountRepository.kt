package com.aistudio.studyos.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Email/password account operations. This class deliberately does not touch local progress;
 * progress import/export must be coordinated by the caller to avoid overwriting local data.
 */
class FirebaseAccountRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    suspend fun createAccount(email: String, password: String): FirebaseUser =
        awaitUser { auth.createUserWithEmailAndPassword(email.trim(), password) }

    suspend fun signIn(email: String, password: String): FirebaseUser =
        awaitUser { auth.signInWithEmailAndPassword(email.trim(), password) }

    fun sendPasswordResetEmail(email: String, onComplete: (Exception?) -> Unit) {
        auth.sendPasswordResetEmail(email.trim())
            .addOnCompleteListener { task -> onComplete(if (task.isSuccessful) null else task.exception ?: IllegalStateException("Password reset email failed.")) }
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
