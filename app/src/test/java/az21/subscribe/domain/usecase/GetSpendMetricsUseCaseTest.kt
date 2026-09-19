package az21.subscribe.domain.usecase

import az21.subscribe.data.fake.FakePriceHistoryDao
import az21.subscribe.data.fake.FakeSubscriptionDao
import az21.subscribe.data.repository.PriceHistoryRepositoryImpl
import az21.subscribe.data.repository.SubscriptionRepositoryImpl
import az21.subscribe.domain.model.BillingCycle
import az21.subscribe.domain.model.SubscriptionDraft
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class GetSpendMetricsUseCaseTest {
  private val clock = Clock.fixed(Instant.parse("2024-05-20T09:00:00Z"), ZoneOffset.UTC)
  private val subscriptionRepository = SubscriptionRepositoryImpl(FakeSubscriptionDao(), clock)
  private val priceHistoryRepository = PriceHistoryRepositoryImpl(FakePriceHistoryDao(), clock)
  private val useCase = GetSpendMetricsUseCase(subscriptionRepository, priceHistoryRepository)

  @Test
  fun combinesMonthlyAndAnnualSubscriptions() =
    runTest {
      val monthly = subscriptionRepository.createSubscription(draft(BillingCycle.MONTHLY, LocalDate.of(2024, 1, 15)))
      priceHistoryRepository.addPriceChange(monthly.id, BigDecimal("10.00"), LocalDate.of(2024, 1, 1))

      val annual = subscriptionRepository.createSubscription(draft(BillingCycle.ANNUAL, LocalDate.of(2024, 1, 15)))
      priceHistoryRepository.addPriceChange(annual.id, BigDecimal("120.00"), LocalDate.of(2024, 1, 1))

      val result = useCase(2024)

      assertEquals(BigDecimal("240.00"), result.trueAnnualSpend)
      assertEquals(BigDecimal("120.00"), result.divviedAnnualSpend)
      assertEquals(12, result.monthly.size)
    }

  @Test
  fun includesArchivedSubscriptions() =
    runTest {
      val subscription =
        subscriptionRepository.createSubscription(draft(BillingCycle.MONTHLY, LocalDate.of(2024, 1, 15)))
      priceHistoryRepository.addPriceChange(subscription.id, BigDecimal("10.00"), LocalDate.of(2024, 1, 1))
      subscriptionRepository.cancelSubscription(subscription.id, LocalDate.of(2024, 1, 31))
      subscriptionRepository.archiveSubscription(subscription.id)

      val result = useCase(2024)

      assertEquals(BigDecimal("10.00"), result.trueAnnualSpend)
    }

  @Test
  fun noSubscriptions_returnsZeroes() =
    runTest {
      val result = useCase(2024)

      assertEquals(BigDecimal.ZERO, result.trueAnnualSpend)
      assertEquals(BigDecimal.ZERO, result.divviedAnnualSpend)
    }

  @Test
  fun subscriptionWithoutPrice_contributesNothing() =
    runTest {
      subscriptionRepository.createSubscription(draft(BillingCycle.MONTHLY, LocalDate.of(2024, 1, 15)))

      val result = useCase(2024)

      assertEquals(BigDecimal.ZERO, result.trueAnnualSpend)
    }

  private fun draft(
    billingCycle: BillingCycle,
    startDate: LocalDate,
  ): SubscriptionDraft =
    SubscriptionDraft(
      name = "Test",
      iconId = "test",
      startDate = startDate,
      billingCycle = billingCycle,
    )
}
