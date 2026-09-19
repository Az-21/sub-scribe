package az21.subscribe.ui.datatransfer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az21.subscribe.R
import az21.subscribe.domain.export.ExportFormat
import az21.subscribe.domain.export.ImportIssue
import az21.subscribe.domain.export.ImportIssueReason
import az21.subscribe.domain.export.ImportPreview
import az21.subscribe.ui.common.SubScribeTopAppBar
import az21.subscribe.ui.theme.AppTheme
import java.time.LocalDate

@Composable
fun DataTransferScreen(
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: DataTransferViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  val jsonExportLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ExportFormat.JSON.mimeType)) { uri ->
      uri?.let { viewModel.exportTo(it.toString(), ExportFormat.JSON) }
    }
  val csvExportLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ExportFormat.CSV.mimeType)) { uri ->
      uri?.let { viewModel.exportTo(it.toString(), ExportFormat.CSV) }
    }
  val importLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      uri?.let { viewModel.importFrom(it.toString()) }
    }

  DataTransferContent(
    uiState = uiState,
    onBack = onBack,
    onExportJson = { jsonExportLauncher.launch(suggestedFileName(ExportFormat.JSON)) },
    onExportCsv = { csvExportLauncher.launch(suggestedFileName(ExportFormat.CSV)) },
    onImport = { importLauncher.launch(ImportMimeTypes) },
    onConfirmImport = viewModel::confirmImport,
    onCancelImport = viewModel::cancelImport,
    onDismissFailure = viewModel::consumeFailureIssues,
    onFeedbackConsumed = viewModel::consumeFeedback,
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataTransferContent(
  uiState: DataTransferUiState,
  onBack: () -> Unit,
  onExportJson: () -> Unit,
  onExportCsv: () -> Unit,
  onImport: () -> Unit,
  onConfirmImport: () -> Unit,
  onCancelImport: () -> Unit,
  onDismissFailure: () -> Unit,
  onFeedbackConsumed: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val snackbarHostState = remember { SnackbarHostState() }
  val feedbackMessage = snackbarMessage(uiState.feedback)

  LaunchedEffect(feedbackMessage) {
    if (feedbackMessage != null) {
      snackbarHostState.showSnackbar(feedbackMessage)
      onFeedbackConsumed()
    }
  }

  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  Scaffold(
    modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      SubScribeTopAppBar(
        title = stringResource(R.string.transfer_title),
        onNavigateUp = onBack,
        scrollBehavior = scrollBehavior,
      )
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
  ) { innerPadding ->
    DataTransferBody(
      uiState = uiState,
      onExportJson = onExportJson,
      onExportCsv = onExportCsv,
      onImport = onImport,
      modifier = Modifier.fillMaxSize().padding(innerPadding),
    )
  }

  uiState.preview?.let { preview ->
    ImportPreviewDialog(preview = preview, onConfirm = onConfirmImport, onDismiss = onCancelImport)
  }

  uiState.failureIssues?.let { issues ->
    ImportFailureDialog(issues = issues, onDismiss = onDismissFailure)
  }
}

@Composable
private fun DataTransferBody(
  uiState: DataTransferUiState,
  onExportJson: () -> Unit,
  onExportCsv: () -> Unit,
  onImport: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    if (uiState.isBusy) {
      LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
    ExportCard(enabled = !uiState.isBusy, onExportJson = onExportJson, onExportCsv = onExportCsv)
    ImportCard(enabled = !uiState.isBusy, onImport = onImport)
  }
}

@Composable
private fun ExportCard(
  enabled: Boolean,
  onExportJson: () -> Unit,
  onExportCsv: () -> Unit,
) {
  Card(modifier = Modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(stringResource(R.string.transfer_export_section), style = MaterialTheme.typography.titleMedium)
      Text(
        stringResource(R.string.transfer_export_description),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Button(
        onClick = onExportJson,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MinHeight, hasStartIcon = true),
      ) {
        ButtonLeadingIcon(imageVector = Icons.Default.Share)
        Text(stringResource(R.string.transfer_export_json))
      }
      OutlinedButton(
        onClick = onExportCsv,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MinHeight, hasStartIcon = true),
      ) {
        ButtonLeadingIcon(imageVector = Icons.Default.Share)
        Text(stringResource(R.string.transfer_export_csv))
      }
    }
  }
}

