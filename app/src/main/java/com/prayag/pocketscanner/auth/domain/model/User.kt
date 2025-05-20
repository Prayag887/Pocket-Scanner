package com.prayag.pocketscanner.auth.domain.model

data class User(
    val uid: String,
    val name: String?,
    val email: String?
) {
    // Determine if the user is new by checking if the name and email are null
    val isNew: Boolean
        get() = name.isNullOrEmpty() || email.isNullOrEmpty()
}
