package az21.subscribe.ui.subscription

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PriceDateWarningTest {
  private val today = LocalDate.of(2024, 5, 20)
  private val startDate = LocalDate.of(2024, 1, 1)
  private val endDate = LocalDate.of(2024, 12, 31)

  @Test
  fun noWarnings_whenDateIsWithinRangeAndNotInFuture() {
    assertEquals(emptyList<PriceDateWarning>(), priceDateWarnings(startDate, startDate, endDate, today, emptyList()))
    assertEquals(
      emptyList<PriceDateWarning>(),
      priceDateWarnings(startDate, startDate, endDate, today, listOf(startDate)),
    )
  }

  @Test
  fun flagsBeforeStartDate() {
    val warnings = priceDateWarnings(LocalDate.of(2023, 12, 31), startDate, endDate, today, emptyList())

    assertEquals(listOf(PriceDateWarning.BEFORE_START_DATE), warnings)
  }

  @Test
  fun flagsAfterEndDate() {
    val endedEarly = LocalDate.of(2024, 4, 30)
    val warnings = priceDateWarnings(LocalDate.of(2024, 5, 1), startDate, endedEarly, today, emptyList())

    assertEquals(listOf(PriceDateWarning.AFTER_END_DATE), warnings)
  }

  @Test
  fun flagsMultipleInOneDay() {
    val date = startDate
    val warnings = priceDateWarnings(date, startDate, endDate, today, listOf(date, date))

    assertEquals(listOf(PriceDateWarning.MULTIPLE_IN_ONE_DAY), warnings)
  }

  @Test
  fun flagsFutureDate() {
    val warnings = priceDateWarnings(LocalDate.of(2024, 6, 1), startDate, null, today, emptyList())

    assertEquals(listOf(PriceDateWarning.IN_FUTURE), warnings)
  }

  @Test
  fun flagsMultipleReasonsOnOneEntry() {
    val date = LocalDate.of(2024, 6, 1)
    val futureStart = LocalDate.of(2024, 7, 1)

    val warnings = priceDateWarnings(date, futureStart, null, today, listOf(date, date))

    assertTrue(PriceDateWarning.BEFORE_START_DATE in warnings)
    assertTrue(PriceDateWarning.MULTIPLE_IN_ONE_DAY in warnings)
    assertTrue(PriceDateWarning.IN_FUTURE in warnings)
  }

  @Test
  fun doesNotFlagBeforeStart_whenStartDateIsUnknown() {
    assertEquals(
      emptyList<PriceDateWarning>(),
      priceDateWarnings(LocalDate.of(2024, 5, 1), null, endDate, today, emptyList()),
    )
  }

  @Test
  fun doesNotFlagAfterEnd_whenEndDateIsUnknown() {
    assertEquals(emptyList<PriceDateWarning>(), priceDateWarnings(today, startDate, null, today, emptyList()))
  }

  @Test
  fun flagsUncoveredStart_forGapBetweenStartAndFirstPrice() {
    val start = LocalDate.of(2025, 9, 1)
    val earliest = LocalDate.of(2025, 11, 1)
    val laterToday = LocalDate.of(2025, 12, 1)

    val warnings = priceDateWarnings(earliest, start, null, laterToday, listOf(earliest))

    assertEquals(listOf(PriceDateWarning.UNCOVERED_FROM_START), warnings)
  }

  @Test
  fun doesNotFlagUncoveredStart_whenFirstPriceIsAtStartDate() {
    assertEquals(
      emptyList<PriceDateWarning>(),
      priceDateWarnings(startDate, startDate, endDate, today, listOf(startDate)),
    )
  }

  @Test
  fun doesNotFlagUncoveredStart_forLaterEntries() {
    val first = LocalDate.of(2024, 1, 1)
    val second = LocalDate.of(2024, 3, 1)

    assertEquals(
      emptyList<PriceDateWarning>(),
      priceDateWarnings(second, first, null, today, listOf(first, second)),
    )
  }

  @Test
  fun doesNotFlagUncoveredStart_whenStartDateIsUnknown() {
    val earliest = LocalDate.of(2024, 3, 1)

    assertEquals(
      emptyList<PriceDateWarning>(),
      priceDateWarnings(earliest, null, null, today, listOf(earliest)),
    )
  }
}
