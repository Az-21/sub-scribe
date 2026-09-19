package az21.subscribe.ui.subscription

import az21.subscribe.MainDispatcherRule
import az21.subscribe.data.fake.FakePaymentMethodDao
import az21.subscribe.data.fake.FakePriceHistoryDao
import az21.subscribe.data.fake.FakeReminderScheduler
import az21.subscribe.data.fake.FakeSubscriptionDao
import az21.subscribe.data.fake.FakeTagDao
import az21.subscribe.data.repository.PaymentMethodRepositoryImpl
import az21.subscribe.data.repository.PriceHistoryRepositoryImpl
import az21.subscribe.data.repository.SubscriptionRepositoryImpl
import az21.subscribe.data.repository.TagRepositoryImpl
import az21.subscribe.domain.model.BillingCycle
import az21.subscribe.domain.model.SubscriptionDraft
import az21.subscribe.domain.usecase.AddPriceChangeUseCase
import az21.subscribe.domain.usecase.ScheduleReminderUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
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
class SubscriptionFormViewModelTest {
  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  private val clock = Clock.fixed(Instant.parse("2024-05-20T09:00:00Z"), ZoneOffset.UTC)
  private val subscriptionRepository = SubscriptionRepositoryImpl(FakeSubscriptionDao(), clock)
  private val priceHistoryRepository = PriceHistoryRepositoryImpl(FakePriceHistoryDao(), clock)
  private val tagRepository = TagRepositoryImpl(FakeTagDao())
  private val paymentMethodRepository = PaymentMethodRepositoryImpl(FakePaymentMethodDao())
  private val reminderScheduler = FakeReminderScheduler()

  private fun createViewModel() =
    SubscriptionFormViewModel(
      subscriptionRepository = subscriptionRepository,
      priceHistoryRepository = priceHistoryRepository,
      tagRepository = tagRepository,
      paymentMethodRepository = paymentMethodRepository,
      addPriceChange = AddPriceChangeUseCase(subscriptionRepository, priceHistoryRepository),
      scheduleReminder = ScheduleReminderUseCase(subscriptionRepository, reminderScheduler),
      clock = clock,
    )

  @Test
  fun initializeWithoutId_startsInAddMode() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()
      collectUiState(viewModel)
      viewModel.initialize(null)
      advanceUntilIdle()

      assertEquals(false, viewModel.uiState.value.isLoading)
      assertEquals(false, viewModel.uiState.value.isEditing)
    }

  @Test
  fun save_createsSubscriptionWithInitialPrice() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()
      collectUiState(viewModel)
      viewModel.initialize(null)
      viewModel.onNameChange("Netflix")
      viewModel.onIconChange("netflix")
      viewModel.onPriceChange("15.99")
      viewModel.onBillingCycleChange(BillingCycle.MONTHLY)

      viewModel.save()
      advanceUntilIdle()

      val created = subscriptionRepository.observeSubscriptions().first().single()
      assertEquals("Netflix", created.name)
      assertEquals("netflix", created.iconId)
      assertEquals(BigDecimal("15.99"), priceHistoryRepository.getTimeline(created.id).single().price)
      assertEquals(listOf(created.id), reminderScheduler.scheduled.map { it.id })
      assertTrue(viewModel.uiState.value.saved)
    }

  @Test
  fun save_withBlankName_requiresNameAndCreatesNothing() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()
      collectUiState(viewModel)
      viewModel.initialize(null)
      viewModel.onPriceChange("9.99")

      viewModel.save()
      advanceUntilIdle()

      assertTrue(viewModel.uiState.value.errors.name)
      assertTrue(subscriptionRepository.observeSubscriptions().first().isEmpty())
    }

  @Test
  fun initializeWithId_loadsExistingValues() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()
      collectUiState(viewModel)
      val existing =
        subscriptionRepository.createSubscription(
          SubscriptionDraft(
            name = "Spotify",
            iconId = "spotify",
            startDate = LocalDate.of(2024, 1, 1),
            billingCycle = BillingCycle.ANNUAL,
          ),
        )
      priceHistoryRepository.addPriceChange(existing.id, BigDecimal("99.00"), LocalDate.of(2024, 1, 1))

      viewModel.initialize(existing.id.toString())
      advanceUntilIdle()

      val state = viewModel.uiState.value
      assertEquals(false, state.isLoading)
      assertEquals(true, state.isEditing)
      assertEquals("Spotify", state.name)
      assertEquals("99", state.price)
    }

  @Test
  fun save_whenPriceChanged_addsNewPriceEntry() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()
      collectUiState(viewModel)
      val existing =
        subscriptionRepository.createSubscription(
          SubscriptionDraft(
            name = "Spotify",
            iconId = "spotify",
            startDate = LocalDate.of(2024, 1, 1),
            billingCycle = BillingCycle.ANNUAL,
          ),
        )
      priceHistoryRepository.addPriceChange(existing.id, BigDecimal("99.00"), LocalDate.of(2024, 1, 1))
      viewModel.initialize(existing.id.toString())
      advanceUntilIdle()
      viewModel.onPriceChange("109.00")

      viewModel.save()
      advanceUntilIdle()

      assertEquals(
        listOf(BigDecimal("109.00"), BigDecimal("99.00")),
        priceHistoryRepository.getTimeline(existing.id).map { it.price },
      )
    }

  private fun TestScope.collectUiState(viewModel: SubscriptionFormViewModel) {
    backgroundScope.launch { viewModel.uiState.collect {} }
  }
}
