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
    onPrimary = TextSoftDark,
    primaryContainer = SagePastel,
    onPrimaryContainer = TextSoftDark,
    secondary = SereneBlue,
    onSecondary = TextSoftDark,
    secondaryContainer = SerenePastel,
    onSecondaryContainer = TextSoftDark,
    surfaceVariant = SerenePastel,
    surfaceContainer = androidx.compose.ui.graphics.Color(0xFFEFF8F9),
    surfaceContainerLow = androidx.compose.ui.graphics.Color(0xFFF7FBFC),
    surfaceContainerHigh = SagePastel,
    surfaceContainerHighest = androidx.compose.ui.graphics.Color(0xFFDFF1F4),
    surfaceBright = SurfaceWhite,
    surfaceDim = androidx.compose.ui.graphics.Color(0xFFDBE9EB),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFFBBDDE2),
    onSurfaceVariant = TextSoftDark,
    outline = androidx.compose.ui.graphics.Color(0xFF6E6E73),
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
