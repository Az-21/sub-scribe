package az21.subscribe.domain.export

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Version of the export document format. Bump when the shape of [ExportDocument] changes in a way
 * that older importers cannot read; importers reject versions they do not understand.
 */
const val EXPORT_SCHEMA_VERSION: Int = 1

/**
 * Full-fidelity snapshot of everything a user owns: subscriptions, their price timelines, tags and
 * tag assignments, payment methods, and app settings. All ids and dates are stored as strings so the
 * schema is stable and language-agnostic.
 */
@Serializable
data class ExportDocument(
  @SerialName("schema_version") val schemaVersion: Int = EXPORT_SCHEMA_VERSION,
  @SerialName("exported_at") val exportedAt: String = "",
  val subscriptions: List<SubscriptionExport> = emptyList(),
  @SerialName("price_history") val priceHistory: List<PriceHistoryExport> = emptyList(),
  val tags: List<TagExport> = emptyList(),
  @SerialName("subscription_tags") val subscriptionTags: List<SubscriptionTagExport> = emptyList(),
  @SerialName("payment_methods") val paymentMethods: List<PaymentMethodExport> = emptyList(),
  val settings: SettingsExport = SettingsExport(),
)

@Serializable
data class SubscriptionExport(
  val id: String,
  val name: String,
  @SerialName("icon_id") val iconId: String,
  @SerialName("start_date") val startDate: String,
  @SerialName("billing_cycle") val billingCycle: String,
  val status: String,
  @SerialName("end_date") val endDate: String? = null,
  @SerialName("reminder_days_before") val reminderDaysBefore: Int? = null,
  @SerialName("payment_method_id") val paymentMethodId: String? = null,
  val notes: String? = null,
  @SerialName("created_at") val createdAt: String,
  @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class PriceHistoryExport(
  val id: String,
  @SerialName("subscription_id") val subscriptionId: String,
  val price: String,
  @SerialName("effective_from_date") val effectiveFromDate: String,
  @SerialName("created_at") val createdAt: String,
)

@Serializable
data class TagExport(
  val id: String,
  val name: String,
  val color: Int? = null,
)

@Serializable
data class SubscriptionTagExport(
  @SerialName("subscription_id") val subscriptionId: String,
  @SerialName("tag_id") val tagId: String,
)

@Serializable
data class PaymentMethodExport(
  val id: String,
  val label: String,
  val color: Int? = null,
)

@Serializable
data class SettingsExport(
  val currency: String = "",
  @SerialName("theme_seed_source") val themeSeedSource: String = "",
  @SerialName("theme_seed_color") val themeSeedColor: Int? = null,
  @SerialName("theme_variant") val themeVariant: String = "",
  @SerialName("theme_mode") val themeMode: String = "",
)
