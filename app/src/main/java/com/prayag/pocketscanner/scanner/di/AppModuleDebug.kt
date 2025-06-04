package com.prayag.pocketscanner.scanner.di

import android.content.Context
import com.prayag.pocketscanner.scanner.data.repository.FileSystemDocumentRepositoryImpl
import com.prayag.pocketscanner.scanner.domain.repository.DocumentRepository
import com.prayag.pocketscanner.scanner.domain.usecase.GetDocumentsUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.batchoperations.BatchDeleteDocumentsUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.batchoperations.BatchSaveDocumentsUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.cloudstorage.CachePdfLocallyUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.cloudstorage.DetectCloudProviderUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.cloudstorage.SyncWithCloudStorageUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.combined.ImportPdfUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.combined.ProcessMultiplePdfsUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.document.DeleteDocumentUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.document.GetAllDocumentsUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.document.GetDocumentByIdUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.document.GetDocumentPagesUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.document.SaveDocumentUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.externaldocument.ExtractPagesFromPdfUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.externaldocument.GetPdfMetadataUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.externaldocument.ImportPdfFromCloudStorageUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.externaldocument.ImportPdfFromDeviceUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.externaldocument.OpenExternalPdfUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.externaldocument.ValidatePdfAccessUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.searchfilter.FilterDocumentsByDateRangeUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.searchfilter.FilterDocumentsByFormatUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.searchfilter.FilterDocumentsByTagUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.searchfilter.GetDocumentStatisticsUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.searchfilter.SearchDocumentsUseCase
import com.prayag.pocketscanner.scanner.domain.usecase.searchfilter.SortDocumentsUseCase
//import com.prayag.pocketscanner.presentation.viewmodels.DocumentDetailViewModel
import com.prayag.pocketscanner.scanner.presentation.viewmodels.DocumentViewModel
import com.prayag.pocketscanner.scanner.utils.FileUtils
import com.prayag.pocketscanner.scanner.utils.PdfUtils
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import java.io.File


fun appModule(isDebugBuild: Boolean) = module {

    // Document Use Cases
    single { GetAllDocumentsUseCase(get()) }
    single { GetDocumentByIdUseCase(get()) }
    single { SaveDocumentUseCase(get()) }
    single { DeleteDocumentUseCase(get()) }
    single { GetDocumentPagesUseCase(get()) }

    // External PDF Use Cases
    single { OpenExternalPdfUseCase(get()) }
    single { ImportPdfFromDeviceUseCase(get()) }
    single { ImportPdfFromCloudStorageUseCase(get()) }
    single { ValidatePdfAccessUseCase(get()) }
    single { GetPdfMetadataUseCase(get()) }
    single { ExtractPagesFromPdfUseCase(get()) }

    // Cloud Storage Use Cases
    single { SyncWithCloudStorageUseCase(get()) }
    single { CachePdfLocallyUseCase(get()) }
    single { DetectCloudProviderUseCase(get()) }

    // Combined Use Cases
    single {
        ImportPdfUseCase(
            validatePdfAccessUseCase = get(),
            detectCloudProviderUseCase = get(),
            importPdfFromDeviceUseCase = get(),
            importPdfFromCloudStorageUseCase = get(),
            openExternalPdfUseCase = get()
        )
    }
    single { ProcessMultiplePdfsUseCase(get()) }

    // Search and Filter Use Cases
    single { SearchDocumentsUseCase(get()) }
    single { FilterDocumentsByTagUseCase(get()) }
    single { FilterDocumentsByFormatUseCase(get()) }
    single { FilterDocumentsByDateRangeUseCase(get()) }
    single { SortDocumentsUseCase(get()) }
    single { GetDocumentStatisticsUseCase(get()) }
    single { GetDocumentsUseCase(get()) }

    // Batch Operations Use Cases
    single { BatchDeleteDocumentsUseCase(get()) }
    single { BatchSaveDocumentsUseCase(get()) }

    single<File> { get<Context>().filesDir } // Provide File instance
    single<DocumentRepository> {
        FileSystemDocumentRepositoryImpl(
            context = get(),
            filesDir = get()
        )
    }

    single { PdfUtils(get()) }
    single { FileUtils() }

    viewModelOf(::DocumentViewModel)
//    viewModelOf(::DocumentDetailViewModel)
}