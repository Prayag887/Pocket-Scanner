package com.prayag.pocketscanner


import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class PocketScannerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val isDebugBuild = false

        startKoin {
            androidLogger()
            androidContext(this@PocketScannerApplication)
            modules(
//                appModule(isDebugBuild),
//                loginModule,
//                splashModule
            )
        }
    }
}