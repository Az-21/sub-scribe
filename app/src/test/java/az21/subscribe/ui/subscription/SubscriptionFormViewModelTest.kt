package az21.subscribe.ui.subscription

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
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
import az21.subscribe.domain.model.ReminderSpec
import az21.subscribe.domain.model.Subscription
import az21.subscribe.domain.model.SubscriptionDraft
import az21.subscribe.domain.usecase.AddPriceChangeUseCase
import az21.subscribe.domain.usecase.SavePriceHistoryUseCase
import az21.subscribe.domain.usecase.ScheduleReminderUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
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
      savePriceHistory =
        SavePriceHistoryUseCase(
          subscriptionRepository,
          priceHistoryRepository,
          AddPriceChangeUseCase(subscriptionRepository, priceHistoryRepository),
        ),
      scheduleReminder = ScheduleReminderUseCase(subscriptionRepository, reminderScheduler),
      clock = clock,
    )

  @Test
  fun initializeWithoutId_startsInAddMode() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()

      viewModel.uiState.test {
        val state = awaitLoaded()
        viewModel.initialize(null)

        assertEquals(false, state.isLoading)
        assertEquals(false, state.isEditing)
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun save_createsSubscriptionWithInitialPrice() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()

      viewModel.uiState.test {
        awaitLoaded()
        viewModel.initialize(null)
        viewModel.onNameChange("Netflix")
        viewModel.onIconChange("netflix")
        viewModel.onPriceEntryChange(0, "15.99")
        viewModel.onBillingCycleChange(BillingCycle.MONTHLY)

        viewModel.save()

        await { it.saved }
        val created = subscriptionRepository.observeSubscriptions().first().single()
        assertEquals("Netflix", created.name)
        assertEquals("netflix", created.iconId)
        assertEquals(BigDecimal("15.99"), priceHistoryRepository.getTimeline(created.id).single().price)
        assertEquals(listOf(created.id), reminderScheduler.scheduled.map { it.id })
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun save_persistsIconColor() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()

      viewModel.uiState.test {
        awaitLoaded()
        viewModel.initialize(null)
        viewModel.onNameChange("Netflix")
        viewModel.onPriceEntryChange(0, "9.99")
        viewModel.onIconColorChange(0xFFE57373.toInt())

        viewModel.save()

        await { it.saved }
        val created = subscriptionRepository.observeSubscriptions().first().single()
        assertEquals(0xFFE57373.toInt(), created.iconColor)
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun initializeWithId_loadsIconColor() =
    runTest(mainDispatcherRule.testDispatcher) {
      val existing =
        subscriptionRepository.createSubscription(
          SubscriptionDraft(
            name = "Spotify",
            iconId = "simple:spotify",
            startDate = LocalDate.of(2024, 1, 1),
            billingCycle = BillingCycle.ANNUAL,
            iconColor = 0xFF1DB954.toInt(),
          ),
        )
      val viewModel = createViewModel()

      viewModel.uiState.test {
        awaitLoaded()
        viewModel.initialize(existing.id.toString())

        val state = await { !it.isLoading && it.isEditing && it.name == "Spotify" }
        assertEquals(0xFF1DB954.toInt(), state.iconColor)
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun save_withBlankName_requiresNameAndCreatesNothing() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()

      viewModel.uiState.test {
        awaitLoaded()
        viewModel.initialize(null)
        viewModel.onPriceEntryChange(0, "9.99")

        viewModel.save()

        val state = await { it.errors.name }
        assertTrue(state.errors.name)
        assertTrue(subscriptionRepository.observeSubscriptions().first().isEmpty())
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun initializeWithId_loadsExistingValues() =
    runTest(mainDispatcherRule.testDispatcher) {
      val existing =
        subscriptionRepository.createSubscription(
          SubscriptionDraft(
            name = "Spotify",
            iconId = "spotify",
            startDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2025, 1, 1),
            billingCycle = BillingCycle.ANNUAL,
          ),
        )
      priceHistoryRepository.addPriceChange(existing.id, BigDecimal("99.00"), LocalDate.of(2024, 1, 1))
      val viewModel = createViewModel()

      viewModel.uiState.test {
        awaitLoaded()
        viewModel.initialize(existing.id.toString())

        val state = await { !it.isLoading && it.isEditing && it.name == "Spotify" }
        assertEquals("99", state.priceEntries.single().price)
        assertEquals(LocalDate.of(2024, 1, 1), state.priceEntries.single().effectiveFromDate)
        assertEquals(LocalDate.of(2024, 1, 1), state.startDate)
        assertEquals(LocalDate.of(2025, 1, 1), state.endDate)
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun save_persistsEndDate() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()

      viewModel.uiState.test {
        awaitLoaded()
        viewModel.initialize(null)
        viewModel.onNameChange("Netflix")
        viewModel.onPriceEntryChange(0, "9.99")
        viewModel.onEndDateChange(LocalDate.of(2024, 12, 31))

        viewModel.save()

        await { it.saved }
        val created = subscriptionRepository.observeSubscriptions().first().single()
        assertEquals(LocalDate.of(2024, 12, 31), created.endDate)
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun save_whenPriceEntryEdited_updatesItInPlace() =
    runTest(mainDispatcherRule.testDispatcher) {
      val existing = createExistingSubscription()
      val entry =
        priceHistoryRepository.addPriceChange(existing.id, BigDecimal("99.00"), LocalDate.of(2024, 1, 1))
      val viewModel = createViewModel()

      viewModel.uiState.test {
        awaitLoaded()
        viewModel.initialize(existing.id.toString())
        await { !it.isLoading && it.name == "Spotify" }

        viewModel.onPriceEntryChange(0, "109.00")
        viewModel.save()

        await { it.saved }
        val timeline = priceHistoryRepository.getTimeline(existing.id)
        assertEquals(listOf(BigDecimal("109.00")), timeline.map { it.price })
        assertEquals(entry.id, timeline.single().id)
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun save_whenPriceEntryAdded_appendsIt() =
    runTest(mainDispatcherRule.testDispatcher) {
      val existing = createExistingSubscription()
      priceHistoryRepository.addPriceChange(existing.id, BigDecimal("99.00"), LocalDate.of(2024, 1, 1))
      val viewModel = createViewModel()

      viewModel.uiState.test {
        awaitLoaded()
        viewModel.initialize(existing.id.toString())
        await { !it.isLoading && it.name == "Spotify" }

        viewModel.onAddPriceEntry()
        viewModel.onPriceEntryChange(1, "119.00")
        viewModel.onPriceEntryDateChange(1, LocalDate.of(2024, 6, 1))
        viewModel.save()

        await { it.saved }
        assertEquals(
          listOf(BigDecimal("119.00"), BigDecimal("99.00")),
          priceHistoryRepository.getTimeline(existing.id).map { it.price },
        )
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun save_whenPriceEntryRemoved_deletesIt() =
    runTest(mainDispatcherRule.testDispatcher) {
      val existing = createExistingSubscription()
      priceHistoryRepository.addPriceChange(existing.id, BigDecimal("99.00"), LocalDate.of(2024, 1, 1))
      priceHistoryRepository.addPriceChange(existing.id, BigDecimal("109.00"), LocalDate.of(2024, 6, 1))
      val viewModel = createViewModel()

      viewModel.uiState.test {
        awaitLoaded()
        viewModel.initialize(existing.id.toString())
        await { !it.isLoading && it.name == "Spotify" }

        viewModel.onRemovePriceEntry(0)
        viewModel.save()

        await { it.saved }
        assertEquals(
          listOf(BigDecimal("99.00")),
          priceHistoryRepository.getTimeline(existing.id).map { it.price },
        )
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun save_withAllPriceEntriesRemoved_flagsAndCreatesNothing() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()

      viewModel.uiState.test {
        awaitLoaded()
        viewModel.initialize(null)
        viewModel.onNameChange("Netflix")
        viewModel.onRemovePriceEntry(0)

        viewModel.save()

        val state = await { it.errors.price }
        assertTrue(state.errors.price)
        assertTrue(subscriptionRepository.observeSubscriptions().first().isEmpty())
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun initializeWithoutId_defaultsStartDateToToday() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()

      viewModel.uiState.test {
        awaitLoaded()
        viewModel.initialize(null)

        val state = await { it.startDate != null }
        assertEquals(LocalDate.of(2024, 5, 20), state.startDate)
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun save_persistsMultipleReminders() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()

      viewModel.uiState.test {
        awaitLoaded()
        viewModel.initialize(null)
        viewModel.onNameChange("Netflix")
        viewModel.onPriceEntryChange(0, "9.99")
        viewModel.onAddReminder()
        viewModel.onReminderDaysChange(0, "3")
        viewModel.onReminderTimeChange(0, LocalTime.of(8, 30))
        viewModel.onAddReminder()
        viewModel.onReminderDaysChange(1, "7")
        viewModel.onReminderTimeChange(1, LocalTime.of(9, 0))

        viewModel.save()

        await { it.saved }
        val created = subscriptionRepository.observeSubscriptions().first().single()
        assertEquals(
          listOf(ReminderSpec(3, LocalTime.of(8, 30)), ReminderSpec(7, LocalTime.of(9, 0))),
          created.reminders,
        )
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun save_withInvalidReminderDays_flagsEntryAndCreatesNothing() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()

      viewModel.uiState.test {
        awaitLoaded()
        viewModel.initialize(null)
        viewModel.onNameChange("Netflix")
        viewModel.onPriceEntryChange(0, "9.99")
        viewModel.onAddReminder()

        viewModel.save()

        val state = await { it.reminders.any { entry -> entry.isError } }
        assertTrue(state.reminders.first().isError)
        assertTrue(subscriptionRepository.observeSubscriptions().first().isEmpty())
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun initializeWithId_loadsExistingReminders() =
    runTest(mainDispatcherRule.testDispatcher) {
      val existing =
        subscriptionRepository.createSubscription(
          SubscriptionDraft(
            name = "Spotify",
            iconId = "spotify",
            startDate = LocalDate.of(2024, 1, 1),
            billingCycle = BillingCycle.ANNUAL,
            reminders = listOf(ReminderSpec(5, LocalTime.of(7, 15))),
          ),
        )
      val viewModel = createViewModel()

      viewModel.uiState.test {
        awaitLoaded()
        viewModel.initialize(existing.id.toString())

        val state = await { !it.isLoading && it.reminders.isNotEmpty() }
        assertEquals("5", state.reminders.single().daysBefore)
        assertEquals(LocalTime.of(7, 15), state.reminders.single().time)
        cancelAndIgnoreRemainingEvents()
      }
    }

  private suspend fun createExistingSubscription(): Subscription =
    subscriptionRepository.createSubscription(
      SubscriptionDraft(
        name = "Spotify",
        iconId = "spotify",
        startDate = LocalDate.of(2024, 1, 1),
        billingCycle = BillingCycle.ANNUAL,
      ),
    )

  private suspend fun ReceiveTurbine<SubscriptionFormUiState>.awaitLoaded(): SubscriptionFormUiState =
    await { !it.isLoading }

  private suspend fun ReceiveTurbine<SubscriptionFormUiState>.await(
    predicate: (SubscriptionFormUiState) -> Boolean,
  ): SubscriptionFormUiState {
    var state = awaitItem()
    while (!predicate(state)) {
      state = awaitItem()
    }
    return state
  }
}
