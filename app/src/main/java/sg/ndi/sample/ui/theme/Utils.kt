package sg.ndi.sample.ui.theme

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import sg.ndi.sample.R

object Utils {

    fun setSystemBarsColors(
        context: ComponentActivity,
        @ColorRes statusBarColor: Int = android.R.color.transparent,
        @ColorRes navigationBarColor: Int? = android.R.color.transparent,
        lightSystemIcons: Boolean? = null
    ) {

        val resolvedStatusBarColor = ContextCompat.getColor(context, statusBarColor)
        val resolvedNavigationBarColor = navigationBarColor?.run { ContextCompat.getColor(context, this) }

        setSystemBarsColorInts(
            context = context,
            statusBarColor = resolvedStatusBarColor,
            navigationBarColor = resolvedNavigationBarColor,
            lightSystemIcons = lightSystemIcons
        )
    }

    private fun isDarkTheme(context: Context): Boolean {
        return context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    private fun Context.getNavBarColorInt(@ColorInt color: Int?): Int {
        val sdkInt = Build.VERSION.SDK_INT

        return when {
            // greater than android 10 (supports gesture navigation)
            // will automatically apply a scrim if color is transparent and is non-gesture navigation
            sdkInt >= Build.VERSION_CODES.Q -> color ?: ContextCompat.getColor(this, android.R.color.transparent)
            // use a translucent light or dark scrim color, devices >= O supports dark/light nav bar icons
            sdkInt >= Build.VERSION_CODES.O_MR1 -> color ?: ContextCompat.getColor(this, R.color.translucent_statusbar_navbar)
            // use a translucent dark scrim color
            else -> ContextCompat.getColor(this, R.color.translucent)
        }
    }

    fun setSystemBarsColorInts(
        context: ComponentActivity,
        @ColorInt statusBarColor: Int = Color.TRANSPARENT,
        @ColorInt navigationBarColor: Int? = null,
        lightSystemIcons: Boolean? = null
    ) {
        context.run {
            val isDarkTheme = isDarkTheme(context)

            if (navigationBarColor != null) {

                val resolvedNavigationBarColor = context.getNavBarColorInt(navigationBarColor)

                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(statusBarColor, statusBarColor) {  lightSystemIcons ?: isDarkTheme },
                    navigationBarStyle = if (isDarkTheme || lightSystemIcons == true) {
                        SystemBarStyle.dark(resolvedNavigationBarColor)
                    } else {
                        SystemBarStyle.light(scrim = resolvedNavigationBarColor, darkScrim = resolvedNavigationBarColor)
                    }
                )
            } else {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(statusBarColor, statusBarColor) {  lightSystemIcons ?: isDarkTheme }
                )
            }
        }
    }
}