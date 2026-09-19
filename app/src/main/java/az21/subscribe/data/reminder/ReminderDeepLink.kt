package az21.subscribe.data.reminder

import android.content.Intent

/** Intent extra that opens a subscription's detail screen when a reminder notification is tapped. */
object ReminderDeepLink {
  const val EXTRA_SUBSCRIPTION_ID: String = "az21.subscribe.extra.SUBSCRIPTION_ID"

  fun subscriptionId(intent: Intent?): String? = intent?.getStringExtra(EXTRA_SUBSCRIPTION_ID)
}
