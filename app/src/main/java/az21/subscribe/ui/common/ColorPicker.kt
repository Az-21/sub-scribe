package az21.subscribe.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import az21.subscribe.R

/**
 * A row of color choices: a leading custom picker swatch that opens [ColorPickerDialog], followed by
 * the [PresetColors] palette. The custom swatch is selected when [selected] is set but is not one of
 * the presets, and shows [fallbackColor] until the user picks something.
 */
@Composable
fun ColorPicker(
  selected: Int?,
  fallbackColor: Int,
  customContentDescription: String,
  presetContentDescription: String,
  onColorChange: (Int) -> Unit,
  modifier: Modifier = Modifier,
  swatchSize: Dp = ColorSwatchSize,
) {
  var showPicker by remember { mutableStateOf(false) }
  FlowRow(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(ColorSwatchSpacing),
    verticalArrangement = Arrangement.spacedBy(ColorSwatchSpacing),
  ) {
    ColorSwatch(
      color = selected ?: fallbackColor,
      selected = selected != null && selected !in PresetColors,
      onClickLabel = customContentDescription,
      onClick = { showPicker = true },
      badgeIcon = Icons.Outlined.Colorize,
      modifier = Modifier.size(swatchSize),
    )
    PresetColors.forEach { color ->
      ColorSwatch(
        color = color,
        selected = selected == color,
        onClickLabel = presetContentDescription,
        onClick = { onColorChange(color) },
        modifier = Modifier.size(swatchSize),
      )
    }
  }
  if (showPicker) {
    ColorPickerDialog(
      initialColor = selected ?: fallbackColor,
      onConfirm = { color ->
        onColorChange(color)
        showPicker = false
      },
      onDismiss = { showPicker = false },
    )
  }
}

@Composable
private fun ColorSwatch(
  color: Int,
  selected: Boolean,
  onClickLabel: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  badgeIcon: ImageVector? = null,
) {
  Box(
    modifier =
      modifier
        .clip(CircleShape)
        .background(Color(color))
        .clickable(onClickLabel = onClickLabel, role = Role.RadioButton, onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    if (badgeIcon != null) {
      Box(
        modifier =
          Modifier
            .size(BadgeDiameter)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = BADGE_SCRIM_ALPHA), CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = badgeIcon,
          contentDescription = null,
          modifier = Modifier.size(BadgeIconSize),
          tint = MaterialTheme.colorScheme.onSurface,
        )
      }
    }
    if (selected) {
      Box(
        modifier =
          Modifier.matchParentSize().border(SelectionStroke, MaterialTheme.colorScheme.onSurface, CircleShape),
      )
    }
  }
}

val ColorSwatchSize: Dp = 52.dp
private val ColorSwatchSpacing = 12.dp
private val SelectionStroke = 3.dp
private val BadgeDiameter = 24.dp
private val BadgeIconSize = 16.dp
private const val BADGE_SCRIM_ALPHA = 0.85f
