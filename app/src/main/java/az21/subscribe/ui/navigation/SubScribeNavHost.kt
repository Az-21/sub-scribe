package az21.subscribe.ui.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import az21.subscribe.ui.archive.ArchiveScreen
import az21.subscribe.ui.calendar.CalendarScreen
import az21.subscribe.ui.home.HomeScreen
import az21.subscribe.ui.metrics.MetricsScreen
import az21.subscribe.ui.paymentmethods.PaymentMethodsScreen
import az21.subscribe.ui.settings.SettingsScreen
import az21.subscribe.ui.subscription.SubscriptionDetailScreen
import az21.subscribe.ui.subscription.SubscriptionFormScreen
import az21.subscribe.ui.tags.TagsScreen

@Composable
fun SubScribeNavHost(modifier: Modifier = Modifier) {
  val backStack = rememberNavBackStack(HomeRoute)
  val activity = LocalActivity.current
  val navigateTopLevel: (NavKey) -> Unit = { route ->
    backStack.clear()
    backStack.add(route)
  }
  val popBackStack: () -> Unit = { backStack.removeLastOrNull() }
  val popOrFinish: () -> Unit = {
    if (backStack.size > 1) backStack.removeLastOrNull() else activity?.finish()
  }

  NavDisplay(
    backStack = backStack,
    modifier = modifier,
    onBack = popOrFinish,
    entryProvider =
      entryProvider {
        entry<HomeRoute> {
          HomeScreen(
            onAddSubscription = { backStack.add(SubscriptionFormRoute()) },
            onOpenSubscription = { id -> backStack.add(SubscriptionDetailRoute(id)) },
            onOpenArchive = { backStack.add(ArchiveRoute) },
            onNavigateTopLevel = navigateTopLevel,
          )
        }
        entry<CalendarRoute> { CalendarScreen(onNavigateTopLevel = navigateTopLevel) }
        entry<MetricsRoute> { MetricsScreen(onNavigateTopLevel = navigateTopLevel) }
        entry<ArchiveRoute> {
          ArchiveScreen(
            onOpenSubscription = { id -> backStack.add(SubscriptionDetailRoute(id)) },
            onNavigateTopLevel = navigateTopLevel,
          )
        }
        entry<SettingsRoute> {
          SettingsScreen(
            onOpenTags = { backStack.add(TagsRoute) },
            onOpenPaymentMethods = { backStack.add(PaymentMethodsRoute) },
            onNavigateTopLevel = navigateTopLevel,
          )
        }
        entry<SubscriptionFormRoute> { route ->
          SubscriptionFormScreen(
            subscriptionId = route.subscriptionId,
            onBack = popBackStack,
            onSaved = popBackStack,
          )
        }
        entry<SubscriptionDetailRoute> { route ->
          SubscriptionDetailScreen(
            subscriptionId = route.subscriptionId,
            onBack = popBackStack,
            onEdit = { id -> backStack.add(SubscriptionFormRoute(id)) },
          )
        }
        entry<TagsRoute> { TagsScreen(onBack = popBackStack) }
        entry<PaymentMethodsRoute> { PaymentMethodsScreen(onBack = popBackStack) }
      },
  )
}
