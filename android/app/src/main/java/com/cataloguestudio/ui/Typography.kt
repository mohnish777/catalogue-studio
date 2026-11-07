package com.cataloguestudio.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.cataloguestudio.R

private val DisplayFont = FontFamily(Font(R.font.instrument_serif))
private val BodyFont =
    FontFamily(
        Font(R.font.dm_sans, weight = FontWeight.Normal),
        Font(R.font.dm_sans, weight = FontWeight.Medium),
    )

private val Defaults = Typography()

internal val StudioTypography =
    Typography(
        displayLarge =
            Defaults.displayLarge.copy(
                fontFamily = DisplayFont,
                fontSize = 52.sp,
                lineHeight = 56.sp,
            ),
        displayMedium = Defaults.displayMedium.copy(fontFamily = DisplayFont),
        displaySmall = Defaults.displaySmall.copy(fontFamily = DisplayFont),
        headlineLarge =
            Defaults.headlineLarge.copy(
                fontFamily = DisplayFont,
                fontSize = 38.sp,
                lineHeight = 44.sp,
            ),
        headlineMedium = Defaults.headlineMedium.copy(fontFamily = DisplayFont),
        headlineSmall = Defaults.headlineSmall.copy(fontFamily = DisplayFont),
        titleLarge = Defaults.titleLarge.copy(fontFamily = BodyFont),
        titleMedium = Defaults.titleMedium.copy(fontFamily = BodyFont),
        titleSmall = Defaults.titleSmall.copy(fontFamily = BodyFont),
        bodyLarge = Defaults.bodyLarge.copy(fontFamily = BodyFont),
        bodyMedium = Defaults.bodyMedium.copy(fontFamily = BodyFont),
        bodySmall = Defaults.bodySmall.copy(fontFamily = BodyFont),
        labelLarge = Defaults.labelLarge.copy(fontFamily = BodyFont),
        labelMedium = Defaults.labelMedium.copy(fontFamily = BodyFont),
        labelSmall = Defaults.labelSmall.copy(fontFamily = BodyFont),
    )
