package com.prayag.pocketscanner.splash.domain.usecase

import android.content.Context
import com.prayag.pocketscanner.auth.domain.model.User
import com.prayag.pocketscanner.auth.domain.usecase.TryAutoLoginUseCase

class HandleSplashFlowUseCase(
    private val checkConnectivityUseCase: CheckConnectivityUseCase,
    private val tryAutoLoginUseCase: TryAutoLoginUseCase
) {
    sealed class Result {
        object NoInternet : Result()
        data class LoginSuccess(val user: User) : Result()
        object LoginFailed : Result()
    }

    suspend operator fun invoke(context: Context): Result {
        return if (checkConnectivityUseCase()) {
            try {
                val loginResult = tryAutoLoginUseCase(context)
                loginResult.fold(
                    onSuccess = { user ->
                        if (user != null) {
                            Result.LoginSuccess(user)
                        } else {
                            Result.LoginFailed
                        }
                    },
                    onFailure = {
                        Result.LoginFailed
                    }
                )
            } catch (e: Exception) {
                Result.LoginFailed
            }
        } else {
            Result.NoInternet
        }
    }
}