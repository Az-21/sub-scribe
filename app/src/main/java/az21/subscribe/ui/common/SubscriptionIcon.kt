package az21.subscribe.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance

/**
 * Renders a subscription's icon glyph from the combined Simple Icons and Material Icons catalog,
 * falling back to a monogram when the stored key has no matching icon. [iconColor] overrides the
 * icon's default color; when null the icon's brand color (or the theme for Material Icons) is used.
 *
 * The tint is inverted at draw time when it would otherwise be too close to the surface. This is a
 * display-only adjustment: the stored color is never modified.
 */
@Composable
fun SubscriptionIcon(
  iconId: String?,
  name: String,
  modifier: Modifier = Modifier,
  iconColor: Int? = null,
  shape: Shape = CircleShape,
) {
  val entry = remember(iconId) { IconCatalog.findEntry(iconId) }
  val vector = entry?.vector
  val customColor = (iconColor ?: entry?.defaultColor)?.let(::Color)
  if (vector != null) {
    Icon(
      imageVector = vector,
      contentDescription = null,
      tint = readableOnSurface(customColor ?: MaterialTheme.colorScheme.primary),
      modifier = modifier,
    )
  } else {
    val initial = name.trim().firstOrNull()?.uppercase() ?: "?"
    Box(
      modifier = modifier.background(customColor ?: MaterialTheme.colorScheme.secondaryContainer, shape),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = initial,
        style = MaterialTheme.typography.titleMedium,
        color = customColor?.let { onContentColorFor(it) } ?: MaterialTheme.colorScheme.onSecondaryContainer,
      )
    }
  }
}

/** Keeps [color] when it contrasts with the surface, otherwise flips it so the glyph stays visible. */
@Composable
private fun readableOnSurface(color: Color): Color =
  if (hasSufficientContrast(color, MaterialTheme.colorScheme.surface, MIN_ICON_CONTRAST)) {
    color
  } else {
    color.inverted()
  }

/** Picks a readable foreground for [background]: black on light colors, white on dark ones. */
private fun onContentColorFor(background: Color): Color =
  if (background.luminance() > LIGHT_LUMINANCE_THRESHOLD) Color.Black else Color.White

private const val LIGHT_LUMINANCE_THRESHOLD = 0.5f
private const val MIN_ICON_CONTRAST = 3.0f
