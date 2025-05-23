package com.prayag.pocketscanner.scanner.di

import android.content.Context
import com.prayag.pocketscanner.scanner.data.repository.FileSystemDocumentRepositoryImpl
import com.prayag.pocketscanner.scanner.domain.repository.DocumentRepository
import com.prayag.pocketscanner.scanner.domain.usecase.GetDocumentsUseCase
//import com.prayag.pocketscanner.presentation.viewmodels.DocumentDetailViewModel
import com.prayag.pocketscanner.scanner.presentation.viewmodels.DocumentViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module


fun appModule(isDebugBuild: Boolean) = module {

    single<DocumentRepository> { FileSystemDocumentRepositoryImpl(get<Context>().filesDir) }
    factory { GetDocumentsUseCase(get()) }

    viewModelOf(::DocumentViewModel)
//    viewModelOf(::DocumentDetailViewModel)
}