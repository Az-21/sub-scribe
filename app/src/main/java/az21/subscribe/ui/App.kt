package az21.subscribe.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az21.subscribe.ui.navigation.SubScribeNavHost
import az21.subscribe.ui.theme.AppTheme

@Composable
fun SubScribeApp(
  modifier: Modifier = Modifier,
  viewModel: AppViewModel = hiltViewModel(),
) {
  val settings by viewModel.settings.collectAsStateWithLifecycle()
  AppTheme(settings = settings) {
    SubScribeNavHost(modifier = modifier)
  }
}
