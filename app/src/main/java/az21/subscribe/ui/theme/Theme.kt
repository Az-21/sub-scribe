package az21.subscribe.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import az21.subscribe.domain.model.AppSettings
import az21.subscribe.domain.model.ThemeMode
import az21.subscribe.domain.model.ThemeSeedSource

/**
 * Single source of theming for the app. Recomputes the Material 3 Expressive [ColorScheme]
 * reactively whenever the persisted [AppSettings] (seed color, style variant, brightness) change,
 * so theme edits apply live without an app restart.
 */
@Composable
fun AppTheme(
  settings: AppSettings = AppSettings(),
  content: @Composable () -> Unit,
) {
  val isDark = resolveDarkTheme(settings.themeMode)
  val context = LocalContext.current
  val seedColor =
    remember(settings.themeSeedSource, settings.themeSeedColor, context) {
      resolveSeedColor(settings, context)
    }
  val colorScheme =
    remember(seedColor, settings.themeVariant, isDark) {
      themeColorScheme(seedColor = seedColor, variant = settings.themeVariant, isDark = isDark)
    }
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

@Composable
private fun resolveDarkTheme(mode: ThemeMode): Boolean =
  when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
  }

private fun resolveSeedColor(
  settings: AppSettings,
  context: Context,
): Int {
  val manualColor = settings.themeSeedColor
  return when {
    settings.themeSeedSource == ThemeSeedSource.MANUAL && manualColor != null -> manualColor
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context).primary.toArgb()
    else -> FallbackSeedColor
  }
}
