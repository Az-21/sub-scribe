package az21.subscribe.ui.theme

import androidx.compose.ui.graphics.luminance
import az21.subscribe.domain.model.ThemeVariant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeColorSchemeTest {
  @Test
  fun isDeterministic_forSameInputs() {
    val first = themeColorScheme(seedColor, ThemeVariant.TONAL_SPOT, isDark = false)
    val second = themeColorScheme(seedColor, ThemeVariant.TONAL_SPOT, isDark = false)

    assertEquals(first.primary, second.primary)
    assertEquals(first.surface, second.surface)
    assertEquals(first.onPrimary, second.onPrimary)
  }

  @Test
  fun lightAndDarkSchemesDiffer() {
    val light = themeColorScheme(seedColor, ThemeVariant.TONAL_SPOT, isDark = false)
    val dark = themeColorScheme(seedColor, ThemeVariant.TONAL_SPOT, isDark = true)

    assertNotEquals(light.surface, dark.surface)
    assertTrue(dark.surface.luminance() < light.surface.luminance())
    assertTrue(dark.onSurface.luminance() > light.onSurface.luminance())
  }

  @Test
  fun eachVariantProducesADistinctScheme() {
    val signatures =
      ThemeVariant.entries.map { variant ->
        val scheme = themeColorScheme(seedColor, variant, isDark = false)
        listOf(scheme.primary, scheme.secondary, scheme.tertiary, scheme.surfaceContainer)
      }

    assertEquals(ThemeVariant.entries.size, signatures.toSet().size)
  }

  @Test
  fun manualSeedChangesPrimary_comparedToFallbackSeed() {
    val fallback = themeColorScheme(FallbackSeedColor, ThemeVariant.TONAL_SPOT, isDark = false)
    val manual = themeColorScheme(manualSeedColor, ThemeVariant.TONAL_SPOT, isDark = false)

    assertNotEquals(fallback.primary, manual.primary)
  }

  private companion object {
    val seedColor = 0xFF006A6A.toInt()
    val manualSeedColor = 0xFFB3261E.toInt()
  }
}
