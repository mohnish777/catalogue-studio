package com.cataloguestudio.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.cataloguestudio.R

val Paper = Color(0xFFF7F6F2)
val Ink = Color(0xFF252B25)
val Forest = Color(0xFF344D3B)
val Muted = Color(0xFF626A60)
val Line = Color(0xFFE1E3DA)
val Sage = Color(0xFFE8EDE5)
val Gold = Color(0xFF88642F)
val DisplayFont = FontFamily(Font(R.font.instrument_serif))
private val BodyFont =
    FontFamily(
        Font(R.font.dm_sans),
        Font(R.font.dm_sans, FontWeight.Medium),
        Font(R.font.dm_sans, FontWeight.SemiBold),
        Font(R.font.dm_sans, FontWeight.Bold),
    )

@Composable
fun StudioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme =
            lightColorScheme(
                primary = Forest,
                onPrimary = Color.White,
                background = Paper,
                onBackground = Ink,
                surface = Paper,
                onSurface = Ink,
                surfaceVariant = Sage,
                onSurfaceVariant = Muted,
                outline = Line,
                error = Color(0xFFA73B31),
            ),
        typography =
            Typography(
                displayLarge =
                    TextStyle(fontFamily = DisplayFont, fontSize = 52.sp, lineHeight = 54.sp),
                headlineLarge =
                    TextStyle(fontFamily = DisplayFont, fontSize = 38.sp, lineHeight = 40.sp),
                headlineMedium =
                    TextStyle(fontFamily = DisplayFont, fontSize = 30.sp, lineHeight = 34.sp),
                titleLarge =
                    TextStyle(
                        fontFamily = BodyFont,
                        fontSize = 20.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                titleMedium =
                    TextStyle(
                        fontFamily = BodyFont,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                bodyLarge = TextStyle(fontFamily = BodyFont, fontSize = 16.sp, lineHeight = 26.sp),
                bodyMedium = TextStyle(fontFamily = BodyFont, fontSize = 14.sp, lineHeight = 22.sp),
                bodySmall = TextStyle(fontFamily = BodyFont, fontSize = 12.sp, lineHeight = 18.sp),
                labelLarge =
                    TextStyle(
                        fontFamily = BodyFont,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                labelMedium =
                    TextStyle(
                        fontFamily = BodyFont,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.2.sp,
                    ),
                labelSmall =
                    TextStyle(
                        fontFamily = BodyFont,
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        letterSpacing = 1.4.sp,
                    ),
            ),
        content = content,
    )
}
