package az21.subscribe.data.reminder

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import az21.subscribe.domain.billing.BillingSchedule
import az21.subscribe.domain.model.Subscription
import az21.subscribe.domain.model.SubscriptionStatus
import az21.subscribe.domain.reminder.ReminderScheduler
import az21.subscribe.domain.reminder.ReminderType
import az21.subscribe.domain.repository.SubscriptionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

/** Shows a due reminder and chains the subscription's next one. */
@HiltWorker
class ReminderWorker
  @AssistedInject
  constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val subscriptionRepository: SubscriptionRepository,
    private val notifier: ReminderNotifier,
    private val scheduler: ReminderScheduler,
    private val clock: Clock,
  ) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
      val input = parseInput()
      val subscription = input?.let { subscriptionRepository.getSubscription(it.subscriptionId) }
      if (input != null && subscription != null && subscription.status == SubscriptionStatus.ACTIVE) {
        if (isStillDue(subscription, input.type, input.targetDate)) {
          notifier.show(subscription, input.type, input.targetDate)
        }
        scheduler.enqueueNext(subscription)
      }
      return Result.success()
    }

    private fun isStillDue(
      subscription: Subscription,
      type: ReminderType,
      targetDate: LocalDate,
    ): Boolean =
      when (type) {
        ReminderType.BILLING -> {
          subscription.reminderDaysBefore != null &&
            BillingSchedule.nextBillingDate(subscription, LocalDate.now(clock)) == targetDate
        }

        ReminderType.TRIAL_ENDING -> {
          subscription.trialReminderEnabled && BillingSchedule.billingStartDate(subscription) == targetDate
        }
      }

    private fun parseInput(): ReminderInput? =
      inputData.getString(KEY_SUBSCRIPTION_ID)?.let(::parseUuid)?.let { subscriptionId ->
        inputData.getString(KEY_TYPE)?.let(::parseType)?.let { type ->
          inputData.getString(KEY_TARGET_DATE)?.let(::parseDate)?.let { targetDate ->
            ReminderInput(subscriptionId, type, targetDate)
          }
        }
      }

    private fun parseUuid(value: String): UUID? = runCatching { UUID.fromString(value) }.getOrNull()

    private fun parseType(value: String): ReminderType? = ReminderType.entries.firstOrNull { it.name == value }

    private fun parseDate(value: String): LocalDate? = runCatching { LocalDate.parse(value) }.getOrNull()

    private data class ReminderInput(
      val subscriptionId: UUID,
      val type: ReminderType,
      val targetDate: LocalDate,
    )

    companion object {
      const val KEY_SUBSCRIPTION_ID: String = "subscription_id"
      const val KEY_TYPE: String = "reminder_type"
      const val KEY_TARGET_DATE: String = "target_date"
    }
  }
