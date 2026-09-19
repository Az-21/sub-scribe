package az21.subscribe.domain.export

import az21.subscribe.domain.model.AppSettings
import az21.subscribe.domain.model.BillingCycle
import az21.subscribe.domain.model.Currency
import az21.subscribe.domain.model.PaymentMethod
import az21.subscribe.domain.model.PriceHistory
import az21.subscribe.domain.model.Subscription
import az21.subscribe.domain.model.SubscriptionStatus
import az21.subscribe.domain.model.Tag
import az21.subscribe.domain.model.ThemeMode
import az21.subscribe.domain.model.ThemeSeedSource
import az21.subscribe.domain.model.ThemeVariant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** Pure mapping behavior between domain models and their export representations. */
class ExportMappersTest {
  @Test
  fun settingsToDomain_mapsKnownValues() {
    val export =
      SettingsExport(
        currency = "eur",
        themeSeedSource = "MANUAL",
        themeSeedColor = 0x112233,
        themeVariant = "EXPRESSIVE",
        themeMode = "DARK",
      )

    assertEquals(
      AppSettings(
        currency = Currency.EUR,
        themeSeedSource = ThemeSeedSource.MANUAL,
        themeSeedColor = 0x112233,
        themeVariant = ThemeVariant.EXPRESSIVE,
        themeMode = ThemeMode.DARK,
      ),
      export.toDomain(),
    )
  }

  @Test
  fun settingsToDomain_fallsBackOnUnknownOrBlankValues() {
    val export =
      SettingsExport(
        currency = "XYZ",
        themeSeedSource = "",
        themeSeedColor = null,
        themeVariant = "not-a-variant",
        themeMode = "",
      )

    val settings = export.toDomain()

    assertEquals(Currency.DEFAULT, settings.currency)
    assertEquals(ThemeSeedSource.SYSTEM, settings.themeSeedSource)
    assertNull(settings.themeSeedColor)
    assertEquals(ThemeVariant.TONAL_SPOT, settings.themeVariant)
    assertEquals(ThemeMode.SYSTEM, settings.themeMode)
  }

  @Test
  fun settingsRoundTrip_throughExport() {
    val settings =
      AppSettings(
        currency = Currency.GBP,
        themeSeedSource = ThemeSeedSource.MANUAL,
        themeSeedColor = 0x445566,
        themeVariant = ThemeVariant.VIBRANT,
        themeMode = ThemeMode.LIGHT,
      )

    assertEquals(settings, settings.toExport().toDomain())
  }

  @Test
  fun toImportSummary_countsEveryList() {
    val document =
      ExportDocument(
        subscriptions = listOf(subscriptionExport("1"), subscriptionExport("2")),
        priceHistory = listOf(priceExport()),
        tags = listOf(TagExport(id = "t", name = "Tag")),
        subscriptionTags = listOf(SubscriptionTagExport("1", "t")),
        paymentMethods = listOf(PaymentMethodExport(id = "p", label = "Visa")),
      )

    val summary = document.toImportSummary()

    assertEquals(2, summary.subscriptions)
    assertEquals(1, summary.priceHistory)
    assertEquals(1, summary.tags)
    assertEquals(1, summary.subscriptionTags)
    assertEquals(1, summary.paymentMethods)
  }

  @Test
  fun subscriptionToExport_mapsNullableFieldsToNull() {
    val subscription =
      Subscription(
        id = UUID.fromString("11111111-1111-1111-1111-111111111111"),
        name = "Test",
        iconId = "test",
        startDate = LocalDate.of(2024, 1, 1),
        billingCycle = BillingCycle.MONTHLY,
        status = SubscriptionStatus.ACTIVE,
        endDate = null,
        paymentMethodId = null,
        notes = null,
        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2024-01-01T00:00:00Z"),
      )

    val export = subscription.toExport()

    assertNull(export.endDate)
    assertEquals(emptyList<ReminderExport>(), export.reminders)
    assertNull(export.paymentMethodId)
    assertNull(export.notes)
    assertEquals("MONTHLY", export.billingCycle)
    assertEquals("ACTIVE", export.status)
  }

  @Test
  fun subscriptionToExport_mapsIconColor() {
    val subscription =
      Subscription(
        id = UUID.fromString("11111111-1111-1111-1111-111111111111"),
        name = "Test",
        iconId = "simple:netflix",
        startDate = LocalDate.of(2024, 1, 1),
        billingCycle = BillingCycle.MONTHLY,
        status = SubscriptionStatus.ACTIVE,
        endDate = null,
        paymentMethodId = null,
        notes = null,
        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2024-01-01T00:00:00Z"),
        iconColor = 0xFFE57373.toInt(),
      )

    val export = subscription.toExport()

    assertEquals("simple:netflix", export.iconId)
    assertEquals(0xFFE57373.toInt(), export.iconColor)
  }

  @Test
  fun priceHistoryToExport_usesPlainString() {
    val history =
      PriceHistory(
        id = UUID.randomUUID(),
        subscriptionId = UUID.randomUUID(),
        price = BigDecimal("0.0000001"),
        effectiveFromDate = LocalDate.of(2024, 1, 1),
        createdAt = Instant.EPOCH,
      )

    assertEquals("0.0000001", history.toExport().price)
  }

  @Test
  fun tagAndPaymentMethodToExport_mapFields() {
    val tag = Tag(id = UUID.fromString("33333333-3333-3333-3333-333333333333"), name = "Work", color = 0x00FF00)
    val paymentMethod =
      PaymentMethod(
        id = UUID.fromString("44444444-4444-4444-4444-444444444444"),
        label = "PayPal",
        color = 0x0000FF,
      )

    assertEquals("33333333-3333-3333-3333-333333333333", tag.toExport().id)
    assertEquals(0x00FF00, tag.toExport().color)
    assertEquals("PayPal", paymentMethod.toExport().label)
    assertEquals(0x0000FF, paymentMethod.toExport().color)
  }

  private fun subscriptionExport(id: String): SubscriptionExport =
    SubscriptionExport(
      id = id,
      name = "Test",
      iconId = "test",
      startDate = "2025-01-01",
      billingCycle = "MONTHLY",
      status = "ACTIVE",
      createdAt = "2025-01-01T00:00:00Z",
      updatedAt = "2025-01-01T00:00:00Z",
    )

  private fun priceExport(): PriceHistoryExport =
    PriceHistoryExport(
      id = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
      subscriptionId = "1",
      price = "9.99",
      effectiveFromDate = "2025-01-01",
      createdAt = "2025-01-01T00:00:00Z",
    )
}
