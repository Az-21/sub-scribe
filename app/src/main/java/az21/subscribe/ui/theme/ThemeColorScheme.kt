package az21.subscribe.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import az21.subscribe.domain.model.ThemeVariant
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.dynamiccolor.DynamicColor
import com.materialkolor.dynamiccolor.MaterialDynamicColors
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.DynamicScheme
import com.materialkolor.scheme.SchemeExpressive
import com.materialkolor.scheme.SchemeNeutral
import com.materialkolor.scheme.SchemeTonalSpot
import com.materialkolor.scheme.SchemeVibrant

/** Standard (non-boosted) contrast, matching the Material 3 default. */
private const val DEFAULT_CONTRAST_LEVEL = 0.0

/**
 * Builds a full Material 3 Expressive [ColorScheme] from [seedColor] (ARGB), a [variant] and the
 * requested brightness, using the 2025 Material color spec. Pure and deterministic so it can be
 * unit tested on the JVM.
 */
@Suppress(
  "LongMethod", // Flat one-role-per-line mapping of the ColorScheme constructor; splitting hurts clarity.
)
fun themeColorScheme(
  seedColor: Int,
  variant: ThemeVariant,
  isDark: Boolean,
): ColorScheme {
  val scheme = dynamicScheme(seedColor = seedColor, variant = variant, isDark = isDark)
  val colors = MaterialDynamicColors()

  fun role(color: DynamicColor): Color = Color(color.getArgb(scheme))
  return ColorScheme(
    primary = role(colors.primary()),
    onPrimary = role(colors.onPrimary()),
    primaryContainer = role(colors.primaryContainer()),
    onPrimaryContainer = role(colors.onPrimaryContainer()),
    inversePrimary = role(colors.inversePrimary()),
    secondary = role(colors.secondary()),
    onSecondary = role(colors.onSecondary()),
    secondaryContainer = role(colors.secondaryContainer()),
    onSecondaryContainer = role(colors.onSecondaryContainer()),
    tertiary = role(colors.tertiary()),
    onTertiary = role(colors.onTertiary()),
    tertiaryContainer = role(colors.tertiaryContainer()),
    onTertiaryContainer = role(colors.onTertiaryContainer()),
    background = role(colors.background()),
    onBackground = role(colors.onBackground()),
    surface = role(colors.surface()),
    onSurface = role(colors.onSurface()),
    surfaceVariant = role(colors.surfaceVariant()),
    onSurfaceVariant = role(colors.onSurfaceVariant()),
    surfaceTint = role(colors.surfaceTint()),
    inverseSurface = role(colors.inverseSurface()),
    inverseOnSurface = role(colors.inverseOnSurface()),
    error = role(colors.error()),
    onError = role(colors.onError()),
    errorContainer = role(colors.errorContainer()),
    onErrorContainer = role(colors.onErrorContainer()),
    outline = role(colors.outline()),
    outlineVariant = role(colors.outlineVariant()),
    scrim = role(colors.scrim()),
    surfaceBright = role(colors.surfaceBright()),
    surfaceDim = role(colors.surfaceDim()),
    surfaceContainer = role(colors.surfaceContainer()),
    surfaceContainerHigh = role(colors.surfaceContainerHigh()),
    surfaceContainerHighest = role(colors.surfaceContainerHighest()),
    surfaceContainerLow = role(colors.surfaceContainerLow()),
    surfaceContainerLowest = role(colors.surfaceContainerLowest()),
    primaryFixed = role(colors.primaryFixed()),
    primaryFixedDim = role(colors.primaryFixedDim()),
    onPrimaryFixed = role(colors.onPrimaryFixed()),
    onPrimaryFixedVariant = role(colors.onPrimaryFixedVariant()),
    secondaryFixed = role(colors.secondaryFixed()),
    secondaryFixedDim = role(colors.secondaryFixedDim()),
    onSecondaryFixed = role(colors.onSecondaryFixed()),
    onSecondaryFixedVariant = role(colors.onSecondaryFixedVariant()),
    tertiaryFixed = role(colors.tertiaryFixed()),
    tertiaryFixedDim = role(colors.tertiaryFixedDim()),
    onTertiaryFixed = role(colors.onTertiaryFixed()),
    onTertiaryFixedVariant = role(colors.onTertiaryFixedVariant()),
  )
}

private fun dynamicScheme(
  seedColor: Int,
  variant: ThemeVariant,
  isDark: Boolean,
): DynamicScheme {
  val sourceColor = Hct.fromInt(seedColor)
  val specVersion = ColorSpec.SpecVersion.SPEC_2025
  return when (variant) {
    ThemeVariant.TONAL_SPOT -> SchemeTonalSpot(sourceColor, isDark, DEFAULT_CONTRAST_LEVEL, specVersion)
    ThemeVariant.NEUTRAL -> SchemeNeutral(sourceColor, isDark, DEFAULT_CONTRAST_LEVEL, specVersion)
    ThemeVariant.VIBRANT -> SchemeVibrant(sourceColor, isDark, DEFAULT_CONTRAST_LEVEL, specVersion)
    ThemeVariant.EXPRESSIVE -> SchemeExpressive(sourceColor, isDark, DEFAULT_CONTRAST_LEVEL, specVersion)
  }
}
