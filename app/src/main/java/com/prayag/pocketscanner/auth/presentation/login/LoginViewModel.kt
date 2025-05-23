package com.prayag.pocketscanner.auth.presentation.login

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.prayag.pocketscanner.auth.data.repository.ProceedWithoutLoginException
import com.prayag.pocketscanner.auth.domain.usecase.SignInWithGoogleUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val TAG = "LoginViewModel"

class LoginViewModel(
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val webClientId: String
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private fun signIn(idToken: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val result = signInWithGoogleUseCase(idToken)
                result.fold(
                    onSuccess = { user ->
                        _authState.value = AuthState.Success(user)
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Firebase authentication failed", exception)
                        _authState.value = AuthState.Error(
                            message = "Authentication failed: ${exception.message}",
                            exception = exception as? Exception
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during sign-in process", e)
                _authState.value = AuthState.Error(
                    message = "Unexpected error occurred: ${e.message}",
                    exception = e
                )
            }
        }
    }

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading

                val credentialManager = CredentialManager.create(context)

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(true)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )

                handleSignInResult(result)

            } catch (e: ProceedWithoutLoginException) {
                // Explicitly caught if AuthRepositoryImpl throws it
                Log.e(TAG, "Firebase failed due to no internet, proceeding without login", e)
                _authState.value = AuthState.ProceedWithoutLogin

            } catch (e: Exception) {
                // This could include CredentialManager failure (e.g. no Google account, or no network)
                Log.e(TAG, "Google Sign-In failed, proceeding without login", e)
                _authState.value = AuthState.ProceedWithoutLogin
            }
        }
    }



    private fun handleSignInResult(result: GetCredentialResponse) {
        when (val credential = result.credential) {
            is GoogleIdTokenCredential -> {
                try {
                    val idToken = credential.idToken
                    Log.d(TAG, "Google Sign-In successful, token received: ${idToken.take(10)}...")
                    signIn(idToken)
                } catch (e: GoogleIdTokenParsingException) {
                    Log.e(TAG, "Invalid Google ID token", e)
                    _authState.value = AuthState.Error(
                        message = "Invalid Google ID token",
                        exception = e
                    )
                }
            }
            else -> {
                Log.e(TAG, "Unexpected credential type")
                _authState.value = AuthState.Error("Unexpected credential type received")
            }
        }
    }

    fun tryAutoLogin(context: Context) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading

                val credentialManager = CredentialManager.create(context)

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(true)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(true)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )

                handleSignInResult(result)

            } catch (e: GetCredentialException) {
                Log.d(TAG, "No saved credentials found for auto-login", e)
                _authState.value = AuthState.Error("No saved credentials found")
            } catch (e: Exception) {
                Log.e(TAG, "Auto-login failed", e)
                _authState.value = AuthState.Error("Auto-login failed: ${e.message}")
            }
        }
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }
}