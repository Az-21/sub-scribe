package az21.subscribe.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

/** WCAG contrast ratio between two colors, from 1 (identical) to 21 (black on white). */
fun contrastRatio(
  first: Color,
  second: Color,
): Float {
  val lighter = max(first.luminance(), second.luminance())
  val darker = min(first.luminance(), second.luminance())
  return (lighter + LUMINANCE_OFFSET) / (darker + LUMINANCE_OFFSET)
}

/** Whether [foreground] reads clearly enough against [background] at the given [minRatio]. */
fun hasSufficientContrast(
  foreground: Color,
  background: Color,
  minRatio: Float,
): Boolean = contrastRatio(foreground, background) >= minRatio

/** The color with each RGB channel flipped, keeping its alpha. */
fun Color.inverted(): Color = Color(red = 1f - red, green = 1f - green, blue = 1f - blue, alpha = alpha)

private const val LUMINANCE_OFFSET = 0.05f
