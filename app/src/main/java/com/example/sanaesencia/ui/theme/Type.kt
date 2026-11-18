package com.example.sanaesencia.ui.theme
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.example.sanaesencia.R
private val Lato=FontFamily(Font(R.font.lato_regular))
private val Base=Typography()
val Typography=Typography(
    displayLarge=Base.displayLarge.copy(fontFamily=Lato), displayMedium=Base.displayMedium.copy(fontFamily=Lato),displaySmall=Base.displaySmall.copy(fontFamily=Lato),
    headlineLarge=Base.headlineLarge.copy(fontFamily=Lato),headlineMedium=Base.headlineMedium.copy(fontFamily=Lato),headlineSmall=Base.headlineSmall.copy(fontFamily=Lato),
    titleLarge=Base.titleLarge.copy(fontFamily=Lato),titleMedium=Base.titleMedium.copy(fontFamily=Lato),titleSmall=Base.titleSmall.copy(fontFamily=Lato),
    bodyLarge=Base.bodyLarge.copy(fontFamily=Lato),bodyMedium=Base.bodyMedium.copy(fontFamily=Lato),bodySmall=Base.bodySmall.copy(fontFamily=Lato),
    labelLarge=Base.labelLarge.copy(fontFamily=Lato),labelMedium=Base.labelMedium.copy(fontFamily=Lato),labelSmall=Base.labelSmall.copy(fontFamily=Lato))
