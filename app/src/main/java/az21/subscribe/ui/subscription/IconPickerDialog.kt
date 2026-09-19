package az21.subscribe.ui.subscription

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import az21.subscribe.R
import az21.subscribe.ui.common.SimpleIconsCatalog

private const val GRID_COLUMNS = 5

/** Searchable grid over the embedded Simple Icons set; returns the chosen icon key. */
@Composable
fun IconPickerDialog(
  currentIconId: String?,
  onSelect: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  var query by remember { mutableStateOf("") }
  val icons = remember(query) { derivedIcons(query) }

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
        OutlinedTextField(
          value = query,
          onValueChange = { query = it },
          modifier = Modifier.fillMaxWidth(),
          placeholder = { Text(stringResource(R.string.form_icon_search)) },
          leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
          singleLine = true,
        )
        LazyVerticalGrid(
          columns = GridCells.Fixed(GRID_COLUMNS),
          modifier = Modifier.fillMaxWidth().height(360.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          items(items = icons, key = { entry -> entry.key }) { entry ->
            IconButton(onClick = { onSelect(entry.key) }) {
              Icon(
                imageVector = entry.vector,
                contentDescription = entry.displayName,
                tint =
                  if (entry.key == SimpleIconsCatalog.toIconKey(currentIconId.orEmpty())) {
                    MaterialTheme.colorScheme.primary
                  } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                  },
                modifier = Modifier.size(28.dp),
              )
            }
          }
        }
        TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
          Text(stringResource(R.string.action_close))
        }
      }
    }
  }
}

private fun derivedIcons(query: String): List<SimpleIconsCatalog.IconEntry> {
  if (query.isBlank()) return SimpleIconsCatalog.all
  val normalized = SimpleIconsCatalog.toIconKey(query)
  return SimpleIconsCatalog.all.filter { entry ->
    entry.key.contains(normalized) || entry.displayName.contains(query.trim(), ignoreCase = true)
  }
}
