package com.cataloguestudio

import android.app.Application
import com.cataloguestudio.data.KtorCatalogueDataSource
import com.cataloguestudio.domain.CatalogueRemoteDataSource
import com.cataloguestudio.presentation.CatalogueViewModel
import com.cataloguestudio.presentation.EnquiryViewModel
import com.cataloguestudio.presentation.ProductDetailViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

class CatalogueApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@CatalogueApplication)
            modules(catalogueDataModule, cataloguePresentationModule)
        }
    }
}

val catalogueDataModule = module {
    single {
        HttpClient(OkHttp) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(HttpTimeout) {
                requestTimeoutMillis = 25_000
                connectTimeoutMillis = 5000
                socketTimeoutMillis = 25_000
            }
        }
    }
    single<CatalogueRemoteDataSource> { KtorCatalogueDataSource(get(), BuildConfig.BASE_URL) }
}
val cataloguePresentationModule = module {
    viewModelOf(::CatalogueViewModel)
    viewModelOf(::ProductDetailViewModel)
    viewModelOf(::EnquiryViewModel)
}
