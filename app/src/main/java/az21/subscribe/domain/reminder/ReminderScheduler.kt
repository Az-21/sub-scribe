package az21.subscribe.domain.reminder

import az21.subscribe.domain.model.Subscription
import java.util.UUID

/**
 * Schedules and cancels local reminder notifications.
 *
 * Implemented with WorkManager, which persists pending work across device reboots, so no boot
 * receiver is needed.
 */
interface ReminderScheduler {
  /** Cancels any existing reminders for the subscription and schedules its current reminders. */
  suspend fun schedule(subscription: Subscription)

  /**
   * Schedules the subscription's current reminders without cancelling existing work. Used by the
   * reminder worker to chain the next occurrence after one fires.
   */
  suspend fun enqueueNext(subscription: Subscription)

  /** Cancels all pending reminders for the subscription. */
  suspend fun cancel(subscriptionId: UUID)
}
