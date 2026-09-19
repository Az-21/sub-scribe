package az21.subscribe.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Detail destination for a single subscription. */
@Serializable
data class SubscriptionDetailRoute(
  val subscriptionId: String,
) : NavKey
