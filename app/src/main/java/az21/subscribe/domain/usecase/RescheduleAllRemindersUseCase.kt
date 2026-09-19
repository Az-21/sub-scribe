package az21.subscribe.domain.usecase

import az21.subscribe.domain.model.SubscriptionStatus
import az21.subscribe.domain.reminder.ReminderScheduler
import az21.subscribe.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Reconciles every active subscription's reminders on app start. This is a safety net for pending
 * work lost before this feature existed and keeps schedules correct without user interaction.
 */
class RescheduleAllRemindersUseCase
  @Inject
  constructor(
    private val repository: SubscriptionRepository,
    private val scheduler: ReminderScheduler,
  ) {
    suspend operator fun invoke() {
      repository
        .observeSubscriptions()
        .first()
        .filter { it.status == SubscriptionStatus.ACTIVE }
        .forEach { scheduler.schedule(it) }
    }
  }
