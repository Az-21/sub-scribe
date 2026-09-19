package az21.subscribe.domain.usecase

import az21.subscribe.domain.reminder.ReminderScheduler
import java.util.UUID
import javax.inject.Inject

/** Cancels every scheduled reminder for a subscription. */
class CancelReminderUseCase
  @Inject
  constructor(
    private val scheduler: ReminderScheduler,
  ) {
    suspend operator fun invoke(subscriptionId: UUID) = scheduler.cancel(subscriptionId)
  }
