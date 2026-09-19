package az21.subscribe.data.reminder

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import az21.subscribe.domain.billing.BillingSchedule
import az21.subscribe.domain.model.Subscription
import az21.subscribe.domain.model.SubscriptionStatus
import az21.subscribe.domain.reminder.ReminderScheduler
import az21.subscribe.domain.repository.SubscriptionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
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
        if (isStillDue(subscription, input.targetDate)) {
          notifier.show(subscription, input.targetDate, input.daysBefore, input.time)
        }
        scheduler.enqueueNext(subscription)
      }
      return Result.success()
    }

    private fun isStillDue(
      subscription: Subscription,
      targetDate: LocalDate,
    ): Boolean = BillingSchedule.nextBillingDate(subscription, LocalDate.now(clock)) == targetDate

    private fun parseInput(): ReminderInput? {
      val subscriptionId = inputData.getString(KEY_SUBSCRIPTION_ID)?.let(::parseUuid)
      val targetDate = inputData.getString(KEY_TARGET_DATE)?.let(::parseDate)
      val time = inputData.getString(KEY_TIME)?.let(::parseTime)
      return if (subscriptionId == null || targetDate == null || time == null) {
        null
      } else {
        ReminderInput(
          subscriptionId = subscriptionId,
          targetDate = targetDate,
          daysBefore = inputData.getInt(KEY_DAYS_BEFORE, 0),
          time = time,
        )
      }
    }

    private fun parseUuid(value: String): UUID? = runCatching { UUID.fromString(value) }.getOrNull()

    private fun parseDate(value: String): LocalDate? = runCatching { LocalDate.parse(value) }.getOrNull()

    private fun parseTime(value: String): LocalTime? = runCatching { LocalTime.parse(value) }.getOrNull()

    private data class ReminderInput(
      val subscriptionId: UUID,
      val targetDate: LocalDate,
      val daysBefore: Int,
      val time: LocalTime,
    )

    companion object {
      const val KEY_SUBSCRIPTION_ID: String = "subscription_id"
      const val KEY_TARGET_DATE: String = "target_date"
      const val KEY_DAYS_BEFORE: String = "reminder_days_before"
      const val KEY_TIME: String = "reminder_time"
    }
  }
