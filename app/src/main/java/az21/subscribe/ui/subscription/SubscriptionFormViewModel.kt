package az21.subscribe.ui.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az21.subscribe.domain.model.BillingCycle
import az21.subscribe.domain.model.PriceEntryDraft
import az21.subscribe.domain.model.ReminderSpec
import az21.subscribe.domain.model.SubscriptionDraft
import az21.subscribe.domain.repository.PaymentMethodRepository
import az21.subscribe.domain.repository.PriceHistoryRepository
import az21.subscribe.domain.repository.SubscriptionRepository
import az21.subscribe.domain.repository.TagRepository
import az21.subscribe.domain.usecase.SavePriceHistoryUseCase
import az21.subscribe.domain.usecase.ScheduleReminderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject

/**
 * Drives the add/edit subscription form. Adding creates the subscription plus its first price entry;
 * editing updates the subscription and records a new price point when the price changed.
 *
 * The function count intentionally exceeds the default threshold: each editable field has its own
 * handler so the composable can stay stateless and receive plain callbacks.
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class SubscriptionFormViewModel
  @Inject
  constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val priceHistoryRepository: PriceHistoryRepository,
    tagRepository: TagRepository,
    paymentMethodRepository: PaymentMethodRepository,
    private val savePriceHistory: SavePriceHistoryUseCase,
    private val scheduleReminder: ScheduleReminderUseCase,
    private val clock: Clock,
  ) : ViewModel() {
    private val form = MutableStateFlow(SubscriptionFormUiState())
    private var started = false
    private var editingId: UUID? = null

    val uiState: StateFlow<SubscriptionFormUiState> =
      combine(
        form,
        tagRepository.observeTags(),
        paymentMethodRepository.observePaymentMethods(),
      ) { state, tags, paymentMethods ->
        state.copy(availableTags = tags, paymentMethods = paymentMethods)
      }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        SubscriptionFormUiState(),
      )

    /** Loads the subscription being edited. Safe to call repeatedly: only the first call loads. */
    fun initialize(subscriptionId: String?) {
      if (started) return
      started = true
      if (subscriptionId == null) {
        val today = LocalDate.now(clock)
        form.value =
          form.value.copy(
            isLoading = false,
            startDate = today,
            priceEntries = listOf(PriceEntry(effectiveFromDate = today)),
          )
        return
      }
      val id = UUID.fromString(subscriptionId)
      editingId = id
      form.value = form.value.copy(isEditing = true, isLoading = true)
      viewModelScope.launch { load(id) }
    }

    private suspend fun load(id: UUID) {
      val subscription = subscriptionRepository.getSubscription(id)
      if (subscription == null) {
        form.value = form.value.copy(isLoading = false)
        return
      }
      val entries =
        priceHistoryRepository.getTimeline(id).map { entry ->
          PriceEntry(
            id = entry.id,
            price = entry.price.stripTrailingZeros().toPlainString(),
            effectiveFromDate = entry.effectiveFromDate,
          )
        }
      form.value =
        form.value.copy(
          isLoading = false,
          name = subscription.name,
          iconId = subscription.iconId,
          iconColor = subscription.iconColor,
          startDate = subscription.startDate,
          endDate = subscription.endDate,
          billingCycle = subscription.billingCycle,
          priceEntries = entries.ifEmpty { listOf(PriceEntry(effectiveFromDate = subscription.startDate)) },
          reminders =
            subscription.reminders.map { reminder ->
              ReminderEntry(daysBefore = reminder.daysBefore.toString(), time = reminder.time)
            },
          selectedTagIds = subscriptionRepository.observeTagIds(id).first().toSet(),
          paymentMethodId = subscription.paymentMethodId,
          notes = subscription.notes.orEmpty(),
        )
    }

    fun onNameChange(value: String) {
      update { it.copy(name = value, errors = it.errors.copy(name = false)) }
    }

    fun onIconChange(iconId: String) {
      update { it.copy(iconId = iconId) }
    }

    fun onIconColorChange(color: Int?) {
      update { it.copy(iconColor = color) }
    }

    fun onStartDateChange(date: LocalDate?) {
      update { it.copy(startDate = date) }
    }

    fun onEndDateChange(date: LocalDate?) {
      update { it.copy(endDate = date) }
    }

    fun onBillingCycleChange(cycle: BillingCycle) {
      update { it.copy(billingCycle = cycle) }
    }

    fun onPriceEntryChange(
      index: Int,
      value: String,
    ) {
      updatePriceEntry(index) { entry -> entry.copy(price = value, isError = false) }
    }

    fun onPriceEntryDateChange(
      index: Int,
      date: LocalDate,
    ) {
      updatePriceEntry(index) { entry -> entry.copy(effectiveFromDate = date) }
    }

    fun onAddPriceEntry() {
      update { state ->
        state.copy(priceEntries = state.priceEntries + PriceEntry(effectiveFromDate = LocalDate.now(clock)))
      }
    }

    fun onRemovePriceEntry(index: Int) {
      update { state ->
        state.copy(priceEntries = state.priceEntries.filterIndexed { i, _ -> i != index })
      }
    }

    fun onAddReminder() {
      update { it.copy(reminders = it.reminders + ReminderEntry()) }
    }

    fun onRemoveReminder(index: Int) {
      update { state ->
        state.copy(reminders = state.reminders.filterIndexed { i, _ -> i != index })
      }
    }

    fun onReminderDaysChange(
      index: Int,
      value: String,
    ) {
      updateReminder(index) { entry ->
        entry.copy(daysBefore = value.filter(Char::isDigit), isError = false)
      }
    }

    fun onReminderTimeChange(
      index: Int,
      time: LocalTime,
    ) {
      updateReminder(index) { entry -> entry.copy(time = time) }
    }

    fun onToggleTag(tagId: UUID) {
      update { state ->
        val tags = state.selectedTagIds
        state.copy(selectedTagIds = if (tagId in tags) tags - tagId else tags + tagId)
      }
    }

    fun onPaymentMethodChange(paymentMethodId: UUID?) {
      update { it.copy(paymentMethodId = paymentMethodId) }
    }

    fun onNotesChange(value: String) {
      update { it.copy(notes = value) }
    }

    fun save() {
      val state = form.value
      val parsedPrices =
        state.priceEntries.map { entry ->
          entry.price.toBigDecimalOrNull()?.takeIf { value -> value.signum() >= 0 }?.let { price ->
            PriceEntryDraft(id = entry.id, price = price, effectiveFromDate = entry.effectiveFromDate)
          }
        }
      val priceEntries =
        state.priceEntries.mapIndexed { index, entry ->
          entry.copy(isError = parsedPrices[index] == null)
        }
      val specs =
        state.reminders.map { entry ->
          entry.daysBefore
            .toIntOrNull()
            ?.takeIf { it >= 0 }
            ?.let { days -> ReminderSpec(days, entry.time) }
        }
      val reminders =
        state.reminders.mapIndexed { index, entry ->
          ReminderEntry(daysBefore = entry.daysBefore, time = entry.time, isError = specs[index] == null)
        }
      val errors =
        FormErrors(
          name = state.name.isBlank(),
          price = state.priceEntries.isEmpty() || parsedPrices.any { it == null },
        )
      if (errors.hasErrors || specs.any { it == null }) {
        form.value = state.copy(errors = errors, priceEntries = priceEntries, reminders = reminders)
        return
      }
      viewModelScope.launch { persist(state, parsedPrices.filterNotNull(), specs.filterNotNull()) }
    }

    private suspend fun persist(
      state: SubscriptionFormUiState,
      priceEntries: List<PriceEntryDraft>,
      specs: List<ReminderSpec>,
    ) {
      val notes = state.notes.ifBlank { null }
      val id = editingId

      if (id == null) {
        val created =
          subscriptionRepository.createSubscription(
            SubscriptionDraft(
              name = state.name.trim(),
              iconId = state.iconId,
              startDate = state.startDate,
              endDate = state.endDate,
              billingCycle = state.billingCycle,
              reminders = specs,
              paymentMethodId = state.paymentMethodId,
              notes = notes,
              iconColor = state.iconColor,
            ),
          )
        savePriceHistory(created.id, priceEntries)
        subscriptionRepository.setTags(created.id, state.selectedTagIds)
        scheduleReminder(created)
      } else {
        val existing = subscriptionRepository.getSubscription(id)
        if (existing != null) {
          val updated =
            existing.copy(
              name = state.name.trim(),
              iconId = state.iconId,
              startDate = state.startDate ?: existing.startDate,
              endDate = state.endDate,
              billingCycle = state.billingCycle,
              reminders = specs,
              paymentMethodId = state.paymentMethodId,
              notes = notes,
              iconColor = state.iconColor,
            )
          subscriptionRepository.updateSubscription(updated)
          savePriceHistory(id, priceEntries)
          subscriptionRepository.setTags(id, state.selectedTagIds)
          scheduleReminder(updated)
        }
      }
      form.value = form.value.copy(saved = true)
    }

    private fun updatePriceEntry(
      index: Int,
      transform: (PriceEntry) -> PriceEntry,
    ) {
      update { state ->
        state.copy(
          priceEntries =
            state.priceEntries.mapIndexed { i, entry -> if (i == index) transform(entry) else entry },
        )
      }
    }

    private fun updateReminder(
      index: Int,
      transform: (ReminderEntry) -> ReminderEntry,
    ) {
      update { state ->
        state.copy(
          reminders = state.reminders.mapIndexed { i, entry -> if (i == index) transform(entry) else entry },
        )
      }
    }

    private fun update(transform: (SubscriptionFormUiState) -> SubscriptionFormUiState) {
      form.value = transform(form.value)
    }

    private companion object {
      const val STOP_TIMEOUT_MILLIS = 5_000L
    }
  }
