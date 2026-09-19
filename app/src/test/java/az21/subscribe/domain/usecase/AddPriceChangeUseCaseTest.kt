package az21.subscribe.domain.usecase

import az21.subscribe.data.fake.FakePriceHistoryDao
import az21.subscribe.data.fake.FakeSubscriptionDao
import az21.subscribe.data.repository.PriceHistoryRepositoryImpl
import az21.subscribe.data.repository.SubscriptionRepositoryImpl
import az21.subscribe.domain.model.BillingCycle
import az21.subscribe.domain.model.SubscriptionDraft
import az21.subscribe.domain.model.SubscriptionTransitionException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

class AddPriceChangeUseCaseTest {
  private val clock = Clock.fixed(Instant.parse("2024-05-20T09:00:00Z"), ZoneOffset.UTC)
  private val subscriptionRepository = SubscriptionRepositoryImpl(FakeSubscriptionDao(), clock)
  private val priceHistoryRepository = PriceHistoryRepositoryImpl(FakePriceHistoryDao(), clock)
  private val useCase = AddPriceChangeUseCase(subscriptionRepository, priceHistoryRepository)

  @Test
  fun addsBackdatedEntry() =
    runTest {
      val subscription = subscriptionRepository.createSubscription(draft())

      val entry = useCase(subscription.id, BigDecimal("9.99"), LocalDate.of(2023, 1, 1))

      assertEquals(BigDecimal("9.99"), entry.price)
      assertEquals(LocalDate.of(2023, 1, 1), entry.effectiveFromDate)
    }

  @Test
  fun addsFutureDatedEntry() =
    runTest {
      val subscription = subscriptionRepository.createSubscription(draft())

      val entry = useCase(subscription.id, BigDecimal("12.99"), LocalDate.of(2025, 1, 1))

      assertEquals(LocalDate.of(2025, 1, 1), entry.effectiveFromDate)
    }

  @Test
  fun rejectsNegativePrice() =
    runTest {
      val subscription = subscriptionRepository.createSubscription(draft())

      val error =
        runCatching { useCase(subscription.id, BigDecimal("-0.01"), LocalDate.of(2024, 1, 1)) }
          .exceptionOrNull()

      assertTrue(error is IllegalArgumentException)
    }

  @Test
  fun rejectsUnknownSubscription() =
    runTest {
      val error =
        runCatching { useCase(UUID.randomUUID(), BigDecimal("9.99"), LocalDate.of(2024, 1, 1)) }
          .exceptionOrNull()

      assertTrue(error is SubscriptionTransitionException.NotFound)
    }

  private fun draft(): SubscriptionDraft =
    SubscriptionDraft(
      name = "Netflix",
      iconId = "netflix",
      billingCycle = BillingCycle.MONTHLY,
    )
}
