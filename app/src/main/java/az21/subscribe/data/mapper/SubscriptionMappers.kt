package az21.subscribe.data.mapper

import az21.subscribe.data.local.entity.SubscriptionEntity
import az21.subscribe.data.local.entity.SubscriptionReminderEntity
import az21.subscribe.data.local.entity.SubscriptionWithReminders
import az21.subscribe.domain.model.ReminderSpec
import az21.subscribe.domain.model.Subscription
import java.util.UUID

fun SubscriptionWithReminders.toDomain(): Subscription =
  Subscription(
    id = subscription.id,
    name = subscription.name,
    iconId = subscription.iconId,
    startDate = subscription.startDate,
    billingCycle = subscription.billingCycle,
    status = subscription.status,
    endDate = subscription.endDate,
    reminders =
      reminders
        .map(SubscriptionReminderEntity::toDomain)
        .sortedWith(compareBy({ it.daysBefore }, { it.time })),
    paymentMethodId = subscription.paymentMethodId,
    notes = subscription.notes,
    createdAt = subscription.createdAt,
    updatedAt = subscription.updatedAt,
    iconColor = subscription.iconColor,
  )

fun SubscriptionEntity.toDomain(): Subscription =
  Subscription(
    id = id,
    name = name,
    iconId = iconId,
    startDate = startDate,
    billingCycle = billingCycle,
    status = status,
    endDate = endDate,
    reminders = emptyList(),
    paymentMethodId = paymentMethodId,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    iconColor = iconColor,
  )

fun SubscriptionReminderEntity.toDomain(): ReminderSpec = ReminderSpec(daysBefore = daysBefore, time = time)

fun Subscription.toEntity(): SubscriptionEntity =
  SubscriptionEntity(
    id = id,
    name = name,
    iconId = iconId,
    startDate = startDate,
    billingCycle = billingCycle,
    status = status,
    endDate = endDate,
    paymentMethodId = paymentMethodId,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    iconColor = iconColor,
  )

fun ReminderSpec.toEntity(
  subscriptionId: UUID,
  id: UUID,
): SubscriptionReminderEntity =
  SubscriptionReminderEntity(
    id = id,
    subscriptionId = subscriptionId,
    daysBefore = daysBefore,
    time = time,
  )
