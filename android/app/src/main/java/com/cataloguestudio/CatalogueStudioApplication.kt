package com.cataloguestudio

import android.app.Application
import com.cataloguestudio.data.di.catalogueDataModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class CatalogueStudioApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@CatalogueStudioApplication)
            modules(catalogueDataModule(BuildConfig.BASE_URL))
        }
    }
}