@Composable
private fun ImportCard(
  enabled: Boolean,
  onImport: () -> Unit,
) {
  Card(modifier = Modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(stringResource(R.string.transfer_import_section), style = MaterialTheme.typography.titleMedium)
      Text(
        stringResource(R.string.transfer_import_description),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Button(
        onClick = onImport,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MinHeight, hasStartIcon = true),
      ) {
        ButtonLeadingIcon(imageVector = Icons.Default.Refresh)
        Text(stringResource(R.string.transfer_import_button))
      }
    }
  }
}

@Composable
private fun snackbarMessage(feedback: TransferFeedback?): String? =
  when (feedback) {
    null -> {
      null
    }

    TransferFeedback.ExportSucceeded -> {
      stringResource(R.string.transfer_export_succeeded)
    }

    TransferFeedback.ExportFailed -> {
      stringResource(R.string.transfer_export_failed)
    }

    TransferFeedback.ImportFailed -> {
      stringResource(R.string.transfer_import_failed)
    }

    is TransferFeedback.ImportSucceeded -> {
      stringResource(
        R.string.transfer_import_succeeded,
        feedback.summary.subscriptions,
        feedback.summary.priceHistory,
        feedback.summary.tags,
        feedback.summary.paymentMethods,
      )
    }
  }

@Composable
private fun ImportPreviewDialog(
  preview: ImportPreview,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.transfer_preview_title)) },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          stringResource(
            R.string.transfer_preview_counts,
            preview.summary.subscriptions,
            preview.summary.priceHistory,
            preview.summary.tags,
            preview.summary.paymentMethods,
          ),
        )
        if (preview.issues.isNotEmpty()) {
          HorizontalDivider()
          Text(stringResource(R.string.transfer_issues_title), style = MaterialTheme.typography.titleSmall)
          preview.issues.forEach { issue -> IssueRow(issue) }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onConfirm) { Text(stringResource(R.string.transfer_import_confirm)) }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
    },
  )
}

@Composable
private fun ImportFailureDialog(
  issues: List<ImportIssue>,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.transfer_import_failed_title)) },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        issues.forEach { issue -> IssueRow(issue) }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
    },
  )
}

@Composable
private fun ButtonLeadingIcon(imageVector: ImageVector) {
  val buttonHeight = ButtonDefaults.MinHeight
  Icon(
    imageVector = imageVector,
    contentDescription = null,
    modifier = Modifier.size(ButtonDefaults.iconSizeFor(buttonHeight)),
  )
  Spacer(modifier = Modifier.size(ButtonDefaults.iconSpacingFor(buttonHeight)))
}

@Composable
private fun IssueRow(issue: ImportIssue) {
  val reason = stringResource(issue.reason.labelRes())
  val text = if (issue.location.isBlank()) reason else "${issue.location} · $reason"
  Text(
    text = text,
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
}

private fun ImportIssueReason.labelRes(): Int =
  when (this) {
    ImportIssueReason.UNSUPPORTED_VERSION -> R.string.transfer_issue_unsupported_version
    ImportIssueReason.MALFORMED_FILE -> R.string.transfer_issue_malformed_file
    ImportIssueReason.MISSING_FILE -> R.string.transfer_issue_missing_file
    ImportIssueReason.ROW_SKIPPED -> R.string.transfer_issue_row_skipped
    ImportIssueReason.REFERENCE_CLEARED -> R.string.transfer_issue_reference_cleared
  }

private fun suggestedFileName(format: ExportFormat): String =
  "sub-scribe-export-${LocalDate.now()}.${format.fileExtension}"

private val ImportMimeTypes =
  arrayOf(
    "application/json",
    "application/zip",
    "application/x-zip-compressed",
    "application/octet-stream",
    "text/csv",
    "text/comma-separated-values",
  )

@Preview(showBackground = true)
@Composable
private fun DataTransferContentPreview() {
  AppTheme {
    DataTransferContent(
      uiState = DataTransferUiState(),
      onBack = {},
      onExportJson = {},
      onExportCsv = {},
      onImport = {},
      onConfirmImport = {},
      onCancelImport = {},
      onDismissFailure = {},
      onFeedbackConsumed = {},
    )
  }
}
