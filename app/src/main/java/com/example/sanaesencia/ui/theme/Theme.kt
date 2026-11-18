package com.example.sanaesencia.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val PastelColorScheme = lightColorScheme(
    primary = SageGreen,
    onPrimary = SurfaceWhite,
    primaryContainer = SagePastel,
    onPrimaryContainer = TextSoftDark,
    secondary = SereneBlue,
    onSecondary = SurfaceWhite,
    secondaryContainer = SerenePastel,
    onSecondaryContainer = TextSoftDark,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFEDF3F8),
    surfaceContainer = androidx.compose.ui.graphics.Color(0xFFF0F4F8),
    surfaceContainerLow = androidx.compose.ui.graphics.Color(0xFFF7F9FC),
    surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFFE7EEF5),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFFC3CED9),
    onSurfaceVariant = TextSoftDark,
    outline = androidx.compose.ui.graphics.Color(0xFF71796F),
    background = BackgroundSoft,
    onBackground = TextSoftDark,
    surface = SurfaceWhite,
    onSurface = TextSoftDark,
    surfaceTint = SageGreen,
    error = ErrorSoftRed,
    onError = SurfaceWhite
)

@Composable
fun SanaesenciaTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = PastelColorScheme.background.toArgb()

            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = PastelColorScheme,
        typography = Typography,
        content = content
    )
}
