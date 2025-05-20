package com.prayag.pocketscanner.auth.presentation.login

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.prayag.pocketscanner.auth.domain.model.User
import com.prayag.pocketscanner.auth.domain.usecase.SignInWithGoogleUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val TAG = "LoginViewModel"

class LoginViewModel(
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val applicationContext: Context
) : ViewModel() {

    // Using sealed class for better type safety and state management
    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        data class Success(val user: User) : AuthState()
        data class Error(
            val message: String,
            val exception: Exception? = null,
            val animationState: SkyAnimationState? = null // <-- Optional addition
        ) : AuthState()
    }


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

    fun handleGoogleSignInResult(data: Intent?) {
        if (data == null) {
            _authState.value = AuthState.Error("Sign-in intent data is null")
            return
        }

        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                val idToken = account.idToken
                Log.d(TAG, "Google Sign-In successful, token received: ${idToken?.take(10)}...")

                if (idToken != null) {
                    signIn(idToken)
                } else {
                    Log.e(TAG, "Google Sign-In: ID token is null.")
                    _authState.value = AuthState.Error("No ID token found from Google Sign-In")
                }
            } else {
                Log.e(TAG, "Google Sign-In: Account is null.")
                _authState.value = AuthState.Error("Google Sign-In failed: Account is null")
            }
        } catch (e: ApiException) {
            // Handle specific API errors with more detailed messages
            val errorMsg = when (e.statusCode) {
                // Add specific error codes as needed
                12500 -> "Google Play Services update required"
                12501 -> "User canceled the sign-in flow"
                else -> "Google Sign-In failed: ${e.message} (code: ${e.statusCode})"
            }

            Log.e(TAG, errorMsg, e)
            _authState.value = AuthState.Error(errorMsg, e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in Google Sign-In", e)
            _authState.value = AuthState.Error("Unexpected error in Google Sign-In: ${e.message}", e)
        }
    }

    fun tryAutoLogin() {
        viewModelScope.launch {
            try {
                val account = GoogleSignIn.getLastSignedInAccount(applicationContext)
                if (account != null && account.idToken != null) {
                    signIn(account.idToken!!)
                } else {
                    _authState.value = AuthState.Error("User not signed in")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Auto-login failed", e)
                _authState.value = AuthState.Error("Auto-login failed: ${e.localizedMessage}")
            }
        }
    }



    // Reset auth state (useful when navigating away or retrying)
    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }
}