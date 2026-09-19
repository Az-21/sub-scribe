package az21.subscribe.domain.usecase

import az21.subscribe.data.fake.FakeReminderScheduler
import az21.subscribe.data.fake.FakeSubscriptionDao
import az21.subscribe.data.repository.SubscriptionRepositoryImpl
import az21.subscribe.domain.model.BillingCycle
import az21.subscribe.domain.model.SubscriptionDraft
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

class ReminderUseCasesTest {
  private val clock = Clock.fixed(Instant.parse("2024-05-20T09:00:00Z"), ZoneOffset.UTC)
  private val repository = SubscriptionRepositoryImpl(FakeSubscriptionDao(), clock)
  private val scheduler = FakeReminderScheduler()

  @Test
  fun schedule_byId_schedulesStoredSubscription() =
    runTest {
      val subscription = create()

      ScheduleReminderUseCase(repository, scheduler)(subscription.id)

      assertEquals(listOf(subscription.id), scheduler.scheduled.map { it.id })
    }

  @Test
  fun schedule_unknownId_doesNothing() =
    runTest {
      ScheduleReminderUseCase(repository, scheduler)(UUID.randomUUID())

      assertTrue(scheduler.scheduled.isEmpty())
    }

  @Test
  fun schedule_subscription_schedulesDirectly() =
    runTest {
      ScheduleReminderUseCase(repository, scheduler)(create())

      assertEquals(1, scheduler.scheduled.size)
    }

  @Test
  fun cancel_cancelsSubscriptionReminders() =
    runTest {
      val id = UUID.randomUUID()

      CancelReminderUseCase(scheduler)(id)

      assertEquals(listOf(id), scheduler.cancelled)
    }

  @Test
  fun rescheduleAll_onlySchedulesActiveSubscriptions() =
    runTest {
      val active = create("Netflix")
      val cancelled = create("Spotify")
      repository.cancelSubscription(cancelled.id)

      RescheduleAllRemindersUseCase(repository, scheduler)()

      assertEquals(listOf(active.id), scheduler.scheduled.map { it.id })
    }

  private suspend fun create(name: String = "Netflix") =
    repository.createSubscription(
      SubscriptionDraft(
        name = name,
        iconId = "netflix",
        startDate = LocalDate.of(2024, 1, 1),
        billingCycle = BillingCycle.MONTHLY,
      ),
    )
}
