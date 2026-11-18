package com.functional.spice.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.functional.spice.R

// Space Grotesk for all UI text: 700 titles, 600 labels, 500 body.
// (Plus Jakarta Sans stays in res/font: the printed footer strip uses it.)
val HeadingFontFamily = FontFamily(
    Font(R.font.space_grotesk_regular, FontWeight.Normal),
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_semibold, FontWeight.SemiBold),
    Font(R.font.space_grotesk_bold, FontWeight.Bold),
    Font(R.font.space_grotesk_bold, FontWeight.ExtraBold)
)

// Prices, totals, bill numbers and quantities use the same face with tabular figures,
// so columns of numbers still line up.
val MonospacePriceFont = HeadingFontFamily
private const val TABULAR = "tnum"

val SpiceTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = HeadingFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp,
        color = Charcoal
    ),
    headlineMedium = TextStyle(
        fontFamily = HeadingFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp,
        color = Charcoal
    ),
    headlineSmall = TextStyle(
        fontFamily = HeadingFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp,
        color = DeepEspresso
    ),
    titleLarge = TextStyle(
        fontFamily = HeadingFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp,
        color = DeepEspresso
    ),
    titleMedium = TextStyle(
        fontFamily = HeadingFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = Charcoal
    ),
    titleSmall = TextStyle(
        fontFamily = HeadingFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = Charcoal
    ),
    bodyLarge = TextStyle(
        fontFamily = HeadingFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = Charcoal
    ),
    bodyMedium = TextStyle(
        fontFamily = HeadingFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = Charcoal
    ),
    bodySmall = TextStyle(
        fontFamily = HeadingFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = MutedStone
    ),
    labelLarge = TextStyle(
        fontFamily = HeadingFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = Charcoal
    ),
    labelMedium = TextStyle(
        fontFamily = HeadingFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = MutedStone
    ),
    labelSmall = TextStyle(
        fontFamily = HeadingFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        color = MutedStone
    )
)

// JetBrains Mono styles for prices, totals, bill numbers and quantities
object MonoStyles {
    val Hero = TextStyle(
        fontFamily = MonospacePriceFont,
        fontFeatureSettings = TABULAR,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.8).sp,
        color = Charcoal
    )

    val PriceLarge = TextStyle(
        fontFamily = MonospacePriceFont,
        fontFeatureSettings = TABULAR,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        color = Charcoal
    )

    val PriceMedium = TextStyle(
        fontFamily = MonospacePriceFont,
        fontFeatureSettings = TABULAR,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        color = Charcoal
    )

    val PriceSmall = TextStyle(
        fontFamily = MonospacePriceFont,
        fontFeatureSettings = TABULAR,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = Charcoal
    )

    val Caption = TextStyle(
        fontFamily = MonospacePriceFont,
        fontFeatureSettings = TABULAR,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        color = MutedStone
    )

    val Key = TextStyle(
        fontFamily = MonospacePriceFont,
        fontFeatureSettings = TABULAR,
        fontWeight = FontWeight.Medium,
        fontSize = 19.sp,
        lineHeight = 24.sp,
        color = Charcoal
    )
}
