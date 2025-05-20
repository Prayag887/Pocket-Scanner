package com.prayag.pocketscanner.auth.di

import com.google.firebase.auth.FirebaseAuth
import com.prayag.pocketscanner.auth.data.repository.AuthRepositoryImpl
import com.prayag.pocketscanner.auth.domain.repository.AuthRepository
import com.prayag.pocketscanner.auth.domain.usecase.SignInWithGoogleUseCase
import com.prayag.pocketscanner.auth.presentation.login.LoginViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val loginModule = module {
    single { FirebaseAuth.getInstance() }
    single<AuthRepository> { AuthRepositoryImpl(get()) }
    factory { SignInWithGoogleUseCase(get()) }
    viewModel { LoginViewModel(get(), get()) }
}
