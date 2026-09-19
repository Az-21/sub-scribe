package az21.subscribe.domain.export

import az21.subscribe.domain.model.AppSettings
import az21.subscribe.domain.model.Currency
import az21.subscribe.domain.model.PaymentMethod
import az21.subscribe.domain.model.PriceHistory
import az21.subscribe.domain.model.Subscription
import az21.subscribe.domain.model.Tag
import az21.subscribe.domain.model.ThemeMode
import az21.subscribe.domain.model.ThemeSeedSource
import az21.subscribe.domain.model.ThemeVariant

fun Subscription.toExport(): SubscriptionExport =
  SubscriptionExport(
    id = id.toString(),
    name = name,
    iconId = iconId,
    startDate = startDate.toString(),
    billingCycle = billingCycle.name,
    status = status.name,
    endDate = endDate?.toString(),
    reminderDaysBefore = reminderDaysBefore,
    paymentMethodId = paymentMethodId?.toString(),
    notes = notes,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
  )

fun PriceHistory.toExport(): PriceHistoryExport =
  PriceHistoryExport(
    id = id.toString(),
    subscriptionId = subscriptionId.toString(),
    price = price.toPlainString(),
    effectiveFromDate = effectiveFromDate.toString(),
    createdAt = createdAt.toString(),
  )

fun Tag.toExport(): TagExport = TagExport(id = id.toString(), name = name, color = color)

fun PaymentMethod.toExport(): PaymentMethodExport =
  PaymentMethodExport(id = id.toString(), label = label, color = color)

fun AppSettings.toExport(): SettingsExport =
  SettingsExport(
    currency = currency.code,
    themeSeedSource = themeSeedSource.name,
    themeSeedColor = themeSeedColor,
    themeVariant = themeVariant.name,
    themeMode = themeMode.name,
  )

fun ExportDocument.toImportSummary(): ImportSummary =
  ImportSummary(
    subscriptions = subscriptions.size,
    priceHistory = priceHistory.size,
    tags = tags.size,
    subscriptionTags = subscriptionTags.size,
    paymentMethods = paymentMethods.size,
    settingsApplied = true,
  )

fun SettingsExport.toDomain(): AppSettings =
  AppSettings(
    currency = Currency.fromCode(currency),
    themeSeedSource = themeSeedSource.toEnumOr(ThemeSeedSource.SYSTEM),
    themeSeedColor = themeSeedColor,
    themeVariant = themeVariant.toEnumOr(ThemeVariant.TONAL_SPOT),
    themeMode = themeMode.toEnumOr(ThemeMode.SYSTEM),
  )

private inline fun <reified T : Enum<T>> String.toEnumOr(default: T): T =
  enumValues<T>().firstOrNull { it.name == this } ?: default
