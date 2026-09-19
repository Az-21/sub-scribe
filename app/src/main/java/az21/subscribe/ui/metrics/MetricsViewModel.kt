package az21.subscribe.ui.metrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az21.subscribe.domain.metrics.YearlySpend
import az21.subscribe.domain.repository.SettingsRepository
import az21.subscribe.domain.usecase.GetSpendMetricsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.Month
import javax.inject.Inject

/**
 * Drives the metrics screen: a browsable calendar year of true-monthly charges, the divvied-annual
 * view of annual subscriptions, and the true-total for the year, all in the selected currency.
 */
@HiltViewModel
class MetricsViewModel
  @Inject
  constructor(
    private val getSpendMetrics: GetSpendMetricsUseCase,
    settingsRepository: SettingsRepository,
    clock: Clock,
  ) : ViewModel() {
    private val today = LocalDate.now(clock)
    private val selectedYear = MutableStateFlow(today.year)
    private val selectedMonth = MutableStateFlow(today.month)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val metrics: StateFlow<YearlySpend?> =
      selectedYear
        .flatMapLatest { year -> flow { emit(getSpendMetrics(year)) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    val uiState: StateFlow<MetricsUiState> =
      combine(metrics, selectedMonth, settingsRepository.settings) { yearly, month, settings ->
        MetricsUiState(
          year = yearly?.year ?: selectedYear.value,
          selectedMonth = month,
          isLoading = yearly == null,
          monthly = yearly?.monthly.orEmpty(),
          totalSpend = yearly?.trueAnnualSpend ?: BigDecimal.ZERO,
          divviedAnnualSpend = yearly?.divviedAnnualSpend ?: BigDecimal.ZERO,
          currency = settings.currency,
        )
      }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        MetricsUiState(year = today.year, selectedMonth = today.month),
      )

    fun onPreviousYear() {
      selectedYear.value -= 1
    }

    fun onNextYear() {
      selectedYear.value += 1
    }

    fun onMonthSelected(month: Month) {
      selectedMonth.value = month
    }

    private companion object {
      const val STOP_TIMEOUT_MILLIS = 5_000L
    }
  }
