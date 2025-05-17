package com.example.pocketscanner.di

import com.example.pocketscanner.data.repository.DocumentRepositoryImpl
import com.example.pocketscanner.domain.repository.DocumentRepository
import com.example.pocketscanner.domain.usecase.GetDocumentsUseCase
import com.example.pocketscanner.presentation.viewmodels.DocumentViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Repositories
    single<DocumentRepository> { DocumentRepositoryImpl() }

    // Use Cases
    factory { GetDocumentsUseCase(get()) }

    // ViewModels
    viewModel { DocumentViewModel(get()) }
}
