package az21.subscribe

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import az21.subscribe.data.reminder.ReminderDeepLink
import az21.subscribe.ui.SubScribeApp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  private val deepLinkSubscriptionId = MutableStateFlow<String?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    deepLinkSubscriptionId.value = ReminderDeepLink.subscriptionId(intent)
    val pendingSubscriptionId = deepLinkSubscriptionId.asStateFlow()
    setContent {
      SubScribeApp(
        modifier = Modifier.fillMaxSize(),
        pendingSubscriptionId = pendingSubscriptionId,
        onDeepLinkConsumed = { deepLinkSubscriptionId.value = null },
      )
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    deepLinkSubscriptionId.value = ReminderDeepLink.subscriptionId(intent)
  }
}
