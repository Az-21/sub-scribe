package az21.subscribe.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az21.subscribe.ui.navigation.SubScribeNavHost
import az21.subscribe.ui.theme.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun SubScribeApp(
  modifier: Modifier = Modifier,
  pendingSubscriptionId: StateFlow<String?> = MutableStateFlow<String?>(null),
  onDeepLinkConsumed: () -> Unit = {},
  viewModel: AppViewModel = hiltViewModel(),
) {
  val settings by viewModel.settings.collectAsStateWithLifecycle()
  val deepLinkSubscriptionId by pendingSubscriptionId.collectAsStateWithLifecycle()
  AppTheme(settings = settings) {
    SubScribeNavHost(
      modifier = modifier,
      deepLinkSubscriptionId = deepLinkSubscriptionId,
      onDeepLinkConsumed = onDeepLinkConsumed,
    )
  }
}
