package az21.subscribe.ui.common

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import az21.subscribe.R

/**
 * Shared Material 3 top app bar. Uses the medium two-row variant so the heading sits on its own
 * row, with the back navigation and a tonal primary action on the row above. Screens pass any
 * additional icon actions through [actions].
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SubScribeTopAppBar(
  title: String,
  modifier: Modifier = Modifier,
  onNavigateUp: (() -> Unit)? = null,
  scrollBehavior: TopAppBarScrollBehavior? = null,
  primaryAction: PrimaryAppBarAction? = null,
  actions: @Composable RowScope.() -> Unit = {},
) {
  MediumFlexibleTopAppBar(
    title = { Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
    modifier = modifier,
    navigationIcon = {
      if (onNavigateUp != null) {
        IconButton(onClick = onNavigateUp) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.action_back),
          )
        }
      }
    },
    actions = {
      if (primaryAction != null) {
        val buttonHeight = ButtonDefaults.MinHeight
        FilledTonalButton(
          onClick = primaryAction.onClick,
          contentPadding = ButtonDefaults.contentPaddingFor(buttonHeight, hasStartIcon = true),
        ) {
          Icon(
            imageVector = primaryAction.icon,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.iconSizeFor(buttonHeight)),
          )
          Spacer(modifier = Modifier.size(ButtonDefaults.iconSpacingFor(buttonHeight)))
          Text(text = primaryAction.label, maxLines = 1)
        }
      }
      actions()
    },
    scrollBehavior = scrollBehavior,
  )
}
