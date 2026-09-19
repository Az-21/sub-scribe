package az21.subscribe.ui.datatransfer

import az21.subscribe.MainDispatcherRule
import az21.subscribe.data.export.CsvExportCodec
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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
  private val codecs: Set<ExportCodec> = setOf(JsonExportCodec(), CsvExportCodec())
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

      viewModel.exportTo(URI, ExportFormat.JSON)
      advanceUntilIdle()

      assertTrue(fileStore.files.containsKey(URI))
      assertEquals(TransferFeedback.ExportSucceeded, viewModel.uiState.value.feedback)
    }

  @Test
  fun exportTo_writeFailure_reportsFailure() =
    runTest(mainDispatcherRule.testDispatcher) {
      repository.merge(sampleExportDocument())
      fileStore.failWrite = true

      viewModel.exportTo(URI, ExportFormat.JSON)
      advanceUntilIdle()

      assertEquals(TransferFeedback.ExportFailed, viewModel.uiState.value.feedback)
    }

  @Test
  fun importFrom_validFile_showsPreview() =
    runTest(mainDispatcherRule.testDispatcher) {
      repository.merge(sampleExportDocument())
      val file = ExportDataUseCase(repository, codecs, Clock.systemUTC())(ExportFormat.JSON)
      fileStore.files[URI] = file.bytes

      viewModel.importFrom(URI)
      advanceUntilIdle()

      val preview = viewModel.uiState.value.preview
      assertNotNull(preview)
      assertEquals(2, preview?.summary?.subscriptions)
    }

  @Test
  fun confirmImport_commitsAndReportsSummary() =
    runTest(mainDispatcherRule.testDispatcher) {
      repository.merge(sampleExportDocument())
      val file = ExportDataUseCase(repository, codecs, Clock.systemUTC())(ExportFormat.JSON)
      fileStore.files[URI] = file.bytes
      viewModel.importFrom(URI)
      advanceUntilIdle()

      viewModel.confirmImport()
      advanceUntilIdle()

      assertNull(viewModel.uiState.value.preview)
      assertTrue(viewModel.uiState.value.feedback is TransferFeedback.ImportSucceeded)
    }

  @Test
  fun importFrom_unreadableFile_reportsFailure() =
    runTest(mainDispatcherRule.testDispatcher) {
      fileStore.failRead = true

      viewModel.importFrom(URI)
      advanceUntilIdle()

      assertEquals(TransferFeedback.ImportFailed, viewModel.uiState.value.feedback)
    }

  private companion object {
    const val URI = "content://export"
  }
}
