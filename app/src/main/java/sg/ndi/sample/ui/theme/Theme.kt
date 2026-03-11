package sg.ndi.sample.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.dp

private val DarkColorPalette = darkColors(
    primary = primaryDark,
    primaryVariant = primaryVariant,
    secondary = secondaryDark,
    secondaryVariant = secondaryDark,
    background = Grey80,
    surface = Grey60,
    onPrimary = Grey20,
    onSecondary = Grey90,
    onBackground = Grey10,
    onSurface = Grey20,
)

private val LightColorPalette = lightColors(
    primary = primary,
    primaryVariant = primaryVariant,
    secondary = secondary,
    secondaryVariant = secondaryVariant,
    background = Grey30,
    surface = Grey10,
    onPrimary = Grey10,
    onSecondary = Grey90,
    onBackground = Grey90,
    onSurface = Grey80
)

@OptIn(ExperimentalUnitApi::class)
@Composable
fun NDIRpSampleTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = typography,
        shapes = Shapes,
        content = content
    )
}

val AppLightColorScheme = lightColorScheme(
    // M3 light Color parameters

)
val AppDarkColorScheme = darkColorScheme(
    // M3 dark Color parameters
)

val LocalCardElevation = staticCompositionLocalOf { Dp.Unspecified }

@Composable
fun NDiSampleM3Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val cardElevation = if (darkTheme) 4.dp else 0.dp
    CompositionLocalProvider(LocalCardElevation provides cardElevation) {
        val appColorScheme = if (darkTheme) {
            AppDarkColorScheme
        } else {
            AppLightColorScheme
        }
         androidx.compose.material3.MaterialTheme(
            colorScheme = appColorScheme,
            content = content
        )
    }
}