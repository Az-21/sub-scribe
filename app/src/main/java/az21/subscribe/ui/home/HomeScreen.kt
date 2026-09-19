package az21.subscribe.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import az21.subscribe.R
import az21.subscribe.ui.theme.SubScribeTheme

/**
 * Placeholder home screen. The full subscription list arrives in the core screens phase.
 */
@Composable
fun HomeScreen(
  onOpenMetrics: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.headlineMedium,
      )
      Button(onClick = onOpenMetrics) {
        Text(stringResource(R.string.home_open_metrics))
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
  SubScribeTheme {
    HomeScreen(onOpenMetrics = {})
  }
}
