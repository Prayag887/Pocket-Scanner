package com.prayag.pocketscanner.splash.domain.usecase

import com.prayag.pocketscanner.splash.domain.repository.ConnectivityRepository

class CheckConnectivityUseCase(
    private val connectivityRepository: ConnectivityRepository
) {
    operator fun invoke(): Boolean {
        return connectivityRepository.hasInternetConnection()
    }
}