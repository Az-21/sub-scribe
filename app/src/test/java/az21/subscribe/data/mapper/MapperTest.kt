package az21.subscribe.data.mapper

import az21.subscribe.data.local.entity.SubscriptionEntity
import az21.subscribe.domain.model.BillingCycle
import az21.subscribe.domain.model.PaymentMethod
import az21.subscribe.domain.model.PriceHistory
import az21.subscribe.domain.model.Subscription
import az21.subscribe.domain.model.SubscriptionStatus
import az21.subscribe.domain.model.Tag
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class MapperTest {
  @Test
  fun subscription_entityAndDomain_roundTrip() {
    val subscription =
      Subscription(
        id = UUID.randomUUID(),
        name = "Spotify",
        iconId = "spotify",
        startDate = LocalDate.of(2022, 3, 1),
        billingCycle = BillingCycle.ANNUAL,
        freeTrialMonths = 1,
        status = SubscriptionStatus.ACTIVE,
        endDate = null,
        reminderDaysBefore = 3,
        trialReminderEnabled = true,
        paymentMethodId = UUID.randomUUID(),
        notes = "Family plan",
        createdAt = Instant.parse("2022-03-01T00:00:00Z"),
        updatedAt = Instant.parse("2022-04-01T00:00:00Z"),
      )

    assertEquals(subscription, subscription.toEntity().toDomain())
  }

  @Test
  fun priceHistory_mapsBothWays() {
    val priceHistory =
      PriceHistory(
        id = UUID.randomUUID(),
        subscriptionId = UUID.randomUUID(),
        price = BigDecimal("12.99"),
        effectiveFromDate = LocalDate.of(2024, 1, 1),
        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
      )

    assertEquals(priceHistory, priceHistory.toEntity().toDomain())
  }

  @Test
  fun tag_mapsBothWays() {
    val tag = Tag(id = UUID.randomUUID(), name = "Work", color = 0xFF00FF00.toInt())

    assertEquals(tag, tag.toEntity().toDomain())
  }

  @Test
  fun paymentMethod_mapsBothWays() {
    val paymentMethod = PaymentMethod(id = UUID.randomUUID(), label = "Visa ...1234")

    assertEquals(paymentMethod, paymentMethod.toEntity().toDomain())
  }

  @Test
  fun subscription_withNullOptionals_roundTrips() {
    val subscription =
      Subscription(
        id = UUID.randomUUID(),
        name = "Bare",
        iconId = "bare",
        startDate = LocalDate.of(2024, 1, 1),
        billingCycle = BillingCycle.MONTHLY,
        freeTrialMonths = null,
        status = SubscriptionStatus.ACTIVE,
        endDate = null,
        reminderDaysBefore = null,
        trialReminderEnabled = false,
        paymentMethodId = null,
        notes = null,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
      )

    val mapped = subscription.toEntity().toDomain()

    assertEquals(subscription, mapped)
    assertEquals(null, mapped.freeTrialMonths)
    assertEquals(null, mapped.endDate)
    assertEquals(null, mapped.reminderDaysBefore)
    assertEquals(null, mapped.paymentMethodId)
    assertEquals(null, mapped.notes)
  }

  @Test
  fun subscriptionEntity_exposesExpectedColumns() {
    val entity =
      SubscriptionEntity(
        id = UUID.randomUUID(),
        name = "iCloud",
        iconId = "icloud",
        startDate = LocalDate.of(2023, 1, 1),
        billingCycle = BillingCycle.MONTHLY,
        freeTrialMonths = null,
        status = SubscriptionStatus.CANCELLED,
        endDate = LocalDate.of(2023, 6, 1),
        reminderDaysBefore = null,
        trialReminderEnabled = false,
        paymentMethodId = null,
        notes = null,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
      )

    assertEquals(entity, entity.toDomain().toEntity())
  }
}
