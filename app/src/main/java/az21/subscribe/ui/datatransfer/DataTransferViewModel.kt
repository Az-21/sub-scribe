package az21.subscribe.ui.datatransfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az21.subscribe.domain.export.ExportDocument
import az21.subscribe.domain.export.ExportFormat
import az21.subscribe.domain.export.ImportParseResult
import az21.subscribe.domain.export.ImportPreview
import az21.subscribe.domain.repository.ExportFileStore
import az21.subscribe.domain.usecase.ExportDataUseCase
import az21.subscribe.domain.usecase.ImportDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Drives the export/import screen: picks files, writes exports, previews and commits imports. */
@HiltViewModel
class DataTransferViewModel
  @Inject
  constructor(
    private val exportData: ExportDataUseCase,
    private val importData: ImportDataUseCase,
    private val fileStore: ExportFileStore,
  ) : ViewModel() {
    private val mutableUiState = MutableStateFlow(DataTransferUiState())
    val uiState: StateFlow<DataTransferUiState> = mutableUiState.asStateFlow()

    private var pendingDocument: ExportDocument? = null

    fun exportTo(
      uri: String,
      format: ExportFormat,
    ) {
      mutableUiState.update { it.copy(isBusy = true, feedback = null) }
      viewModelScope.launch {
        runCatching {
          val file = exportData(format)
          fileStore.write(uri, file.bytes)
        }.onSuccess {
          mutableUiState.update { it.copy(isBusy = false, feedback = TransferFeedback.ExportSucceeded) }
        }.onFailure {
          mutableUiState.update { it.copy(isBusy = false, feedback = TransferFeedback.ExportFailed) }
        }
      }
    }

    fun importFrom(uri: String) {
      mutableUiState.update { it.copy(isBusy = true, feedback = null) }
      viewModelScope.launch {
        val result = runCatching { importData.parse(fileStore.read(uri)) }
        result
          .onSuccess(::onParsed)
          .onFailure {
            mutableUiState.update { state -> state.copy(isBusy = false, feedback = TransferFeedback.ImportFailed) }
          }
      }
    }

    fun confirmImport() {
      val document = pendingDocument ?: return
      mutableUiState.update { it.copy(isBusy = true) }
      viewModelScope.launch {
        runCatching { importData.commit(document) }
          .onSuccess { summary ->
            pendingDocument = null
            mutableUiState.update {
              it.copy(isBusy = false, preview = null, feedback = TransferFeedback.ImportSucceeded(summary))
            }
          }.onFailure {
            mutableUiState.update { it.copy(isBusy = false, preview = null, feedback = TransferFeedback.ImportFailed) }
          }
      }
    }

    fun cancelImport() {
      pendingDocument = null
      mutableUiState.update { it.copy(preview = null) }
    }

    fun consumeFeedback() {
      mutableUiState.update { it.copy(feedback = null) }
    }

    fun consumeFailureIssues() {
      mutableUiState.update { it.copy(failureIssues = null) }
    }

    private fun onParsed(result: ImportParseResult) {
      when (result) {
        is ImportParseResult.Success -> {
          pendingDocument = result.document
          mutableUiState.update {
            it.copy(isBusy = false, preview = ImportPreview(result.summary, result.issues))
          }
        }

        is ImportParseResult.Failure -> {
          val hasIssues = result.issues.isNotEmpty()
          mutableUiState.update {
            it.copy(
              isBusy = false,
              failureIssues = result.issues.takeIf { issues -> issues.isNotEmpty() },
              feedback = TransferFeedback.ImportFailed.takeUnless { hasIssues },
            )
          }
        }
      }
    }
  }
