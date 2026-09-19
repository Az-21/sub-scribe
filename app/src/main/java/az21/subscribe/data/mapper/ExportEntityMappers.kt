package az21.subscribe.data.mapper

import az21.subscribe.data.local.entity.PaymentMethodEntity
import az21.subscribe.data.local.entity.PriceHistoryEntity
import az21.subscribe.data.local.entity.SubscriptionEntity
import az21.subscribe.data.local.entity.SubscriptionReminderEntity
import az21.subscribe.data.local.entity.SubscriptionWithReminders
import az21.subscribe.data.local.entity.TagEntity
import az21.subscribe.domain.export.PaymentMethodExport
import az21.subscribe.domain.export.PriceHistoryExport
import az21.subscribe.domain.export.ReminderExport
import az21.subscribe.domain.export.SubscriptionExport
import az21.subscribe.domain.export.TagExport
import az21.subscribe.domain.model.BillingCycle
import az21.subscribe.domain.model.SubscriptionStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

fun SubscriptionExport.toEntity(): SubscriptionEntity =
  SubscriptionEntity(
    id = UUID.fromString(id),
    name = name,
    iconId = iconId,
    startDate = LocalDate.parse(startDate),
    billingCycle = BillingCycle.valueOf(billingCycle),
    status = SubscriptionStatus.valueOf(status),
    endDate = endDate?.let(LocalDate::parse),
    paymentMethodId = paymentMethodId?.let(UUID::fromString),
    notes = notes,
    createdAt = Instant.parse(createdAt),
    updatedAt = Instant.parse(updatedAt),
    iconColor = iconColor,
  )

fun ReminderExport.toEntity(
  subscriptionId: UUID,
  id: UUID,
): SubscriptionReminderEntity =
  SubscriptionReminderEntity(
    id = id,
    subscriptionId = subscriptionId,
    daysBefore = daysBefore,
    time = LocalTime.parse(time),
  )

fun SubscriptionExport.toEntityWithReminders(): SubscriptionWithReminders {
  val subscriptionId = UUID.fromString(id)
  return SubscriptionWithReminders(
    subscription = toEntity(),
    reminders = reminders.map { reminder -> reminder.toEntity(subscriptionId, UUID.randomUUID()) },
  )
}

fun PriceHistoryExport.toEntity(): PriceHistoryEntity =
  PriceHistoryEntity(
    id = UUID.fromString(id),
    subscriptionId = UUID.fromString(subscriptionId),
    price = BigDecimal(price),
    effectiveFromDate = LocalDate.parse(effectiveFromDate),
    createdAt = Instant.parse(createdAt),
  )

fun TagExport.toEntity(): TagEntity = TagEntity(id = UUID.fromString(id), name = name, color = color)

fun PaymentMethodExport.toEntity(): PaymentMethodEntity =
  PaymentMethodEntity(id = UUID.fromString(id), label = label, color = color)
