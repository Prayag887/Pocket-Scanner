package com.prayag.pocketscanner.auth.di

import com.google.firebase.auth.FirebaseAuth
import com.prayag.pocketscanner.R
import com.prayag.pocketscanner.auth.data.repository.AuthRepositoryImpl
import com.prayag.pocketscanner.auth.domain.repository.AuthRepository
import com.prayag.pocketscanner.auth.domain.usecase.SignInWithGoogleUseCase
import com.prayag.pocketscanner.auth.domain.usecase.TryAutoLoginUseCase
import com.prayag.pocketscanner.auth.presentation.login.LoginViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val loginModule = module {
    // Provide the web client ID as a single instance
    single<String>(qualifier = named("webClientId")) {
        androidContext().getString(R.string.default_web_client_id)
    }
    single { FirebaseAuth.getInstance() }
    single<AuthRepository> { AuthRepositoryImpl(get()) }
    factory { SignInWithGoogleUseCase(get()) }
    factory { TryAutoLoginUseCase(get(),  get(qualifier = named("webClientId"))) } // Add this line
    viewModel {
        LoginViewModel(
            signInWithGoogleUseCase = get(),
            webClientId = get(qualifier = named("webClientId"))
        )
    }
}