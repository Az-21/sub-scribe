package az21.subscribe.ui.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az21.subscribe.domain.metrics.SpendCalculator
import az21.subscribe.domain.model.BillingCycle
import az21.subscribe.domain.model.SubscriptionDraft
import az21.subscribe.domain.repository.PaymentMethodRepository
import az21.subscribe.domain.repository.PriceHistoryRepository
import az21.subscribe.domain.repository.SubscriptionRepository
import az21.subscribe.domain.repository.TagRepository
import az21.subscribe.domain.usecase.AddPriceChangeUseCase
import az21.subscribe.domain.usecase.ScheduleReminderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
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
    private val addPriceChange: AddPriceChangeUseCase,
    private val scheduleReminder: ScheduleReminderUseCase,
    private val clock: Clock,
  ) : ViewModel() {
    private val form = MutableStateFlow(SubscriptionFormUiState())
    private var started = false
    private var editingId: UUID? = null
    private var loadedPrice: BigDecimal? = null

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
        form.value = form.value.copy(isLoading = false)
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
      val price = SpendCalculator.resolvePrice(priceHistoryRepository.getTimeline(id), LocalDate.now(clock))
      loadedPrice = price
      form.value =
        form.value.copy(
          isLoading = false,
          name = subscription.name,
          iconId = subscription.iconId,
          startDate = subscription.startDate,
          billingCycle = subscription.billingCycle,
          freeTrialMonths = subscription.freeTrialMonths?.toString().orEmpty(),
          price = price?.stripTrailingZeros()?.toPlainString().orEmpty(),
          reminderDaysBefore = subscription.reminderDaysBefore?.toString().orEmpty(),
          trialReminderEnabled = subscription.trialReminderEnabled,
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

    fun onStartDateChange(date: LocalDate?) {
      update { it.copy(startDate = date) }
    }

    fun onBillingCycleChange(cycle: BillingCycle) {
      update { it.copy(billingCycle = cycle) }
    }

    fun onFreeTrialMonthsChange(value: String) {
      update {
        it.copy(
          freeTrialMonths = value.filter(Char::isDigit),
          errors = it.errors.copy(freeTrialMonths = false),
        )
      }
    }

    fun onPriceChange(value: String) {
      update { it.copy(price = value, errors = it.errors.copy(price = false)) }
    }

    fun onReminderDaysChange(value: String) {
      update {
        it.copy(reminderDaysBefore = value.filter(Char::isDigit), errors = it.errors.copy(reminderDays = false))
      }
    }

    fun onTrialReminderChange(enabled: Boolean) {
      update { it.copy(trialReminderEnabled = enabled) }
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
      val price = state.price.toBigDecimalOrNull()?.takeIf { value -> value.signum() >= 0 }
      val reminderDays = state.reminderDaysBefore.toIntOrNull()
      val errors =
        FormErrors(
          name = state.name.isBlank(),
          price = price == null,
          freeTrialMonths = state.freeTrialMonths.isNotBlank() && state.freeTrialMonths.toIntOrNull() == null,
          reminderDays = state.reminderDaysBefore.isNotBlank() && reminderDays == null,
          trialReminder = state.trialReminderEnabled && reminderDays == null,
        )
      if (errors.hasErrors || price == null) {
        form.value = state.copy(errors = errors)
        return
      }
      viewModelScope.launch { persist(state, price) }
    }

    private suspend fun persist(
      state: SubscriptionFormUiState,
      price: BigDecimal,
    ) {
      val trialMonths = state.freeTrialMonths.toIntOrNull()
      val reminderDays = state.reminderDaysBefore.toIntOrNull()
      val notes = state.notes.ifBlank { null }
      val id = editingId

      if (id == null) {
        val created =
          subscriptionRepository.createSubscription(
            SubscriptionDraft(
              name = state.name.trim(),
              iconId = state.iconId,
              startDate = state.startDate,
              billingCycle = state.billingCycle,
              freeTrialMonths = trialMonths,
              reminderDaysBefore = reminderDays,
              trialReminderEnabled = state.trialReminderEnabled,
              paymentMethodId = state.paymentMethodId,
              notes = notes,
            ),
          )
        priceHistoryRepository.addPriceChange(
          subscriptionId = created.id,
          price = price,
          effectiveFromDate = state.startDate ?: LocalDate.now(clock),
        )
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
              billingCycle = state.billingCycle,
              freeTrialMonths = trialMonths,
              reminderDaysBefore = reminderDays,
              trialReminderEnabled = state.trialReminderEnabled,
              paymentMethodId = state.paymentMethodId,
              notes = notes,
            )
          subscriptionRepository.updateSubscription(updated)
          val previousPrice = loadedPrice
          if (previousPrice == null || price.compareTo(previousPrice) != 0) {
            addPriceChange(id, price, LocalDate.now(clock))
          }
          subscriptionRepository.setTags(id, state.selectedTagIds)
          scheduleReminder(updated)
        }
      }
      form.value = form.value.copy(saved = true)
    }

    private fun update(transform: (SubscriptionFormUiState) -> SubscriptionFormUiState) {
      form.value = transform(form.value)
    }

    private companion object {
      const val STOP_TIMEOUT_MILLIS = 5_000L
    }
  }
