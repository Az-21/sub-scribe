package az21.subscribe.data.export

import az21.subscribe.domain.export.EXPORT_SCHEMA_VERSION
import az21.subscribe.domain.export.ExportCodec
import az21.subscribe.domain.export.ExportDocument
import az21.subscribe.domain.export.ExportFormat
import az21.subscribe.domain.export.ImportIssue
import az21.subscribe.domain.export.ImportIssueReason
import az21.subscribe.domain.export.ImportParseResult
import az21.subscribe.domain.export.PaymentMethodExport
import az21.subscribe.domain.export.PriceHistoryExport
import az21.subscribe.domain.export.SettingsExport
import az21.subscribe.domain.export.SubscriptionExport
import az21.subscribe.domain.export.SubscriptionTagExport
import az21.subscribe.domain.export.TagExport
import az21.subscribe.domain.export.toImportSummary
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject

/**
 * Human-readable CSV export: a zip holding one table per entity plus a manifest and a settings
 * key/value sheet. Every table is a plain CSV that opens in any spreadsheet app.
 */
class CsvExportCodec
  @Inject
  constructor() : ExportCodec {
    override val format: ExportFormat = ExportFormat.CSV

    override fun encode(document: ExportDocument): ByteArray {
      val output = ByteArrayOutputStream()
      ZipOutputStream(output).use { zip ->
        zip.writeEntry(MANIFEST, keyValueSheet(manifest(document)))
        zip.writeEntry(SETTINGS, keyValueSheet(settings(document.settings)))
        zip.writeEntry(
          PAYMENT_METHODS,
          CsvTableCodec.encode(
            listOf("id", "label"),
            document.paymentMethods.map { entry -> listOf(entry.id, entry.label) },
          ),
        )
        zip.writeEntry(
          TAGS,
          CsvTableCodec.encode(
            listOf("id", "name", "color"),
            document.tags.map { entry -> listOf(entry.id, entry.name, entry.color?.toString().orEmpty()) },
          ),
        )
        zip
          .writeEntry(
            SUBSCRIPTIONS,
            CsvTableCodec.encode(SUBSCRIPTION_HEADERS, document.subscriptions.map(::subscriptionRow)),
          )
        zip.writeEntry(
          SUBSCRIPTION_TAGS,
          CsvTableCodec.encode(
            listOf("subscription_id", "tag_id"),
            document.subscriptionTags.map { entry -> listOf(entry.subscriptionId, entry.tagId) },
          ),
        )
        zip
          .writeEntry(
            PRICE_HISTORY,
            CsvTableCodec.encode(PRICE_HISTORY_HEADERS, document.priceHistory.map(::priceHistoryRow)),
          )
      }
      return output.toByteArray()
    }

    override fun canDecode(bytes: ByteArray): Boolean =
      bytes.size >= ZIP_MAGIC.size && ZIP_MAGIC.indices.all { index -> bytes[index] == ZIP_MAGIC[index] }

    override fun decode(bytes: ByteArray): ImportParseResult {
      val files = runCatching { readZip(bytes) }.getOrNull()
      val manifest = files?.get(MANIFEST)?.let(::keyValues)
      val version = manifest?.get("schema_version")?.toIntOrNull()
      return if (files == null || manifest == null || version == null) {
        malformed()
      } else {
        buildDocument(files, manifest, version)
      }
    }

    private fun buildDocument(
      files: Map<String, String>,
      manifest: Map<String, String>,
      version: Int,
    ): ImportParseResult {
      val issues = mutableListOf<ImportIssue>()
      val document =
        ExportDocument(
          schemaVersion = version,
          exportedAt = manifest["exported_at"].orEmpty(),
          subscriptions = decodeSubscriptions(files[SUBSCRIPTIONS], issues),
          priceHistory = decodePriceHistory(files[PRICE_HISTORY], issues),
          tags = decodeTags(files[TAGS], issues),
          subscriptionTags = decodeSubscriptionTags(files[SUBSCRIPTION_TAGS], issues),
          paymentMethods = decodePaymentMethods(files[PAYMENT_METHODS], issues),
          settings = decodeSettings(files[SETTINGS], issues),
        )
      return ImportParseResult.Success(document = document, summary = document.toImportSummary(), issues = issues)
    }

    private fun decodeSubscriptions(
      content: String?,
      issues: MutableList<ImportIssue>,
    ): List<SubscriptionExport> =
      table(content, SUBSCRIPTIONS, SUBSCRIPTION_HEADERS, issues).map { row ->
        SubscriptionExport(
          id = row.getValue("id"),
          name = row.getValue("name"),
          iconId = row.getValue("icon_id"),
          startDate = row.getValue("start_date"),
          billingCycle = row.getValue("billing_cycle"),
          freeTrialMonths = row.getValue("free_trial_months").toIntOrNull(),
          status = row.getValue("status"),
          endDate = row.getValue("end_date").ifBlank { null },
          reminderDaysBefore = row.getValue("reminder_days_before").toIntOrNull(),
          trialReminderEnabled = row.getValue("trial_reminder_enabled").toBooleanStrictOrNull() ?: false,
          paymentMethodId = row.getValue("payment_method_id").ifBlank { null },
          notes = row.getValue("notes").ifBlank { null },
          createdAt = row.getValue("created_at"),
          updatedAt = row.getValue("updated_at"),
        )
      }

    private fun decodePriceHistory(
      content: String?,
      issues: MutableList<ImportIssue>,
    ): List<PriceHistoryExport> =
      table(content, PRICE_HISTORY, PRICE_HISTORY_HEADERS, issues).map { row ->
        PriceHistoryExport(
          id = row.getValue("id"),
          subscriptionId = row.getValue("subscription_id"),
          price = row.getValue("price"),
          effectiveFromDate = row.getValue("effective_from_date"),
          createdAt = row.getValue("created_at"),
        )
      }

    private fun decodeTags(
      content: String?,
      issues: MutableList<ImportIssue>,
    ): List<TagExport> =
      table(content, TAGS, listOf("id", "name", "color"), issues).map { row ->
        TagExport(
          id = row.getValue("id"),
          name = row.getValue("name"),
          color = row.getValue("color").toIntOrNull(),
        )
      }

    private fun decodePaymentMethods(
      content: String?,
      issues: MutableList<ImportIssue>,
    ): List<PaymentMethodExport> =
      table(content, PAYMENT_METHODS, listOf("id", "label"), issues).map { row ->
        PaymentMethodExport(id = row.getValue("id"), label = row.getValue("label"))
      }

    private fun decodeSubscriptionTags(
      content: String?,
      issues: MutableList<ImportIssue>,
    ): List<SubscriptionTagExport> =
      table(content, SUBSCRIPTION_TAGS, listOf("subscription_id", "tag_id"), issues).map { row ->
        SubscriptionTagExport(subscriptionId = row.getValue("subscription_id"), tagId = row.getValue("tag_id"))
      }

    private fun decodeSettings(
      content: String?,
      issues: MutableList<ImportIssue>,
    ): SettingsExport {
      val values = keyValues(content)
      if (values == null) {
        issues += ImportIssue(ImportIssueReason.MISSING_FILE, SETTINGS)
        return SettingsExport()
      }
      return SettingsExport(
        currency = values["currency"].orEmpty(),
        themeSeedSource = values["theme_seed_source"].orEmpty(),
        themeSeedColor = values["theme_seed_color"]?.toIntOrNull(),
        themeVariant = values["theme_variant"].orEmpty(),
        themeMode = values["theme_mode"].orEmpty(),
      )
    }

    private fun table(
      content: String?,
      fileName: String,
      requiredHeaders: List<String>,
      issues: MutableList<ImportIssue>,
    ): List<Map<String, String>> {
      if (content == null) {
        issues += ImportIssue(ImportIssueReason.MISSING_FILE, fileName)
        return emptyList()
      }
      val rows = CsvTableCodec.decode(content)
      val headers = rows.firstOrNull()
      val missing = headers?.let { header -> requiredHeaders.filterNot(header::contains) }.orEmpty()
      if (missing.isNotEmpty()) {
        issues += ImportIssue(ImportIssueReason.MALFORMED_FILE, "$fileName: ${missing.joinToString()}")
      }
      return if (headers == null || missing.isNotEmpty()) {
        emptyList()
      } else {
        rows
          .drop(1)
          .filter { row -> row.any(String::isNotBlank) }
          .map { row -> headers.mapIndexed { index, header -> header to row.getOrElse(index) { "" } }.toMap() }
      }
    }

    private fun keyValues(content: String?): Map<String, String>? {
      if (content == null) return null
      return CsvTableCodec
        .decode(content)
        .drop(1)
        .filter { row -> row.size >= 2 && row.first().isNotBlank() }
        .associate { row -> row[0] to row[1] }
    }

    private fun malformed(): ImportParseResult.Failure =
      ImportParseResult.Failure(listOf(ImportIssue(ImportIssueReason.MALFORMED_FILE, "")))

    private fun readZip(bytes: ByteArray): Map<String, String> {
      val files = mutableMapOf<String, String>()
      ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
        var entry: ZipEntry? = zip.nextEntry
        while (entry != null) {
          if (!entry.isDirectory) files[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
          entry = zip.nextEntry
        }
      }
      return files
    }

    private fun ZipOutputStream.writeEntry(
      name: String,
      content: String,
    ) {
      putNextEntry(ZipEntry(name))
      write(content.toByteArray(Charsets.UTF_8))
      closeEntry()
    }

    private companion object {
      val ZIP_MAGIC = byteArrayOf(0x50, 0x4B)
      const val MANIFEST = "manifest.csv"
      const val SETTINGS = "settings.csv"
      const val PAYMENT_METHODS = "payment_methods.csv"
      const val TAGS = "tags.csv"
      const val SUBSCRIPTIONS = "subscriptions.csv"
      const val SUBSCRIPTION_TAGS = "subscription_tags.csv"
      const val PRICE_HISTORY = "price_history.csv"

      val SUBSCRIPTION_HEADERS =
        listOf(
          "id",
          "name",
          "icon_id",
          "start_date",
          "billing_cycle",
          "free_trial_months",
          "status",
          "end_date",
          "reminder_days_before",
          "trial_reminder_enabled",
          "payment_method_id",
          "notes",
          "created_at",
          "updated_at",
        )
      val PRICE_HISTORY_HEADERS = listOf("id", "subscription_id", "price", "effective_from_date", "created_at")

      fun manifest(document: ExportDocument): Map<String, String> =
        linkedMapOf(
          "schema_version" to EXPORT_SCHEMA_VERSION.toString(),
          "exported_at" to document.exportedAt,
        )

      fun settings(settings: SettingsExport): Map<String, String> =
        linkedMapOf(
          "currency" to settings.currency,
          "theme_seed_source" to settings.themeSeedSource,
          "theme_seed_color" to settings.themeSeedColor?.toString().orEmpty(),
          "theme_variant" to settings.themeVariant,
          "theme_mode" to settings.themeMode,
        )

      fun keyValueSheet(values: Map<String, String>): String =
        CsvTableCodec.encode(listOf("key", "value"), values.map { (key, value) -> listOf(key, value) })

      fun subscriptionRow(entry: SubscriptionExport): List<String> =
        with(entry) {
          listOf(
            id,
            name,
            iconId,
            startDate,
            billingCycle,
            freeTrialMonths?.toString().orEmpty(),
            status,
            endDate.orEmpty(),
            reminderDaysBefore?.toString().orEmpty(),
            trialReminderEnabled.toString(),
            paymentMethodId.orEmpty(),
            notes.orEmpty(),
            createdAt,
            updatedAt,
          )
        }

      fun priceHistoryRow(entry: PriceHistoryExport): List<String> =
        with(entry) { listOf(id, subscriptionId, price, effectiveFromDate, createdAt) }
    }
  }
