package com.cataloguestudio.presentation.di

import com.cataloguestudio.presentation.CatalogueViewModel
import com.cataloguestudio.presentation.ProductDetailViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val cataloguePresentationModule = module {
    viewModelOf(::CatalogueViewModel)
    viewModelOf(::ProductDetailViewModel)
}
