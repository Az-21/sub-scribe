package az21.subscribe.domain.usecase

import az21.subscribe.domain.model.Subscription
import az21.subscribe.domain.reminder.ReminderScheduler
import az21.subscribe.domain.repository.SubscriptionRepository
import java.util.UUID
import javax.inject.Inject

/**
 * Recomputes a subscription's billing reminders and schedules them, replacing anything previously
 * queued for it. Call whenever dates, cycle, or reminder settings change.
 */
class ScheduleReminderUseCase
  @Inject
  constructor(
    private val repository: SubscriptionRepository,
    private val scheduler: ReminderScheduler,
  ) {
    suspend operator fun invoke(subscription: Subscription) = scheduler.schedule(subscription)

    suspend operator fun invoke(subscriptionId: UUID) {
      repository.getSubscription(subscriptionId)?.let { scheduler.schedule(it) }
    }
  }
