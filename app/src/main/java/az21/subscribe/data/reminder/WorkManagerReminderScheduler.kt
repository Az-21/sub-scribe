package az21.subscribe.data.reminder

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import az21.subscribe.domain.model.Subscription
import az21.subscribe.domain.reminder.ReminderPlan
import az21.subscribe.domain.reminder.ReminderPlanner
import az21.subscribe.domain.reminder.ReminderScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WorkManager-backed reminder scheduling.
 *
 * Each plan gets a unique work name that includes its target date, so chaining the next occurrence
 * never collides with the running one. All work shares a per-subscription tag so a settings change
 * can cancel and replace the whole set.
 */
@Singleton
class WorkManagerReminderScheduler
  @Inject
  constructor(
    @ApplicationContext context: Context,
    private val clock: Clock,
  ) : ReminderScheduler {
    private val workManager = WorkManager.getInstance(context)

    override suspend fun schedule(subscription: Subscription) {
      cancel(subscription.id)
      enqueueNext(subscription)
    }

    override suspend fun enqueueNext(subscription: Subscription) {
      val now = clock.instant()
      ReminderPlanner.plans(subscription, now, clock.zone).forEach { plan ->
        workManager.enqueueUniqueWork(
          uniqueWorkName(subscription.id, plan),
          ExistingWorkPolicy.KEEP,
          request(subscription.id, plan, now),
        )
      }
    }

    override suspend fun cancel(subscriptionId: UUID) {
      workManager.cancelAllWorkByTag(tag(subscriptionId))
    }

    private fun request(
      subscriptionId: UUID,
      plan: ReminderPlan,
      now: Instant,
    ): OneTimeWorkRequest =
      OneTimeWorkRequestBuilder<ReminderWorker>()
        .setInitialDelay(Duration.between(now, plan.triggerAt))
        .setInputData(
          workDataOf(
            ReminderWorker.KEY_SUBSCRIPTION_ID to subscriptionId.toString(),
            ReminderWorker.KEY_TYPE to plan.type.name,
            ReminderWorker.KEY_TARGET_DATE to plan.targetDate.toString(),
          ),
        ).addTag(tag(subscriptionId))
        .build()

    private fun uniqueWorkName(
      subscriptionId: UUID,
      plan: ReminderPlan,
    ): String = "reminder-${plan.type.name.lowercase()}-$subscriptionId-${plan.targetDate}"

    private fun tag(subscriptionId: UUID): String = "reminder-subscription-$subscriptionId"
  }
