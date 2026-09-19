package az21.subscribe.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey

/** Shared bottom navigation across the top-level destinations. */
@Composable
fun SubScribeBottomBar(
  currentRoute: NavKey,
  onSelect: (NavKey) -> Unit,
  modifier: Modifier = Modifier,
) {
  NavigationBar(modifier = modifier) {
    TopLevelDestination.entries.forEach { destination ->
      NavigationBarItem(
        selected = currentRoute == destination.route,
        onClick = { onSelect(destination.route) },
        icon = { Icon(imageVector = destination.icon, contentDescription = null) },
        label = { Text(stringResource(destination.labelRes)) },
      )
    }
  }
}
