package az21.subscribe.domain.usecase

import az21.subscribe.domain.model.BillingCycle
import az21.subscribe.domain.model.Subscription
import az21.subscribe.domain.model.SubscriptionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

class GetNextBillingDateUseCaseTest {
  private val clock = Clock.fixed(Instant.parse("2024-05-20T09:00:00Z"), ZoneOffset.UTC)
  private val useCase = GetNextBillingDateUseCase(clock)

  @Test
  fun defaultsFromToToday() {
    val subscription = subscription(startDate = LocalDate.of(2024, 1, 15))

    assertEquals(LocalDate.of(2024, 6, 15), useCase(subscription))
  }

  @Test
  fun honoursExplicitFromDate() {
    val subscription = subscription(startDate = LocalDate.of(2024, 1, 15))

    assertEquals(LocalDate.of(2024, 2, 15), useCase(subscription, LocalDate.of(2024, 2, 1)))
  }

  @Test
  fun accountsForFreeTrial() {
    val subscription =
      subscription(startDate = LocalDate.of(2024, 1, 15), freeTrialMonths = 2)

    assertEquals(LocalDate.of(2024, 3, 15), useCase(subscription, LocalDate.of(2024, 1, 1)))
  }

  @Test
  fun returnsNullWhenCancelled() {
    assertNull(useCase(subscription(status = SubscriptionStatus.CANCELLED)))
  }

  @Test
  fun returnsNullWhenArchived() {
    assertNull(useCase(subscription(status = SubscriptionStatus.ARCHIVED)))
  }

  private fun subscription(
    startDate: LocalDate = LocalDate.of(2024, 1, 15),
    freeTrialMonths: Int? = null,
    status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
  ): Subscription =
    Subscription(
      id = UUID.randomUUID(),
      name = "Test",
      iconId = "test",
      startDate = startDate,
      billingCycle = BillingCycle.MONTHLY,
      freeTrialMonths = freeTrialMonths,
      status = status,
      endDate = null,
      reminderDaysBefore = null,
      trialReminderEnabled = false,
      paymentMethodId = null,
      notes = null,
      createdAt = Instant.EPOCH,
      updatedAt = Instant.EPOCH,
    )
}
