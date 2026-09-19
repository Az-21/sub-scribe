package az21.subscribe.domain.export

import az21.subscribe.domain.model.BillingCycle
import az21.subscribe.domain.model.SubscriptionStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** The result of validating a decoded export: the cleaned document plus any problems found. */
data class ValidatedExport(
  val document: ExportDocument,
  val issues: List<ImportIssue>,
)

/**
 * Parses the string fields of a decoded [ExportDocument] and drops anything that cannot be read.
 * Referential integrity is repaired rather than rejected: a subscription pointing at a missing
 * payment method is kept with the link cleared, and price or tag rows pointing at dropped records
 * are removed. This lets an import keep as much good data as possible without failing wholesale.
 */
object ExportValidator {
  fun validate(document: ExportDocument): ValidatedExport {
    val issues = mutableListOf<ImportIssue>()

    val paymentMethods =
      document.paymentMethods.mapIndexedNotNull { index, entry ->
        parsePaymentMethod(entry, "payment_methods[$index]", issues)
      }
    val paymentMethodIds = paymentMethods.map { it.id }.toSet()

    val tags = document.tags.mapIndexedNotNull { index, entry -> parseTag(entry, "tags[$index]", issues) }
    val tagIds = tags.map { it.id }.toSet()

    val subscriptions =
      document.subscriptions
        .mapIndexedNotNull { index, entry -> parseSubscription(entry, "subscriptions[$index]", issues) }
        .associateBy { subscription -> subscription.id }
        .values
        .map { subscription -> clearMissingPaymentMethod(subscription, paymentMethodIds, issues) }
    val subscriptionIds = subscriptions.map { it.id }.toSet()

    val priceHistory =
      document.priceHistory
        .mapIndexedNotNull { index, entry ->
          parsePriceHistory(entry, "price_history[$index]", issues)
        }.filter { entry ->
          val keep = entry.subscriptionId in subscriptionIds
          if (!keep) record(issues, "price_history[${entry.id}]")
          keep
        }

    val subscriptionTags =
      document.subscriptionTags
        .filter { link -> link.subscriptionId in subscriptionIds && link.tagId in tagIds }
        .distinctBy { link -> link.subscriptionId to link.tagId }

    return ValidatedExport(
      document =
        document.copy(
          subscriptions = subscriptions,
          priceHistory = priceHistory,
          tags = tags,
          subscriptionTags = subscriptionTags,
          paymentMethods = paymentMethods,
        ),
      issues = issues,
    )
  }

  private fun parseSubscription(
    entry: SubscriptionExport,
    location: String,
    issues: MutableList<ImportIssue>,
  ): SubscriptionExport? {
    val valid =
      entry.id.isUuid() &&
        entry.name.isNotBlank() &&
        entry.startDate.isDate() &&
        entry.billingCycle.isEnum<BillingCycle>() &&
        entry.status.isEnum<SubscriptionStatus>() &&
        entry.endDate.isDateOrNull() &&
        entry.reminders.all { reminder -> reminder.daysBefore >= 0 && reminder.time.isTime() } &&
        entry.createdAt.isInstant() &&
        entry.updatedAt.isInstant() &&
        entry.paymentMethodId.isUuidOrNull()
    if (!valid) {
      record(issues, location)
      return null
    }
    return entry
  }

  private fun parsePriceHistory(
    entry: PriceHistoryExport,
    location: String,
    issues: MutableList<ImportIssue>,
  ): PriceHistoryExport? {
    val price = entry.price.toBigDecimalOrNull()
    val valid =
      entry.id.isUuid() &&
        entry.subscriptionId.isUuid() &&
        price != null &&
        price.signum() >= 0 &&
        entry.effectiveFromDate.isDate() &&
        entry.createdAt.isInstant()
    if (!valid) {
      record(issues, location)
      return null
    }
    return entry.copy(price = price.toPlainString())
  }

  private fun parseTag(
    entry: TagExport,
    location: String,
    issues: MutableList<ImportIssue>,
  ): TagExport? {
    if (!entry.id.isUuid() || entry.name.isBlank()) {
      record(issues, location)
      return null
    }
    return entry
  }

  private fun parsePaymentMethod(
    entry: PaymentMethodExport,
    location: String,
    issues: MutableList<ImportIssue>,
  ): PaymentMethodExport? {
    if (!entry.id.isUuid() || entry.label.isBlank()) {
      record(issues, location)
      return null
    }
    return entry
  }

  private fun clearMissingPaymentMethod(
    entry: SubscriptionExport,
    paymentMethodIds: Set<String>,
    issues: MutableList<ImportIssue>,
  ): SubscriptionExport {
    val paymentMethodId = entry.paymentMethodId
    val missing = paymentMethodId != null && paymentMethodId !in paymentMethodIds
    if (missing) {
      issues += ImportIssue(ImportIssueReason.REFERENCE_CLEARED, "subscriptions[${entry.id}]")
    }
    return if (missing) entry.copy(paymentMethodId = null) else entry
  }

  private fun record(
    issues: MutableList<ImportIssue>,
    location: String,
  ) {
    issues += ImportIssue(ImportIssueReason.ROW_SKIPPED, location)
  }
}

private fun String.isUuid(): Boolean = runCatching { UUID.fromString(this) }.isSuccess

private fun String?.isUuidOrNull(): Boolean = this == null || isUuid()

private fun String.isDate(): Boolean = runCatching { LocalDate.parse(this) }.isSuccess

private fun String?.isDateOrNull(): Boolean = this == null || isDate()

private fun String.isInstant(): Boolean = runCatching { Instant.parse(this) }.isSuccess

private fun String.isTime(): Boolean = runCatching { java.time.LocalTime.parse(this) }.isSuccess

private inline fun <reified T : Enum<T>> String.isEnum(): Boolean = enumValues<T>().any { it.name == this }

private fun String.toBigDecimalOrNull(): BigDecimal? = runCatching { BigDecimal(this) }.getOrNull()
