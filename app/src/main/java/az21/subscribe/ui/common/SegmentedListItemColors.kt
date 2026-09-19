package az21.subscribe.ui.common

import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Container color for segmented list rows. The default segmented token resolves to `surface`, which
 * matches the screen background, so rows would blend in. `surfaceContainer` keeps them legible and
 * matches the tone used by the connected option buttons.
 */
@Composable
fun segmentedListItemColors(): ListItemColors =
  ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
