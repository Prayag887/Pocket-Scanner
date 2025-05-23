package com.prayag.pocketscanner.auth.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.prayag.pocketscanner.auth.domain.model.User
import com.prayag.pocketscanner.auth.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    override suspend fun signInWithGoogle(idToken: String): Result<User> = withContext(Dispatchers.IO) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        try {
            val result = Tasks.await(firebaseAuth.signInWithCredential(credential))
            val user = result.user
            if (user != null) {
                Result.success(
                    User(
                        uid = user.uid,
                        name = user.displayName ?: "Unknown",
                        email = user.email ?: ""
                    )
                )
            } else {
                Result.failure(Exception("User is null"))
            }
        } catch (e: FirebaseNetworkException) {
            // Special case: allow proceeding without login
            Result.failure(ProceedWithoutLoginException("No internet connection"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun tryAutoLogin(context: Context, idToken: String): Result<User?> {
        return try {
            val credentialManager = CredentialManager.create(context)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(idToken)
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            when (val credential = result.credential) {
                is GoogleIdTokenCredential -> {
                    val idToken = credential.idToken
                    signInWithGoogle(idToken).getOrNull()?.let { user ->
                        Result.success(user)
                    } ?: Result.failure(Exception("Sign in failed"))
                }
                else -> Result.failure(Exception("Unexpected credential type"))
            }
        } catch (e: GetCredentialException) {
            Result.success(null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


// Custom exception to signal fallback login behavior
class ProceedWithoutLoginException(message: String) : Exception(message)
