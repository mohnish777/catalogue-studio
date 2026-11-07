package com.cataloguestudio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.cataloguestudio.R
import com.cataloguestudio.presentation.CatalogueAction
import com.cataloguestudio.presentation.CategoryUi

@Composable
internal fun CatalogueControls(
    query: String,
    selectedCategory: String?,
    categories: List<CategoryUi>,
    onAction: (CatalogueAction) -> Unit,
    onExplore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
    )
    val submitSearch: () -> Unit = {
        keyboard?.hide()
        onAction(CatalogueAction.OnSearch)
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleMedium,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.catalogue_eyebrow), style = MaterialTheme.typography.labelLarge)
                Text(
                    text = stringResource(R.string.catalogue_headline),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text(stringResource(R.string.catalogue_intro), style = MaterialTheme.typography.bodyMedium)
                Button(
                    onClick = onExplore,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                ) {
                    Text(stringResource(R.string.catalogue_explore))
                }
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { onAction(CatalogueAction.OnQueryChange(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.catalogue_search_label)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { onAction(CatalogueAction.OnClearSearch) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_clear),
                                contentDescription = stringResource(R.string.catalogue_clear_search),
                            )
                        }
                    }
                } else null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submitSearch() }),
            )
            Button(onClick = submitSearch, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.catalogue_search))
            }
            Text(
                text = stringResource(R.string.catalogue_categories),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleSmall,
            )
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "all") {
                FilterChip(
                    selected = selectedCategory == null,
                    colors = chipColors,
                    onClick = { onAction(CatalogueAction.OnCategorySelect(null)) },
                    label = { Text(stringResource(R.string.catalogue_all_categories)) },
                )
            }
            items(categories, key = { "category:${it.value}" }) { category ->
                FilterChip(
                    selected = selectedCategory == category.value,
                    colors = chipColors,
                    onClick = { onAction(CatalogueAction.OnCategorySelect(category.value)) },
                    label = { Text(category.label.asString()) },
                )
            }
        }
    }
}
