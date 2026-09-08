package oblivion.v2.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val OblivionColorScheme = darkColorScheme(
    primary = OblivionRed,
    onPrimary = Color.White,
    primaryContainer = OblivionPrimaryContainer,
    onPrimaryContainer = OblivionOnPrimaryContainer,

    secondary = OblivionRedLight,
    onSecondary = Color.White,
    secondaryContainer = OblivionSecondaryContainer,
    onSecondaryContainer = OblivionOnSecondaryContainer,

    tertiary = OblivionRedDark,
    onTertiary = Color.White,

    background = OblivionBlack,
    onBackground = OblivionOnSurface,

    surface = OblivionSurface,
    onSurface = OblivionOnSurface,
    surfaceVariant = OblivionSurfaceVariant,
    onSurfaceVariant = OblivionOnSurfaceVariant,

    error = OblivionRed,
    onError = Color.White,
    errorContainer = OblivionErrorContainer,
    onErrorContainer = OblivionOnErrorContainer,

    outline = OblivionOutline,
    outlineVariant = OblivionOutlineVariant,
)

@Composable
fun OblivionTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = OblivionColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = OblivionBlack.toArgb()
            window.navigationBarColor = OblivionBlack.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
