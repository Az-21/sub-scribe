package az21.subscribe.ui.datatransfer

import az21.subscribe.domain.export.ImportIssue
import az21.subscribe.domain.export.ImportPreview
import az21.subscribe.domain.export.ImportSummary

/** Immutable state for the export/import screen. */
data class DataTransferUiState(
  val isBusy: Boolean = false,
  val preview: ImportPreview? = null,
  val failureIssues: List<ImportIssue>? = null,
  val feedback: TransferFeedback? = null,
)

/** One-shot result of an export or import, rendered as a snackbar then consumed. */
sealed interface TransferFeedback {
  data object ExportSucceeded : TransferFeedback

  data object ExportFailed : TransferFeedback

  data class ImportSucceeded(
    val summary: ImportSummary,
  ) : TransferFeedback

  data object ImportFailed : TransferFeedback
}
