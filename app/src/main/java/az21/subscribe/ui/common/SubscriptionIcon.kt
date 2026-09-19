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

/**
 * Renders a subscription's Simple Icons glyph, falling back to a monogram when the stored key has no
 * matching icon.
 */
@Composable
fun SubscriptionIcon(
  iconId: String?,
  name: String,
  modifier: Modifier = Modifier,
  tint: Color = MaterialTheme.colorScheme.primary,
  shape: Shape = CircleShape,
) {
  val vector = remember(iconId) { SimpleIconsCatalog.find(iconId) }
  if (vector != null) {
    Icon(
      imageVector = vector,
      contentDescription = null,
      tint = tint,
      modifier = modifier,
    )
  } else {
    val initial = name.trim().firstOrNull()?.uppercase() ?: "?"
    Box(
      modifier = modifier.background(MaterialTheme.colorScheme.secondaryContainer, shape),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = initial,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
      )
    }
  }
}
