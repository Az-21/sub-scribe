package az21.subscribe.data.mapper

import az21.subscribe.data.local.entity.SubscriptionEntity
import az21.subscribe.domain.model.Subscription

fun SubscriptionEntity.toDomain(): Subscription =
  Subscription(
    id = id,
    name = name,
    iconId = iconId,
    startDate = startDate,
    billingCycle = billingCycle,
    status = status,
    endDate = endDate,
    reminderDaysBefore = reminderDaysBefore,
    paymentMethodId = paymentMethodId,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

fun Subscription.toEntity(): SubscriptionEntity =
  SubscriptionEntity(
    id = id,
    name = name,
    iconId = iconId,
    startDate = startDate,
    billingCycle = billingCycle,
    status = status,
    endDate = endDate,
    reminderDaysBefore = reminderDaysBefore,
    paymentMethodId = paymentMethodId,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )
