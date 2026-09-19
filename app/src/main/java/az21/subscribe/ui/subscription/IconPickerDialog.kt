package az21.subscribe.ui.subscription

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import az21.subscribe.R
import az21.subscribe.ui.common.ColorPicker
import az21.subscribe.ui.common.IconCatalog
import az21.subscribe.ui.common.IconEntry
import az21.subscribe.ui.common.IconSource
import az21.subscribe.ui.theme.FallbackSeedColor

private const val GRID_COLUMNS = 5
private const val GRID_HEIGHT_DP = 280

/** Searchable grid over the Simple and Material icon sets; returns the chosen icon key. */
@Composable
fun IconPickerDialog(
  currentIconId: String?,
  currentColor: Int?,
  onSelect: (String, Int?) -> Unit,
  onColorChange: (Int?) -> Unit,
  onDismiss: () -> Unit,
) {
  var query by remember { mutableStateOf("") }
  val icons = remember(query) { IconCatalog.search(query) }
  val selectedKey = IconCatalog.normalize(currentIconId.orEmpty())
  val defaultColor = IconCatalog.findEntry(currentIconId)?.defaultColor
  val selectedColor = (currentColor ?: defaultColor)?.let(::Color) ?: MaterialTheme.colorScheme.primary

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      shape = MaterialTheme.shapes.extraLarge,
      tonalElevation = 6.dp,
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(text = stringResource(R.string.form_choose_icon), style = MaterialTheme.typography.titleMedium)
        IconSearchField(query = query, onQueryChange = { query = it })
        IconGrid(
          icons = icons,
          selectedKey = selectedKey,
          selectedColor = selectedColor,
          onSelect = onSelect,
        )
        IconColorSection(
          currentColor = currentColor,
          defaultColor = defaultColor,
          onColorChange = onColorChange,
        )
        TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
          Text(stringResource(R.string.action_close))
        }
      }
    }
  }
}

@Composable
private fun IconSearchField(
  query: String,
  onQueryChange: (String) -> Unit,
) {
  OutlinedTextField(
    value = query,
    onValueChange = onQueryChange,
    modifier = Modifier.fillMaxWidth(),
    placeholder = { Text(stringResource(R.string.form_icon_search)) },
    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
    singleLine = true,
  )
}

@Composable
private fun IconGrid(
  icons: List<IconEntry>,
  selectedKey: String,
  selectedColor: Color,
  onSelect: (String, Int?) -> Unit,
) {
  LazyVerticalGrid(
    columns = GridCells.Fixed(GRID_COLUMNS),
    modifier = Modifier.fillMaxWidth().height(GRID_HEIGHT_DP.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    items(items = icons, key = { entry -> entry.key }) { entry ->
      IconButton(onClick = { onSelect(entry.key, entry.defaultColor) }) {
        Icon(
          imageVector = entry.vector,
          contentDescription = entry.contentDescription(),
          tint = if (entry.key == selectedKey) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(28.dp),
        )
      }
    }
  }
}

@Composable
private fun IconColorSection(
  currentColor: Int?,
  defaultColor: Int?,
  onColorChange: (Int?) -> Unit,
) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Text(
      text = stringResource(R.string.form_icon_color),
      style = MaterialTheme.typography.titleSmall,
      modifier = Modifier.weight(1f),
    )
    if (currentColor != defaultColor) {
      TextButton(onClick = { onColorChange(defaultColor) }) {
        Text(stringResource(R.string.form_icon_color_reset))
      }
    }
  }
  ColorPicker(
    selected = currentColor,
    fallbackColor = defaultColor ?: FallbackSeedColor,
    customContentDescription = stringResource(R.string.color_picker_custom),
    presetContentDescription = stringResource(R.string.form_icon_color),
    onColorChange = onColorChange,
  )
}

/** Disambiguates same-named icons from different sets for screen readers. */
@Composable
private fun IconEntry.contentDescription(): String {
  val source =
    stringResource(
      if (source == IconSource.SIMPLE) R.string.form_icon_source_simple else R.string.form_icon_source_material,
    )
  return stringResource(R.string.form_icon_entry_description, displayName, source)
}
