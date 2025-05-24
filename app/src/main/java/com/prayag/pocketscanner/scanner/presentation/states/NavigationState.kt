package com.prayag.pocketscanner.scanner.presentation.states

// Navigation events for controlled navigation
sealed class NavigationEvent {
    object DataLoaded : NavigationEvent()
    data class DocumentLoaded(val documentId: String) : NavigationEvent()
    data class ProcessingComplete(val filePath: String) : NavigationEvent()
    data class ProcessingFailed(val error: String) : NavigationEvent()
    data class DocumentLoadFailed(val error: String) : NavigationEvent()
}