package com.example.pocketscanner.di

import android.content.Context
import com.example.pocketscanner.data.repository.FileSystemDocumentRepositoryImpl
import com.example.pocketscanner.domain.repository.DocumentRepository
import com.example.pocketscanner.domain.usecase.GetDocumentsUseCase
import com.example.pocketscanner.presentation.viewmodels.DocumentDetailViewModel
import com.example.pocketscanner.presentation.viewmodels.DocumentViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module


fun appModule(isDebugBuild: Boolean) = module {

    single<DocumentRepository> { FileSystemDocumentRepositoryImpl(get<Context>().filesDir) }
    factory { GetDocumentsUseCase(get()) }

    viewModelOf(::DocumentViewModel)
    viewModelOf(::DocumentDetailViewModel)
}