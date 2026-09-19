package az21.subscribe.ui.metrics

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import az21.subscribe.MainDispatcherRule
import az21.subscribe.data.fake.FakePriceHistoryDao
import az21.subscribe.data.fake.FakeSettingsRepository
import az21.subscribe.data.fake.FakeSubscriptionDao
import az21.subscribe.data.repository.PriceHistoryRepositoryImpl
import az21.subscribe.data.repository.SubscriptionRepositoryImpl
import az21.subscribe.domain.model.BillingCycle
import az21.subscribe.domain.model.Currency
import az21.subscribe.domain.model.SubscriptionDraft
import az21.subscribe.domain.usecase.GetSpendMetricsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class MetricsViewModelTest {
  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  private val clock = Clock.fixed(Instant.parse("2024-05-20T09:00:00Z"), ZoneOffset.UTC)
  private val subscriptionRepository = SubscriptionRepositoryImpl(FakeSubscriptionDao(), clock)
  private val priceHistoryRepository = PriceHistoryRepositoryImpl(FakePriceHistoryDao(), clock)
  private val settingsRepository = FakeSettingsRepository()
  private val viewModel =
    MetricsViewModel(
      GetSpendMetricsUseCase(subscriptionRepository, priceHistoryRepository),
      settingsRepository,
      clock,
    )

  @Test
  fun startsOnCurrentYearAndMonth_whileLoading() =
    runTest {
      viewModel.uiState.test {
        val initial = awaitItem()

        assertTrue(initial.isLoading)
        assertEquals(2024, initial.year)
        assertEquals(Month.MAY, initial.selectedMonth)
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun emitsTotalsForTheYear() =
    runTest {
      seed(monthly = "10.00", annual = "120.00")

      viewModel.uiState.test {
        val state = awaitLoaded()

        assertEquals(2024, state.year)
        assertEquals(BigDecimal("240.00"), state.totalSpend)
        assertEquals(BigDecimal("120.00"), state.divviedAnnualSpend)
        assertEquals(12, state.monthly.size)
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun selectingMonth_revealsItsChargeAndDivviedSpend() =
    runTest {
      seed(monthly = "10.00")

      viewModel.uiState.test {
        awaitLoaded()
        viewModel.onMonthSelected(Month.JULY)

        val state = awaitItem()

        assertEquals(Month.JULY, state.selectedMonth)
        assertEquals(BigDecimal("10.00"), state.selectedMonthSpend?.trueMonthlySpend)
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun navigatingToPreviousYear_recomputesForThatYear() =
    runTest {
      seed(monthly = "10.00")

      viewModel.uiState.test {
        awaitLoaded()
        viewModel.onPreviousYear()

        val state = awaitLoaded()

        assertEquals(2023, state.year)
        assertEquals(BigDecimal.ZERO, state.totalSpend)
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun exposesSelectedCurrency() =
    runTest {
      settingsRepository.setCurrency(Currency.EUR)

      viewModel.uiState.test {
        val state = awaitLoaded()

        assertEquals(Currency.EUR, state.currency)
        cancelAndIgnoreRemainingEvents()
      }
    }

  private suspend fun ReceiveTurbine<MetricsUiState>.awaitLoaded(): MetricsUiState {
    var state = awaitItem()
    while (state.isLoading) {
      state = awaitItem()
    }
    return state
  }

  private suspend fun seed(
    monthly: String? = null,
    annual: String? = null,
  ) {
    monthly?.let {
      val subscription = subscriptionRepository.createSubscription(draft(BillingCycle.MONTHLY))
      priceHistoryRepository.addPriceChange(subscription.id, BigDecimal(it), LocalDate.of(2024, 1, 1))
    }
    annual?.let {
      val subscription = subscriptionRepository.createSubscription(draft(BillingCycle.ANNUAL))
      priceHistoryRepository.addPriceChange(subscription.id, BigDecimal(it), LocalDate.of(2024, 1, 1))
    }
  }

  private fun draft(billingCycle: BillingCycle): SubscriptionDraft =
    SubscriptionDraft(
      name = "Test",
      iconId = "test",
      startDate = LocalDate.of(2024, 1, 15),
      billingCycle = billingCycle,
    )
}
