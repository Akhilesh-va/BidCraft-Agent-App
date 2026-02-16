package com.example.bidcarftagent.presentation.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import android.util.Log

class AuthManager @Inject constructor(
    private val auth: FirebaseAuth
) {
    suspend fun signInWithGoogle(idToken: String): Result<String> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val uid = result.user?.uid ?: ""
            Log.d("AuthManager", "signInWithGoogle succeeded uid=$uid")
            Result.success(uid)
        } catch (e: Exception) {
            Log.w("AuthManager", "signInWithGoogle failed", e)
            Result.failure(e)
        }
    }
    suspend fun getIdToken(forceRefresh: Boolean = false): Result<String> {
        return try {
            val token = auth.currentUser?.getIdToken(forceRefresh)?.await()?.token ?: ""
            if (token.isNullOrBlank()) {
                Log.w("AuthManager", "getIdToken: no token")
                Result.failure(Exception("No ID token"))
            } else {
                Log.d("AuthManager", "getIdToken: token len=${token.length}")
                Result.success(token)
            }
        } catch (e: Exception) {
            Log.w("AuthManager", "getIdToken failed", e)
            Result.failure(e)
        }
    }
}

