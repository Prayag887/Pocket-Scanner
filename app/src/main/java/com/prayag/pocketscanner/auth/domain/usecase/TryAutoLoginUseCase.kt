package com.prayag.pocketscanner.auth.domain.usecase

import android.content.Context
import com.prayag.pocketscanner.auth.domain.model.User
import com.prayag.pocketscanner.auth.domain.repository.AuthRepository

class TryAutoLoginUseCase(
    private val authRepository: AuthRepository,
    private val idToken: String
) {
    suspend operator fun invoke(context: Context): Result<User?> {
        return authRepository.tryAutoLogin(context, idToken)
    }
}