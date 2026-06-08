package com.example.personaltrainer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = PT_Primary,
    secondary = PT_Secondary,
    tertiary = PT_Tertiary,
    background = PT_Background_Light,
    surface = PT_Surface_Light,
)

private val DarkColors = darkColorScheme(
    primary = PT_Primary_Dark,
    secondary = PT_Secondary_Dark,
    tertiary = PT_Tertiary_Dark,
    background = PT_Background_Dark,
    surface = PT_Surface_Dark,
)

@Composable
fun PersonaltrainerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // dynamicColor можно оставить true — на Pixel будет красиво, на остальных ок.
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
