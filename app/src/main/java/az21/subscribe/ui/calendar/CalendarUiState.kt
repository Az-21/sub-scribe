package az21.subscribe.ui.calendar

import az21.subscribe.domain.model.Currency
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month
import java.util.UUID

/** A single charge landing on a day in the calendar. */
data class CalendarCharge(
  val subscriptionId: UUID,
  val name: String,
  val iconId: String,
  val date: LocalDate,
  val price: BigDecimal?,
  val iconColor: Int? = null,
)

/** A day cell with the charges that land on it. */
data class CalendarDay(
  val date: LocalDate,
  val charges: List<CalendarCharge>,
)

/**
 * Immutable state for the calendar screen. [firstDayOffset] is the number of blank cells before the
 * first of the month in a Monday-first grid.
 */
data class CalendarUiState(
  val year: Int,
  val month: Month,
  val isLoading: Boolean = true,
  val firstDayOffset: Int = 0,
  val days: List<CalendarDay> = emptyList(),
  val selectedDate: LocalDate? = null,
  val selectedCharges: List<CalendarCharge> = emptyList(),
  val monthTotal: BigDecimal = BigDecimal.ZERO,
  val currency: Currency = Currency.DEFAULT,
)
