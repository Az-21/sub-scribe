package az21.subscribe.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import az21.subscribe.R

/**
 * Shared Material 3 Expressive floating toolbar that replaces the bottom navigation bar. It hosts
 * the top-level destinations as icon actions plus a single floating action button. The FAB creates
 * a new subscription while the home destination is selected and otherwise returns to home.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SubScribeFloatingToolbar(
  selectedDestination: TopLevelDestination?,
  onNavigateTopLevel: (NavKey) -> Unit,
  modifier: Modifier = Modifier,
  onCreateSubscription: (() -> Unit)? = null,
) {
  val createSubscription =
    if (selectedDestination == TopLevelDestination.HOME) onCreateSubscription else null
  HorizontalFloatingToolbar(
    expanded = true,
    modifier = modifier,
    floatingActionButton = {
      if (createSubscription != null) {
        FloatingToolbarDefaults.VibrantFloatingActionButton(onClick = createSubscription) {
          Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.home_add))
        }
      } else {
        FloatingToolbarDefaults.StandardFloatingActionButton(
          onClick = { onNavigateTopLevel(HomeRoute) },
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.action_back),
          )
        }
      }
    },
  ) {
    TopLevelDestination.entries.forEach { destination ->
      val label = stringResource(destination.labelRes)
      if (destination == selectedDestination) {
        FilledIconButton(onClick = { onNavigateTopLevel(destination.route) }) {
          Icon(imageVector = destination.icon, contentDescription = label)
        }
      } else {
        IconButton(onClick = { onNavigateTopLevel(destination.route) }) {
          Icon(imageVector = destination.icon, contentDescription = label)
        }
      }
    }
  }
}
