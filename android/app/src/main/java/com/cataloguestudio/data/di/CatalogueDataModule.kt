package com.cataloguestudio.data.di

import com.cataloguestudio.data.KtorCatalogueDataSource
import com.cataloguestudio.data.createCatalogueHttpClient
import com.cataloguestudio.data.createCatalogueHttpEngine
import com.cataloguestudio.domain.CatalogueRemoteDataSource
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import org.koin.dsl.module
import org.koin.dsl.onClose

internal fun catalogueDataModule(
    baseUrl: String,
    engineFactory: () -> HttpClientEngine = ::createCatalogueHttpEngine,
) = module {
    // HttpClient(engine) does not own the supplied engine; register cleanup for both.
    single<HttpClientEngine> { engineFactory() } onClose { it?.close() }
    single<HttpClient> { createCatalogueHttpClient(baseUrl, get()) } onClose { it?.close() }
    single<CatalogueRemoteDataSource> { KtorCatalogueDataSource(get(), baseUrl) }
}
