package az21.subscribe.domain.model

import java.time.LocalTime

/**
 * A billing reminder rule: fire [daysBefore] days before a charge, at [time] local time. A
 * subscription can carry any number of these.
 */
data class ReminderSpec(
  val daysBefore: Int,
  val time: LocalTime,
)
