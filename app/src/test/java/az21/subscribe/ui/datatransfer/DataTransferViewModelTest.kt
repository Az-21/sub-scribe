package az21.subscribe.ui.datatransfer

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import az21.subscribe.MainDispatcherRule
import az21.subscribe.data.export.JsonExportCodec
import az21.subscribe.data.fake.FakeExportFileStore
import az21.subscribe.data.fake.FakePaymentMethodDao
import az21.subscribe.data.fake.FakePriceHistoryDao
import az21.subscribe.data.fake.FakeSettingsRepository
import az21.subscribe.data.fake.FakeSubscriptionDao
import az21.subscribe.data.fake.FakeTagDao
import az21.subscribe.data.fake.FakeTransactionRunner
import az21.subscribe.data.repository.DataTransferRepositoryImpl
import az21.subscribe.domain.export.ExportCodec
import az21.subscribe.domain.export.ExportFormat
import az21.subscribe.domain.export.sampleExportDocument
import az21.subscribe.domain.usecase.ExportDataUseCase
import az21.subscribe.domain.usecase.ImportDataUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class DataTransferViewModelTest {
  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  private val repository =
    DataTransferRepositoryImpl(
      subscriptionDao = FakeSubscriptionDao(),
      priceHistoryDao = FakePriceHistoryDao(),
      tagDao = FakeTagDao(),
      paymentMethodDao = FakePaymentMethodDao(),
      settingsRepository = FakeSettingsRepository(),
      transactionRunner = FakeTransactionRunner(),
      clock = Clock.fixed(Instant.parse("2026-02-03T04:05:06Z"), ZoneOffset.UTC),
    )
  private val codecs: Set<ExportCodec> = setOf(JsonExportCodec())
  private val fileStore = FakeExportFileStore()
  private val viewModel =
    DataTransferViewModel(
      exportData = ExportDataUseCase(repository, codecs, Clock.systemUTC()),
      importData = ImportDataUseCase(repository, codecs),
      fileStore = fileStore,
    )

  @Test
  fun exportTo_writesFileAndReportsSuccess() =
    runTest(mainDispatcherRule.testDispatcher) {
      repository.merge(sampleExportDocument())

      viewModel.uiState.test {
        viewModel.exportTo(URI, ExportFormat.JSON)

        val state = await { it.feedback == TransferFeedback.ExportSucceeded }
        assertTrue(fileStore.files.containsKey(URI))
        assertEquals(TransferFeedback.ExportSucceeded, state.feedback)
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun exportTo_writeFailure_reportsFailure() =
    runTest(mainDispatcherRule.testDispatcher) {
      repository.merge(sampleExportDocument())
      fileStore.failWrite = true

      viewModel.uiState.test {
        viewModel.exportTo(URI, ExportFormat.JSON)

        await { it.feedback == TransferFeedback.ExportFailed }
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun importFrom_validFile_showsPreview() =
    runTest(mainDispatcherRule.testDispatcher) {
      repository.merge(sampleExportDocument())
      val file = ExportDataUseCase(repository, codecs, Clock.systemUTC())(ExportFormat.JSON)
      fileStore.files[URI] = file.bytes

      viewModel.uiState.test {
        viewModel.importFrom(URI)

        val state = await { it.preview != null }
        assertEquals(2, state.preview?.summary?.subscriptions)
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun confirmImport_commitsAndReportsSummary() =
    runTest(mainDispatcherRule.testDispatcher) {
      repository.merge(sampleExportDocument())
      val file = ExportDataUseCase(repository, codecs, Clock.systemUTC())(ExportFormat.JSON)
      fileStore.files[URI] = file.bytes

      viewModel.uiState.test {
        viewModel.importFrom(URI)
        await { it.preview != null }

        viewModel.confirmImport()

        val state = await { it.preview == null && it.feedback is TransferFeedback.ImportSucceeded }
        assertTrue(state.feedback is TransferFeedback.ImportSucceeded)
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun importFrom_unreadableFile_reportsFailure() =
    runTest(mainDispatcherRule.testDispatcher) {
      fileStore.failRead = true

      viewModel.uiState.test {
        viewModel.importFrom(URI)

        await { it.feedback == TransferFeedback.ImportFailed }
        cancelAndIgnoreRemainingEvents()
      }
    }

  private suspend fun ReceiveTurbine<DataTransferUiState>.await(
    predicate: (DataTransferUiState) -> Boolean,
  ): DataTransferUiState {
    var state = awaitItem()
    while (!predicate(state)) {
      state = awaitItem()
    }
    return state
  }

  private companion object {
    const val URI = "content://export"
  }
}
