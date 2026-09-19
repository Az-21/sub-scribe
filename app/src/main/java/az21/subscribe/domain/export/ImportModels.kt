package az21.subscribe.domain.export

/** Reason an [ImportIssue] was recorded, resolved to user-facing text in the UI layer. */
enum class ImportIssueReason {
  UNSUPPORTED_VERSION,
  MALFORMED_FILE,
  MISSING_FILE,
  ROW_SKIPPED,
  REFERENCE_CLEARED,
}

/** A non-fatal problem encountered while reading an import file. */
data class ImportIssue(
  val reason: ImportIssueReason,
  val location: String,
)

/** How many records a successful import wrote to the database. */
data class ImportSummary(
  val subscriptions: Int = 0,
  val priceHistory: Int = 0,
  val tags: Int = 0,
  val subscriptionTags: Int = 0,
  val paymentMethods: Int = 0,
  val settingsApplied: Boolean = false,
)

/** What an import will do, shown to the user before it is committed. */
data class ImportPreview(
  val summary: ImportSummary,
  val issues: List<ImportIssue> = emptyList(),
)

/** Outcome of parsing an import file. */
sealed interface ImportParseResult {
  data class Success(
    val document: ExportDocument,
    val summary: ImportSummary,
    val issues: List<ImportIssue> = emptyList(),
  ) : ImportParseResult

  data class Failure(
    val issues: List<ImportIssue> = emptyList(),
  ) : ImportParseResult
}
