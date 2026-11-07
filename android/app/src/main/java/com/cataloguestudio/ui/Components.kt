package com.cataloguestudio.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.cataloguestudio.R
import com.cataloguestudio.presentation.ProductUi
import com.cataloguestudio.presentation.UiText
import com.cataloguestudio.presentation.categoryLabel

@Composable
fun UiText.asString(): String =
    when (this) {
        is UiText.Resource -> stringResource(id)
        is UiText.Dynamic -> text
    }

enum class Mark {
    SEARCH,
    BACK,
    ARROW,
    CLOSE,
    CHECK,
    GRID,
}

@Composable
fun StudioIcon(
    mark: Mark,
    modifier: Modifier = Modifier,
    tint: Color = Ink,
    description: String? = null,
) {
    Canvas(modifier.size(22.dp).semantics { description?.let { contentDescription = it } }) {
        val w = size.width
        val h = size.height
        val stroke = 1.7.dp.toPx()
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(tint, Offset(w * x1, h * y1), Offset(w * x2, h * y2), stroke)
        when (mark) {
            Mark.SEARCH -> {
                drawCircle(tint, w * .28f, Offset(w * .43f, h * .43f), style = Stroke(stroke))
                line(.64f, .64f, .9f, .9f)
            }
            Mark.ARROW -> {
                line(.15f, .5f, .85f, .5f)
                line(.6f, .25f, .85f, .5f)
                line(.6f, .75f, .85f, .5f)
            }
            Mark.BACK -> {
                line(.85f, .5f, .15f, .5f)
                line(.4f, .25f, .15f, .5f)
                line(.4f, .75f, .15f, .5f)
            }
            Mark.CLOSE -> {
                line(.25f, .25f, .75f, .75f)
                line(.75f, .25f, .25f, .75f)
            }
            Mark.CHECK -> {
                line(.2f, .5f, .42f, .72f)
                line(.42f, .72f, .82f, .28f)
            }
            Mark.GRID ->
                for (x in listOf(.2f, .6f)) for (y in listOf(.2f, .6f)) drawRect(
                    tint,
                    Offset(w * x, h * y),
                    androidx.compose.ui.geometry.Size(w * .2f, h * .2f),
                    style = Stroke(stroke),
                )
        }
    }
}

@Composable
fun ProductImage(product: ProductUi, modifier: Modifier = Modifier) {
    SubcomposeAsyncImage(
        model = product.imageUrl,
        contentDescription = product.title,
        modifier = modifier.background(Color.White),
        contentScale = ContentScale.Fit,
        loading = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 1.5.dp)
            }
        },
        error = {
            Column(
                Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                StudioIcon(Mark.GRID, tint = Muted)
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.image_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }
        },
    )
}

@Composable
fun ProductCard(product: ProductUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .testTag("product_${product.id}")
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClickLabel = stringResource(R.string.view_product), onClick = onClick)
    ) {
        Box(
            Modifier.fillMaxWidth()
                .height(176.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            ProductImage(product, Modifier.fillMaxSize().padding(22.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            categoryLabel(product.category).asString().uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(5.dp))
        Text(
            product.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(5.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                product.price,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(R.string.rating_short, product.rating),
                style = MaterialTheme.typography.bodySmall,
                color = Gold,
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), modifier, style = MaterialTheme.typography.labelMedium, color = Muted)
}

@Composable
fun SectionHeading(title: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        subtitle?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = Muted)
        }
    }
}

@Composable
fun LoadingState() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.loading),
            style = MaterialTheme.typography.bodyMedium,
            color = Muted,
        )
    }
}

@Composable
fun ErrorState(message: UiText, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.something_wrong),
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(12.dp))
        Text(message.asString(), style = MaterialTheme.typography.bodyMedium, color = Muted)
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.try_again)) }
    }
}

@Composable
fun PageHeader(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            StudioIcon(Mark.BACK, description = stringResource(R.string.go_back))
        }
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
        Text(
            stringResource(R.string.studio),
            Modifier.padding(end = 12.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Muted,
        )
    }
}

@Composable
fun ProductSummary(product: ProductUi) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Line, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProductImage(product, Modifier.size(64.dp).padding(6.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                product.title,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(product.price, style = MaterialTheme.typography.bodyMedium, color = Muted)
        }
    }
}

val previewProduct =
    ProductUi(
        1,
        "Everyday canvas backpack",
        "A durable canvas companion for the everyday. Generous storage and a clean silhouette.",
        "$109.95",
        "men's clothing",
        "",
        "4.7",
        120,
    )
