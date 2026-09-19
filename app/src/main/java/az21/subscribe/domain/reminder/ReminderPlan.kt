package az21.subscribe.domain.reminder

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * A single reminder that should fire at [triggerAt] and mentions [targetDate].
 *
 * [targetDate] is the charge date for a billing reminder or the trial end date for a trial reminder.
 */
data class ReminderPlan(
  val type: ReminderType,
  val triggerAt: Instant,
  val targetDate: LocalDate,
) {
  companion object {
    /** Reminders land at 09:00 local time so they arrive before the day gets going. */
    const val NOTIFICATION_HOUR: Int = 9

    /** The instant [daysBefore] days before [date], at [NOTIFICATION_HOUR] in [zone]. */
    fun triggerAt(
      date: LocalDate,
      daysBefore: Int,
      zone: ZoneId,
    ): Instant =
      date
        .minusDays(daysBefore.toLong())
        .atTime(NOTIFICATION_HOUR, 0)
        .atZone(zone)
        .toInstant()
  }
}
