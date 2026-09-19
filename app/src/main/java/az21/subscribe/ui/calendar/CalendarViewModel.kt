package az21.subscribe.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az21.subscribe.domain.billing.BillingSchedule
import az21.subscribe.domain.metrics.SpendCalculator
import az21.subscribe.domain.model.Currency
import az21.subscribe.domain.model.PriceHistory
import az21.subscribe.domain.model.Subscription
import az21.subscribe.domain.model.SubscriptionStatus
import az21.subscribe.domain.repository.PriceHistoryRepository
import az21.subscribe.domain.repository.SettingsRepository
import az21.subscribe.domain.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject

/**
 * Drives the calendar: lays out the selected month and lists, per day, the active subscriptions that
 * are charged on it using the historically-effective price.
 */
@HiltViewModel
class CalendarViewModel
  @Inject
  constructor(
    subscriptionRepository: SubscriptionRepository,
    priceHistoryRepository: PriceHistoryRepository,
    settingsRepository: SettingsRepository,
    private val clock: Clock,
  ) : ViewModel() {
    private val visibleMonth = MutableStateFlow(YearMonth.now(clock))
    private val selectedDate = MutableStateFlow(LocalDate.now(clock))

    val uiState: StateFlow<CalendarUiState> =
      combine(
        subscriptionRepository.observeSubscriptions(),
        priceHistoryRepository.observeAllTimelines(),
        settingsRepository.settings,
        visibleMonth,
        selectedDate,
      ) { subscriptions, timelines, settings, month, selected ->
        buildState(subscriptions, timelines, month, selected, settings.currency)
      }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        CalendarUiState(year = visibleMonth.value.year, month = visibleMonth.value.month),
      )

    fun onPreviousMonth() {
      visibleMonth.value = visibleMonth.value.minusMonths(1)
      selectedDate.value = visibleMonth.value.atDay(1)
    }

    fun onNextMonth() {
      visibleMonth.value = visibleMonth.value.plusMonths(1)
      selectedDate.value = visibleMonth.value.atDay(1)
    }

    fun onDaySelected(date: LocalDate) {
      selectedDate.value = date
    }

    private fun buildState(
      subscriptions: List<Subscription>,
      timelines: Map<UUID, List<PriceHistory>>,
      month: YearMonth,
      selected: LocalDate,
      currency: Currency,
    ): CalendarUiState {
      val monthStart = month.atDay(1)
      val monthEnd = month.atEndOfMonth()
      val charges =
        subscriptions
          .filter { subscription -> subscription.status == SubscriptionStatus.ACTIVE }
          .flatMap { subscription ->
            val timeline = timelines[subscription.id].orEmpty()
            BillingSchedule.billingDatesBetween(subscription, monthStart, monthEnd).mapNotNull { date ->
              CalendarCharge(
                subscriptionId = subscription.id,
                name = subscription.name,
                iconId = subscription.iconId,
                date = date,
                price = SpendCalculator.resolvePrice(timeline, date),
                iconColor = subscription.iconColor,
              )
            }
          }

      val chargesByDate = charges.groupBy { charge -> charge.date }
      val days =
        (1..month.lengthOfMonth()).map { day ->
          val date = month.atDay(day)
          CalendarDay(date = date, charges = chargesByDate[date].orEmpty())
        }

      return CalendarUiState(
        year = month.year,
        month = month.month,
        isLoading = false,
        firstDayOffset = monthStart.dayOfWeek.value - 1,
        days = days,
        selectedDate = selected,
        selectedCharges = chargesByDate[selected].orEmpty(),
        monthTotal = charges.mapNotNull { charge -> charge.price }.fold(BigDecimal.ZERO, BigDecimal::add),
        currency = currency,
      )
    }

    private companion object {
      const val STOP_TIMEOUT_MILLIS = 5_000L
    }
  }
