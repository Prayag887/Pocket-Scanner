package com.prayag.pocketscanner.splash.domain.repository

interface ConnectivityRepository {
    fun hasInternetConnection(): Boolean
}