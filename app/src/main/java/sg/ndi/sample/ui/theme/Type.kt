package sg.ndi.sample.ui.theme

import androidx.compose.material.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp
import sg.ndi.sample.R

private val Poppins = FontFamily(
    Font(R.font.poppins_regular),
    Font(R.font.poppins_italic, style = FontStyle.Italic),
    Font(R.font.poppins_medium, FontWeight.W500),
    Font(R.font.poppins_semibold, FontWeight.W600),
    Font(R.font.poppins_bold, FontWeight.W700),
)

@OptIn(ExperimentalTextApi::class)
@ExperimentalUnitApi
val typography = Typography(
    defaultFontFamily = Poppins,
    h1 = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        letterSpacing = 0.sp,
        lineHeight = TextUnit(36f, TextUnitType.Sp),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    h2 = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        letterSpacing = 0.sp,
        lineHeight = TextUnit(32f, TextUnitType.Sp),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    h3 = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        letterSpacing = 0.sp,
        lineHeight = TextUnit(32f, TextUnitType.Sp),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    h4 = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        letterSpacing = 0.sp,
        lineHeight = TextUnit(28f, TextUnitType.Sp),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    h5 = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        letterSpacing = 0.sp,
        lineHeight = TextUnit(24f, TextUnitType.Sp),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    h6 = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        letterSpacing = 0.sp,
        lineHeight = TextUnit(24f, TextUnitType.Sp),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    // Subheading
    subtitle1 = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.sp,
        lineHeight = TextUnit(20f, TextUnitType.Sp),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    // Title
    subtitle2 = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp,
        lineHeight = TextUnit(24f, TextUnitType.Sp),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    // Body base
    body1 = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.sp,
        lineHeight = TextUnit(24f, TextUnitType.Sp),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    // Body small
    body2 = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.sp,
        lineHeight = TextUnit(20f, TextUnitType.Sp),
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    ),
    // button base
    button = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        letterSpacing = 0.sp,
        lineHeight = TextUnit(20f, TextUnitType.Sp),
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    ),
    caption = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    ),
    overline = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = 1.sp,
        lineHeight = TextUnit(20f, TextUnitType.Sp),
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )
)
