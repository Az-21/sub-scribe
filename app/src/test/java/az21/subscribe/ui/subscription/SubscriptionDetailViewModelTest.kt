package az21.subscribe.ui.subscription

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import az21.subscribe.MainDispatcherRule
import az21.subscribe.data.fake.FakePaymentMethodDao
import az21.subscribe.data.fake.FakePriceHistoryDao
import az21.subscribe.data.fake.FakeSettingsRepository
import az21.subscribe.data.fake.FakeSubscriptionDao
import az21.subscribe.data.fake.FakeTagDao
import az21.subscribe.data.repository.PaymentMethodRepositoryImpl
import az21.subscribe.data.repository.PriceHistoryRepositoryImpl
import az21.subscribe.data.repository.SubscriptionRepositoryImpl
import az21.subscribe.data.repository.TagRepositoryImpl
import az21.subscribe.domain.model.BillingCycle
import az21.subscribe.domain.model.Subscription
import az21.subscribe.domain.model.SubscriptionDraft
import az21.subscribe.domain.model.SubscriptionStatus
import az21.subscribe.domain.usecase.AddPriceChangeUseCase
import az21.subscribe.domain.usecase.ArchiveSubscriptionUseCase
import az21.subscribe.domain.usecase.CancelSubscriptionUseCase
import az21.subscribe.domain.usecase.DeleteSubscriptionUseCase
import az21.subscribe.domain.usecase.GetNextBillingDateUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionDetailViewModelTest {
  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  private val clock = Clock.fixed(Instant.parse("2024-05-20T09:00:00Z"), ZoneOffset.UTC)
  private val subscriptionRepository = SubscriptionRepositoryImpl(FakeSubscriptionDao(), clock)
  private val priceHistoryRepository = PriceHistoryRepositoryImpl(FakePriceHistoryDao(), clock)
  private val tagRepository = TagRepositoryImpl(FakeTagDao())
  private val paymentMethodRepository = PaymentMethodRepositoryImpl(FakePaymentMethodDao())
  private val settingsRepository = FakeSettingsRepository()

  private fun createViewModel() =
    SubscriptionDetailViewModel(
      subscriptionRepository = subscriptionRepository,
      priceHistoryRepository = priceHistoryRepository,
      tagRepository = tagRepository,
      paymentMethodRepository = paymentMethodRepository,
      settingsRepository = settingsRepository,
      getNextBillingDate = GetNextBillingDateUseCase(clock),
      addPriceChange = AddPriceChangeUseCase(subscriptionRepository, priceHistoryRepository),
      cancelSubscription = CancelSubscriptionUseCase(subscriptionRepository),
      archiveSubscription = ArchiveSubscriptionUseCase(subscriptionRepository),
      deleteSubscription = DeleteSubscriptionUseCase(subscriptionRepository),
      clock = clock,
    )

  @Test
  fun buildsCurrentPriceNextBillingAndTimelineDeltas() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()
      val subscription = create()
      priceHistoryRepository.addPriceChange(subscription.id, BigDecimal("9.99"), LocalDate.of(2023, 1, 1))
      priceHistoryRepository.addPriceChange(subscription.id, BigDecimal("14.99"), LocalDate.of(2024, 1, 1))

      viewModel.initialize(subscription.id.toString())

      viewModel.uiState.test {
        val state = awaitLoaded()

        assertEquals(BigDecimal("14.99"), state.currentPrice)
        assertEquals(LocalDate.of(2024, 6, 1), state.nextBillingDate)
        assertEquals(2, state.timeline.size)
        assertEquals(BigDecimal("5.00"), state.timeline.first().delta)
        assertNull(state.timeline.last().delta)
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun cancel_transitionsToCancelled() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()
      val subscription = create()
      viewModel.initialize(subscription.id.toString())

      viewModel.cancel()
      advanceUntilIdle()

      val updated = subscriptionRepository.getSubscription(subscription.id)
      assertEquals(SubscriptionStatus.CANCELLED, updated?.status)
      assertEquals(LocalDate.of(2024, 5, 20), updated?.endDate)
    }

  @Test
  fun archive_isIgnoredUntilCancelled() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()
      val subscription = create()
      viewModel.initialize(subscription.id.toString())

      viewModel.archive()
      advanceUntilIdle()

      assertEquals(SubscriptionStatus.ACTIVE, subscriptionRepository.getSubscription(subscription.id)?.status)
    }

  @Test
  fun addPrice_appendsToTimeline() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()
      val subscription = create()
      viewModel.initialize(subscription.id.toString())

      viewModel.addPrice(BigDecimal("19.99"), LocalDate.of(2024, 6, 1))
      advanceUntilIdle()

      assertEquals(BigDecimal("19.99"), priceHistoryRepository.getTimeline(subscription.id).single().price)
    }

  private suspend fun create(): Subscription =
    subscriptionRepository.createSubscription(
      SubscriptionDraft(
        name = "Netflix",
        iconId = "netflix",
        startDate = LocalDate.of(2024, 1, 1),
        billingCycle = BillingCycle.MONTHLY,
      ),
    )

  private suspend fun ReceiveTurbine<SubscriptionDetailUiState>.awaitLoaded(): SubscriptionDetailUiState {
    var state = awaitItem()
    while (state.isLoading) {
      state = awaitItem()
    }
    return state
  }
}
