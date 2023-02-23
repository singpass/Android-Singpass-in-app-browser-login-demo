package sg.ndi.sample.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.ExperimentalUnitApi

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