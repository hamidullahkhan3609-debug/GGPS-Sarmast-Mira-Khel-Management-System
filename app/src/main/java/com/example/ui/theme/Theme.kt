package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = DarkNavyPrimary,
    secondary = DarkNavySecondary,
    tertiary = SchoolGold,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color(0xFF002244),
    onSecondary = Color(0xFF002244),
    onTertiary = Color(0xFF3E2000),
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = SchoolNavyPrimary,
    secondary = SchoolNavySecondary,
    tertiary = SchoolGold,
    background = SchoolSurface,
    surface = SchoolCardBg,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = SchoolTextDark,
    onSurface = SchoolTextDark,
    primaryContainer = Color(0xFFE0EBF7),
    onPrimaryContainer = SchoolNavyPrimary,
    secondaryContainer = Color(0xFFD6EFF8),
    onSecondaryContainer = SchoolNavySecondary,
    tertiaryContainer = SchoolGoldContainer,
    onTertiaryContainer = Color(0xFF5A3E00),
  )

@Composable
fun GgpsTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Keep school branding consistent
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) = GgpsTheme(darkTheme, dynamicColor, content)
