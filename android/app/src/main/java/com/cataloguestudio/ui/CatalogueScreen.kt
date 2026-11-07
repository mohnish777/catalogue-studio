package com.cataloguestudio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cataloguestudio.R
import com.cataloguestudio.presentation.*
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun CatalogueRoot(onProduct: (Int) -> Unit, viewModel: CatalogueViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is CatalogueEvent.OpenProduct -> onProduct(event.id)
        }
    }
    CatalogueScreen(state, viewModel::onAction)
}

@Composable
fun CatalogueScreen(state: CatalogueState, onAction: (CatalogueAction) -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    Scaffold(
        containerColor = Paper,
        topBar = {
            Row(
                Modifier.fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.brand),
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        stringResource(R.string.brand_note),
                        style = MaterialTheme.typography.labelSmall,
                        color = Muted,
                    )
                }
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(Sage),
                    contentAlignment = Alignment.Center,
                ) {
                    StudioIcon(Mark.GRID, tint = Forest)
                }
            }
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(140.dp),
            state = gridState,
            modifier = Modifier.fillMaxSize().padding(padding).testTag("catalogue_grid"),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    if (state.category == null && state.submittedSearch.isEmpty()) {
                        HeroCard(state.featured.firstOrNull()) {
                            scope.launch { gridState.animateScrollToItem(1) }
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = { onAction(CatalogueAction.QueryChanged(it)) },
                        modifier = Modifier.fillMaxWidth().testTag("search_input"),
                        singleLine = true,
                        placeholder = {
                            Text(
                                stringResource(R.string.search_hint),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        leadingIcon = { StudioIcon(Mark.SEARCH, tint = Muted) },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (state.query.isNotEmpty())
                                    IconButton(
                                        onClick = { onAction(CatalogueAction.ClearSearch) }
                                    ) {
                                        StudioIcon(
                                            Mark.CLOSE,
                                            description = stringResource(R.string.clear_search),
                                        )
                                    }
                                IconButton(
                                    onClick = {
                                        keyboard?.hide()
                                        onAction(CatalogueAction.Search)
                                    },
                                    modifier =
                                        Modifier.padding(end = 6.dp)
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Forest),
                                ) {
                                    StudioIcon(
                                        Mark.ARROW,
                                        tint = Color.White,
                                        description = stringResource(R.string.search),
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions =
                            KeyboardActions(
                                onSearch = {
                                    keyboard?.hide()
                                    onAction(CatalogueAction.Search)
                                }
                            ),
                        shape = RoundedCornerShape(16.dp),
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color.White,
                                focusedContainerColor = Color.White,
                                unfocusedBorderColor = Line,
                            ),
                    )
                    Spacer(Modifier.height(16.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        item {
                            FilterChip(
                                selected = state.category == null,
                                onClick = { onAction(CatalogueAction.SelectCategory(null)) },
                                label = { Text(stringResource(R.string.category_all)) },
                                shape = CircleShape,
                                colors = chipColors(),
                            )
                        }
                        items(state.categories, key = { it }) { category ->
                            FilterChip(
                                selected = state.category == category,
                                onClick = { onAction(CatalogueAction.SelectCategory(category)) },
                                label = { Text(categoryLabel(category).asString()) },
                                shape = CircleShape,
                                colors = chipColors(),
                            )
                        }
                    }
                }
            }
            when {
                state.loading -> item(span = { GridItemSpan(maxLineSpan) }) { LoadingState() }
                state.error != null ->
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        ErrorState(state.error) { onAction(CatalogueAction.Retry) }
                    }
                else -> {
                    if (state.featured.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Column {
                                SectionHeading(
                                    stringResource(
                                        if (state.category == null) R.string.featured_title
                                        else R.string.category_featured_title
                                    ),
                                    stringResource(
                                        if (state.category == null) R.string.featured_subtitle
                                        else R.string.category_featured_subtitle
                                    ),
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    items(state.featured, key = { it.id }) { product ->
                                        ProductCard(
                                            product,
                                            { onAction(CatalogueAction.OpenProduct(product.id)) },
                                            Modifier.width(168.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SectionHeading(
                            when {
                                state.submittedSearch.isNotEmpty() ->
                                    stringResource(R.string.search_results, state.submittedSearch)
                                state.category != null -> categoryLabel(state.category).asString()
                                else -> stringResource(R.string.collection_title)
                            },
                            pluralStringResource(
                                R.plurals.pieces_count,
                                state.products.size,
                                state.products.size,
                            ),
                        )
                    }
                    if (state.products.isEmpty() && state.featured.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Column(
                                Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    stringResource(R.string.no_results_title),
                                    style = MaterialTheme.typography.headlineMedium,
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    stringResource(R.string.no_results_body),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Muted,
                                )
                                Spacer(Modifier.height(16.dp))
                                OutlinedButton(
                                    onClick = { onAction(CatalogueAction.SelectCategory(null)) }
                                ) {
                                    Text(stringResource(R.string.clear_filters))
                                }
                            }
                        }
                    }
                    items(state.products, key = { it.id }) { product ->
                        ProductCard(product, { onAction(CatalogueAction.OpenProduct(product.id)) })
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(Modifier.fillMaxWidth().padding(top = 28.dp)) {
                            HorizontalDivider(color = Line)
                            Spacer(Modifier.height(24.dp))
                            Text(
                                stringResource(R.string.footer_title),
                                style = MaterialTheme.typography.headlineMedium,
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                stringResource(R.string.footer_body),
                                style = MaterialTheme.typography.bodySmall,
                                color = Muted,
                            )
                            Spacer(Modifier.height(20.dp))
                            Eyebrow(stringResource(R.string.prices_note))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun chipColors() =
    FilterChipDefaults.filterChipColors(
        selectedContainerColor = Forest,
        selectedLabelColor = Color.White,
        containerColor = Paper,
        labelColor = Muted,
    )

@Composable
private fun HeroCard(product: ProductUi?, onExplore: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Sage)
            .clickable(onClick = onExplore)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1.25f).padding(end = 10.dp)) {
            Eyebrow(stringResource(R.string.collection_label))
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.hero_title),
                style = MaterialTheme.typography.headlineLarge,
                color = Forest,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.hero_body),
                style = MaterialTheme.typography.bodySmall,
                color = Forest,
            )
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.hero_action),
                    style = MaterialTheme.typography.labelSmall,
                    color = Forest,
                )
                Spacer(Modifier.width(8.dp))
                StudioIcon(Mark.ARROW, Modifier.size(16.dp), tint = Forest)
            }
        }
        if (product != null)
            Box(
                Modifier.weight(.8f)
                    .height(180.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 60.dp,
                            topEnd = 60.dp,
                            bottomStart = 14.dp,
                            bottomEnd = 14.dp,
                        )
                    )
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                ProductImage(product, Modifier.fillMaxSize().padding(14.dp))
            }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 900)
@Composable
private fun CataloguePreview() = StudioTheme {
    CatalogueScreen(
        CatalogueState(
            loading = false,
            categories = listOf("men's clothing", "jewelery", "electronics"),
            featured = listOf(previewProduct),
            products = listOf(previewProduct.copy(id = 2, title = "Essential cotton henley")),
        ),
        {},
    )
}
