package az21.subscribe.data.fake

import az21.subscribe.domain.model.Subscription
import az21.subscribe.domain.reminder.ReminderScheduler
import java.util.UUID

/** Records reminder scheduling calls so use-case and ViewModel tests can assert on them. */
class FakeReminderScheduler : ReminderScheduler {
  val scheduled = mutableListOf<Subscription>()
  val enqueued = mutableListOf<Subscription>()
  val cancelled = mutableListOf<UUID>()

  override suspend fun schedule(subscription: Subscription) {
    scheduled += subscription
  }

  override suspend fun enqueueNext(subscription: Subscription) {
    enqueued += subscription
  }

  override suspend fun cancel(subscriptionId: UUID) {
    cancelled += subscriptionId
  }
}
