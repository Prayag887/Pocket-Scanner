package com.prayag.pocketscanner.di

import android.content.Context
import com.prayag.pocketscanner.data.repository.FileSystemDocumentRepositoryImpl
import com.prayag.pocketscanner.domain.repository.DocumentRepository
import com.prayag.pocketscanner.domain.usecase.GetDocumentsUseCase
//import com.prayag.pocketscanner.presentation.viewmodels.DocumentDetailViewModel
import com.prayag.pocketscanner.presentation.viewmodels.DocumentViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module


fun appModule(isDebugBuild: Boolean) = module {

    single<DocumentRepository> { FileSystemDocumentRepositoryImpl(get<Context>().filesDir) }
    factory { GetDocumentsUseCase(get()) }

    viewModelOf(::DocumentViewModel)
//    viewModelOf(::DocumentDetailViewModel)
}