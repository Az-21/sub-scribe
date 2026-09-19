package az21.subscribe.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/** A subscription row together with its reminder rules. */
data class SubscriptionWithReminders(
  @Embedded val subscription: SubscriptionEntity,
  @Relation(
    parentColumn = "id",
    entityColumn = "subscription_id",
  )
  val reminders: List<SubscriptionReminderEntity>,
)
