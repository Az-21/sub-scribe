package az21.subscribe.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import az21.subscribe.ui.home.HomeScreen
import az21.subscribe.ui.metrics.MetricsScreen

@Composable
fun SubScribeNavHost(modifier: Modifier = Modifier) {
  val backStack = rememberNavBackStack(HomeRoute)

  NavDisplay(
    backStack = backStack,
    modifier = modifier,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<HomeRoute> { HomeScreen(onOpenMetrics = { backStack.add(MetricsRoute) }) }
        entry<MetricsRoute> { MetricsScreen(onBack = { backStack.removeLastOrNull() }) }
      },
  )
}
