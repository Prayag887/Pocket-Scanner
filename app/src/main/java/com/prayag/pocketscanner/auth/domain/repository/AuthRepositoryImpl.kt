package com.prayag.pocketscanner.auth.domain.repository

import com.prayag.pocketscanner.auth.domain.model.User

interface AuthRepository {
    suspend fun signInWithGoogle(idToken: String): Result<User>
}
