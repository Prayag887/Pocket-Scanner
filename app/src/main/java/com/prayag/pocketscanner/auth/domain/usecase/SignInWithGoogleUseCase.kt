package com.prayag.pocketscanner.auth.domain.usecase

import com.prayag.pocketscanner.auth.domain.repository.AuthRepository
import com.prayag.pocketscanner.auth.domain.model.User

class SignInWithGoogleUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(idToken: String): Result<User> {
        return repository.signInWithGoogle(idToken)
    }
}
