package az21.subscribe.domain.reminder

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * A single reminder that should fire at [triggerAt] for the charge on [targetDate]. [daysBefore] and
 * [time] are the rule that produced it.
 */
data class ReminderPlan(
  val triggerAt: Instant,
  val targetDate: LocalDate,
  val daysBefore: Int,
  val time: LocalTime,
) {
  companion object {
    /** The instant [daysBefore] days before [date], at [time] in [zone]. */
    fun triggerAt(
      date: LocalDate,
      daysBefore: Int,
      time: LocalTime,
      zone: ZoneId,
    ): Instant =
      date
        .minusDays(daysBefore.toLong())
        .atTime(time)
        .atZone(zone)
        .toInstant()
  }
}
