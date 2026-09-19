package az21.subscribe.ui.home

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
import az21.subscribe.domain.usecase.GetNextBillingDateUseCase
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
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  private val clock = Clock.fixed(Instant.parse("2024-05-20T09:00:00Z"), ZoneOffset.UTC)
  private val subscriptionRepository = SubscriptionRepositoryImpl(FakeSubscriptionDao(), clock)
  private val tagDao = FakeTagDao()
  private val tagRepository = TagRepositoryImpl(tagDao)
  private val priceHistoryRepository = PriceHistoryRepositoryImpl(FakePriceHistoryDao(), clock)
  private val paymentMethodRepository = PaymentMethodRepositoryImpl(FakePaymentMethodDao())
  private val settingsRepository = FakeSettingsRepository()

  private fun createViewModel() =
    HomeViewModel(
      subscriptionRepository = subscriptionRepository,
      tagRepository = tagRepository,
      priceHistoryRepository = priceHistoryRepository,
      paymentMethodRepository = paymentMethodRepository,
      settingsRepository = settingsRepository,
      getNextBillingDate = GetNextBillingDateUseCase(clock),
      clock = clock,
    )

  @Test
  fun defaultsToActiveSubscriptionsOnly() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()
      create("Netflix", status = SubscriptionStatus.ACTIVE)
      create("Old Gym", status = SubscriptionStatus.CANCELLED)

      viewModel.uiState.test {
        val state = awaitLoaded()

        assertEquals(listOf("Netflix"), state.items.map { it.subscription.name })
        assertTrue(state.hasSubscriptions)
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun searchQuery_filtersByName() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()
      create("Netflix")
      create("Spotify")

      viewModel.uiState.test {
        awaitLoaded()
        viewModel.onQueryChange("spot")

        val state = awaitLoaded()
        assertEquals(listOf("Spotify"), state.items.map { it.subscription.name })
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun sortByPrice_ordersDescending() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()
      val cheap = create("Cheap")
      val pricey = create("Pricey")
      priceHistoryRepository.addPriceChange(cheap.id, BigDecimal("5.00"), LocalDate.of(2024, 1, 1))
      priceHistoryRepository.addPriceChange(pricey.id, BigDecimal("25.00"), LocalDate.of(2024, 1, 1))

      viewModel.uiState.test {
        awaitLoaded()
        viewModel.onSortChange(SubscriptionSort.PRICE)

        val state = awaitLoaded()
        assertEquals(listOf("Pricey", "Cheap"), state.items.map { it.subscription.name })
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun tagFilter_limitsToTaggedSubscriptions() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()
      val tagged = create("Netflix")
      create("Spotify")
      val tag = tagRepository.createTag("Streaming")
      tagDao.assignments.value = mapOf(tagged.id to setOf(tag.id))

      viewModel.uiState.test {
        awaitLoaded()
        viewModel.onTagChange(tag.id)

        val state = awaitLoaded()
        assertEquals(listOf("Netflix"), state.items.map { it.subscription.name })
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun statusFilter_canRevealCancelledSubscriptions() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()
      create("Netflix", status = SubscriptionStatus.ACTIVE)
      create("Old Gym", status = SubscriptionStatus.CANCELLED)

      viewModel.uiState.test {
        awaitLoaded()
        viewModel.onStatusChange(SubscriptionStatus.CANCELLED)

        val state = awaitLoaded()
        assertEquals(listOf("Old Gym"), state.items.map { it.subscription.name })
        cancelAndIgnoreRemainingEvents()
      }
    }

  private suspend fun ReceiveTurbine<HomeUiState>.awaitLoaded(): HomeUiState {
    var state = awaitItem()
    while (state.isLoading) {
      state = awaitItem()
    }
    return state
  }

  private suspend fun create(
    name: String,
    status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
  ): Subscription {
    val created = subscriptionRepository.createSubscription(draft(name))
    return when (status) {
      SubscriptionStatus.ACTIVE -> created
      SubscriptionStatus.CANCELLED -> subscriptionRepository.cancelSubscription(created.id)
      SubscriptionStatus.ARCHIVED ->
        subscriptionRepository.archiveSubscription(
          subscriptionRepository.cancelSubscription(created.id).id,
        )
    }
  }

  private fun draft(name: String): SubscriptionDraft =
    SubscriptionDraft(
      name = name,
      iconId = "test",
      startDate = LocalDate.of(2024, 1, 1),
      billingCycle = BillingCycle.MONTHLY,
    )
}
