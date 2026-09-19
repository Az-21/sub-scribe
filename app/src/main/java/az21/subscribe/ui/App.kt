package az21.subscribe.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import az21.subscribe.ui.navigation.SubScribeNavHost
import az21.subscribe.ui.theme.SubScribeTheme

@Composable
fun SubScribeApp(modifier: Modifier = Modifier) {
  SubScribeTheme {
    SubScribeNavHost(modifier = modifier)
  }
}
