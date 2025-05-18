package com.example.pocketscanner.di

import android.content.Context
import com.example.pocketscanner.domain.repository.DocumentRepository
import com.example.pocketscanner.data.repository.FileSystemDocumentRepositoryImpl
import com.example.pocketscanner.domain.usecase.GetDocumentsUseCase
import com.example.pocketscanner.presentation.viewmodels.DocumentDetailViewModel
import com.example.pocketscanner.presentation.viewmodels.DocumentViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module


fun appModule(isDebugBuild: Boolean) = module {
//    single<DocumentRepository> {
//        if (isDebugBuild) {
//            DocumentRepositoryImpl()
//        } else {
//            FileSystemDocumentRepository(get<Context>().filesDir)
//        }
//    }

    single<DocumentRepository> { FileSystemDocumentRepositoryImpl(get<Context>().filesDir) }
    factory { GetDocumentsUseCase(get()) }

    viewModel { DocumentViewModel(get()) }
    viewModel { DocumentDetailViewModel(get()) }
}