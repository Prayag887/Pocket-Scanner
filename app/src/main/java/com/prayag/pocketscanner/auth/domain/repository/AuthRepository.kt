package com.prayag.pocketscanner.auth.domain.repository

import android.content.Context
import com.prayag.pocketscanner.auth.domain.model.User

interface AuthRepository {
    suspend fun signInWithGoogle(idToken: String): Result<User>
    suspend fun tryAutoLogin(context: Context, idToken: String): Result<User?>
}
