package az21.subscribe.ui.archive

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import az21.subscribe.MainDispatcherRule
import az21.subscribe.data.fake.FakePriceHistoryDao
import az21.subscribe.data.fake.FakeSettingsRepository
import az21.subscribe.data.fake.FakeSubscriptionDao
import az21.subscribe.data.fake.FakeTagDao
import az21.subscribe.data.repository.PriceHistoryRepositoryImpl
import az21.subscribe.data.repository.SubscriptionRepositoryImpl
import az21.subscribe.data.repository.TagRepositoryImpl
import az21.subscribe.domain.model.BillingCycle
import az21.subscribe.domain.model.Subscription
import az21.subscribe.domain.model.SubscriptionDraft
import az21.subscribe.domain.model.SubscriptionStatus
import az21.subscribe.domain.usecase.ArchiveSubscriptionUseCase
import az21.subscribe.domain.usecase.DeleteSubscriptionUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class ArchiveViewModelTest {
  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  private val clock = Clock.fixed(Instant.parse("2024-05-20T09:00:00Z"), ZoneOffset.UTC)
  private val subscriptionRepository = SubscriptionRepositoryImpl(FakeSubscriptionDao(), clock)
  private val tagRepository = TagRepositoryImpl(FakeTagDao())
  private val priceHistoryRepository = PriceHistoryRepositoryImpl(FakePriceHistoryDao(), clock)
  private val settingsRepository = FakeSettingsRepository()

  private fun createViewModel() =
    ArchiveViewModel(
      subscriptionRepository = subscriptionRepository,
      tagRepository = tagRepository,
      priceHistoryRepository = priceHistoryRepository,
      settingsRepository = settingsRepository,
      archiveSubscription = ArchiveSubscriptionUseCase(subscriptionRepository),
      deleteSubscription = DeleteSubscriptionUseCase(subscriptionRepository),
      clock = clock,
    )

  @Test
  fun listsOnlyCancelledAndArchivedSubscriptions() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()
      createRaw("Netflix")
      createCancelled("Old Gym")
      createArchived("Gym")

      viewModel.uiState.test {
        val state = awaitLoaded()

        assertEquals(listOf("Gym", "Old Gym"), state.items.map { it.subscription.name })
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun confirmDelete_removesThePendingSubscription() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()
      createArchived("Gym")

      viewModel.uiState.test {
        val state = awaitLoaded()
        viewModel.requestDelete(state.items.single())
        viewModel.confirmDelete()
        advanceUntilIdle()

        assertTrue(subscriptionRepository.observeSubscriptions().first().isEmpty())
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun archive_movesCancelledSubscriptionToArchived() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()
      val cancelled = createCancelled("Old Gym")

      viewModel.uiState.test {
        awaitLoaded()
        viewModel.archive(cancelled.id)
        advanceUntilIdle()

        assertEquals(
          SubscriptionStatus.ARCHIVED,
          subscriptionRepository.getSubscription(cancelled.id)?.status,
        )
        cancelAndIgnoreRemainingEvents()
      }
    }

  private suspend fun ReceiveTurbine<ArchiveUiState>.awaitLoaded(): ArchiveUiState {
    var state = awaitItem()
    while (state.isLoading) {
      state = awaitItem()
    }
    return state
  }

  private suspend fun createCancelled(name: String): Subscription =
    subscriptionRepository.cancelSubscription(createRaw(name).id)

  private suspend fun createArchived(name: String): Subscription =
    subscriptionRepository.archiveSubscription(createCancelled(name).id)

  private suspend fun createRaw(name: String): Subscription =
    subscriptionRepository.createSubscription(
      SubscriptionDraft(
        name = name,
        iconId = "test",
        startDate = LocalDate.of(2024, 1, 1),
        billingCycle = BillingCycle.MONTHLY,
      ),
    )
}
