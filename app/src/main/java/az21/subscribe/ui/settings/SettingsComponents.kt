package az21.subscribe.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import az21.subscribe.R
import az21.subscribe.domain.model.ThemeMode
import az21.subscribe.domain.model.ThemeSeedSource
import az21.subscribe.domain.model.ThemeVariant

/** A labelled block of related settings, with the section title above its content. */
@Composable
fun SettingsSection(
  title: String,
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(text = title, style = MaterialTheme.typography.titleSmall)
    content()
  }
}

/**
 * A single-select group of connected [ToggleButton]s for a small set of options, following the
 * Material 3 Expressive connected button group spec. Pass [icon] to show a leading icon per option.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> ConnectedOptionGroup(
  options: List<T>,
  selected: T,
  label: @Composable (T) -> String,
  onSelect: (T) -> Unit,
  modifier: Modifier = Modifier,
  icon: (@Composable (T) -> ImageVector)? = null,
) {
  val contentPadding =
    if (icon != null) {
      ButtonDefaults.ButtonWithIconContentPadding
    } else {
      ButtonDefaults.ContentPadding
    }
  FlowRow(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    verticalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
  ) {
    options.forEachIndexed { index, option ->
      ToggleButton(
        checked = option == selected,
        onCheckedChange = { onSelect(option) },
        shapes = connectedShapes(index = index, count = options.size),
        contentPadding = contentPadding,
        modifier = Modifier.semantics { role = Role.RadioButton },
      ) {
        if (icon != null) {
          Icon(imageVector = icon(option), contentDescription = null)
          Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
        }
        Text(text = label(option), maxLines = 1)
      }
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun connectedShapes(
  index: Int,
  count: Int,
): ToggleButtonShapes =
  when {
    count == 1 -> ButtonGroupDefaults.connectedMiddleButtonShapes()
    index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
    index == count - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
  }

/** Trailing affordance for a row that navigates to another settings screen. */
@Composable
fun NavigationChevron() {
  Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
}

internal fun ThemeVariant.labelRes(): Int =
  when (this) {
    ThemeVariant.TONAL_SPOT -> R.string.variant_tonal_spot
    ThemeVariant.NEUTRAL -> R.string.variant_neutral
    ThemeVariant.VIBRANT -> R.string.variant_vibrant
    ThemeVariant.EXPRESSIVE -> R.string.variant_expressive
  }

internal fun ThemeMode.labelRes(): Int =
  when (this) {
    ThemeMode.SYSTEM -> R.string.settings_theme_mode_system
    ThemeMode.LIGHT -> R.string.settings_theme_mode_light
    ThemeMode.DARK -> R.string.settings_theme_mode_dark
  }

internal fun ThemeSeedSource.labelRes(): Int =
  when (this) {
    ThemeSeedSource.SYSTEM -> R.string.settings_theme_source_system
    ThemeSeedSource.MANUAL -> R.string.settings_theme_source_manual
  }

internal fun ThemeMode.icon(): ImageVector =
  when (this) {
    ThemeMode.SYSTEM -> Icons.Outlined.BrightnessAuto
    ThemeMode.LIGHT -> Icons.Outlined.LightMode
    ThemeMode.DARK -> Icons.Outlined.DarkMode
  }

internal fun ThemeSeedSource.icon(): ImageVector =
  when (this) {
    ThemeSeedSource.SYSTEM -> Icons.Outlined.Wallpaper
    ThemeSeedSource.MANUAL -> Icons.Outlined.ColorLens
  }
