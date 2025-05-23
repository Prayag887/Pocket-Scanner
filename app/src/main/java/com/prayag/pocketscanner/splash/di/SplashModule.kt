package com.prayag.pocketscanner.splash.di

import com.prayag.pocketscanner.splash.data.repository.ConnectivityRepositoryImpl
import com.prayag.pocketscanner.splash.domain.repository.ConnectivityRepository
import com.prayag.pocketscanner.splash.domain.usecase.CheckConnectivityUseCase
import com.prayag.pocketscanner.splash.domain.usecase.HandleSplashFlowUseCase
import com.prayag.pocketscanner.splash.presentation.viewmodel.SplashViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val splashModule = module {
    single<ConnectivityRepository> { ConnectivityRepositoryImpl(androidContext()) }
    single { CheckConnectivityUseCase(get()) }
    single { HandleSplashFlowUseCase(get(), get()) }
//    viewModel { SplashViewModel(get()) }

    viewModelOf(::SplashViewModel)
}