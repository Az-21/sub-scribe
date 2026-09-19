package az21.subscribe.ui.calendar

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import az21.subscribe.MainDispatcherRule
import az21.subscribe.data.fake.FakePriceHistoryDao
import az21.subscribe.data.fake.FakeSettingsRepository
import az21.subscribe.data.fake.FakeSubscriptionDao
import az21.subscribe.data.repository.PriceHistoryRepositoryImpl
import az21.subscribe.data.repository.SubscriptionRepositoryImpl
import az21.subscribe.domain.model.BillingCycle
import az21.subscribe.domain.model.Subscription
import az21.subscribe.domain.model.SubscriptionDraft
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {
  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  private val clock = Clock.fixed(Instant.parse("2024-05-20T09:00:00Z"), ZoneOffset.UTC)
  private val subscriptionRepository = SubscriptionRepositoryImpl(FakeSubscriptionDao(), clock)
  private val priceHistoryRepository = PriceHistoryRepositoryImpl(FakePriceHistoryDao(), clock)
  private val settingsRepository = FakeSettingsRepository()

  private fun createViewModel() =
    CalendarViewModel(
      subscriptionRepository = subscriptionRepository,
      priceHistoryRepository = priceHistoryRepository,
      settingsRepository = settingsRepository,
      clock = clock,
    )

  @Test
  fun laysOutTheVisibleMonthAndResolvesCharges() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()
      val subscription = create()
      priceHistoryRepository.addPriceChange(subscription.id, BigDecimal("9.99"), LocalDate.of(2024, 1, 1))

      viewModel.uiState.test {
        val state = awaitLoaded()

        assertEquals(2024, state.year)
        assertEquals(31, state.days.size)
        assertEquals(2, state.firstDayOffset)
        val day = state.days.first { it.date.dayOfMonth == 15 }
        assertEquals(BigDecimal("9.99"), day.charges.single().price)
        assertEquals(BigDecimal("9.99"), state.monthTotal)
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun navigatingToNextMonth_showsItsCharges() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()
      val subscription = create()
      priceHistoryRepository.addPriceChange(subscription.id, BigDecimal("9.99"), LocalDate.of(2024, 1, 1))

      viewModel.uiState.test {
        awaitLoaded()
        viewModel.onNextMonth()

        val state = awaitState { it.month.value == 6 && it.selectedDate == LocalDate.of(2024, 6, 1) }
        assertEquals(6, state.month.value)
        assertEquals(LocalDate.of(2024, 6, 15), state.days.first { it.charges.isNotEmpty() }.date)
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun selectingADay_revealsItsCharges() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()
      val subscription = create()
      priceHistoryRepository.addPriceChange(subscription.id, BigDecimal("9.99"), LocalDate.of(2024, 1, 1))

      viewModel.uiState.test {
        awaitLoaded()
        viewModel.onDaySelected(LocalDate.of(2024, 5, 15))

        val state = awaitLoaded()
        assertEquals(1, state.selectedCharges.size)
        assertEquals(subscription.id, state.selectedCharges.single().subscriptionId)
        cancelAndIgnoreRemainingEvents()
      }
    }

  private suspend fun ReceiveTurbine<CalendarUiState>.awaitLoaded(): CalendarUiState =
    awaitState { state -> !state.isLoading }

  private suspend fun ReceiveTurbine<CalendarUiState>.awaitState(
    predicate: (CalendarUiState) -> Boolean,
  ): CalendarUiState {
    var state = awaitItem()
    while (!predicate(state)) {
      state = awaitItem()
    }
    return state
  }

  private suspend fun create(): Subscription =
    subscriptionRepository.createSubscription(
      SubscriptionDraft(
        name = "Netflix",
        iconId = "netflix",
        startDate = LocalDate.of(2024, 1, 15),
        billingCycle = BillingCycle.MONTHLY,
      ),
    )
}
