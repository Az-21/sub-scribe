package az21.subscribe.ui.subscription

import java.time.LocalDate

/** Reasons a price point's effective date deserves a warning while editing. */
enum class PriceDateWarning {
  /** The price takes effect before the subscription's start date. */
  BEFORE_START_DATE,

  /** The price takes effect after the subscription's end date. */
  AFTER_END_DATE,

  /** More than one price change shares this effective date. */
  MULTIPLE_IN_ONE_DAY,

  /** The price takes effect after today, so it is not applied yet. */
  IN_FUTURE,

  /**
   * This is the earliest price and it starts after the subscription's start date, so billings
   * between the start date and this price have no value.
   */
  UNCOVERED_FROM_START,
}

/**
 * Returns every reason [effectiveFromDate] is flagged: relative to the subscription's [startDate]
 * and [endDate], to [today], and against the [allEffectiveDates] of the whole timeline. Purely
 * advisory: callers warn the user, they never remove the entry.
 */
fun priceDateWarnings(
  effectiveFromDate: LocalDate,
  startDate: LocalDate?,
  endDate: LocalDate?,
  today: LocalDate,
  allEffectiveDates: List<LocalDate>,
): List<PriceDateWarning> =
  buildList {
    if (startDate != null && effectiveFromDate.isBefore(startDate)) {
      add(PriceDateWarning.BEFORE_START_DATE)
    }
    if (endDate != null && effectiveFromDate.isAfter(endDate)) {
      add(PriceDateWarning.AFTER_END_DATE)
    }
    if (allEffectiveDates.count { date -> date == effectiveFromDate } > 1) {
      add(PriceDateWarning.MULTIPLE_IN_ONE_DAY)
    }
    if (effectiveFromDate.isAfter(today)) {
      add(PriceDateWarning.IN_FUTURE)
    }
    if (
      startDate != null &&
      effectiveFromDate.isAfter(startDate) &&
      effectiveFromDate == allEffectiveDates.minOrNull()
    ) {
      add(PriceDateWarning.UNCOVERED_FROM_START)
    }
  }
