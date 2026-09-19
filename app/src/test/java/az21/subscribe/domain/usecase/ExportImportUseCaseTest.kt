package az21.subscribe.domain.usecase

import az21.subscribe.data.export.CsvExportCodec
import az21.subscribe.data.export.JsonExportCodec
import az21.subscribe.data.fake.FakePaymentMethodDao
import az21.subscribe.data.fake.FakePriceHistoryDao
import az21.subscribe.data.fake.FakeSettingsRepository
import az21.subscribe.data.fake.FakeSubscriptionDao
import az21.subscribe.data.fake.FakeTagDao
import az21.subscribe.data.fake.FakeTransactionRunner
import az21.subscribe.data.repository.DataTransferRepositoryImpl
import az21.subscribe.domain.export.ExportCodec
import az21.subscribe.domain.export.ExportFormat
import az21.subscribe.domain.export.ImportIssueReason
import az21.subscribe.domain.export.ImportParseResult
import az21.subscribe.domain.export.sampleExportDocument
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ExportImportUseCaseTest {
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
  private val exportData = ExportDataUseCase(repository, codecs, Clock.systemUTC())
  private val importData = ImportDataUseCase(repository, codecs)

  @Test
  fun exportJson_roundTripsThroughParse() =
    runTest {
      val document = sampleExportDocument()
      repository.merge(document)

      val file = exportData(ExportFormat.JSON)
      val parsed = importData.parse(file.bytes)

      assertTrue(parsed is ImportParseResult.Success)
      val success = parsed as ImportParseResult.Success
      assertEquals(document.subscriptions.sortedBy { it.id }, success.document.subscriptions.sortedBy { it.id })
      assertEquals(document.priceHistory.sortedBy { it.id }, success.document.priceHistory.sortedBy { it.id })
    }

  @Test
  fun exportCsv_roundTripsThroughParse() =
    runTest {
      val document = sampleExportDocument()
      repository.merge(document)

      val file = exportData(ExportFormat.CSV)
      val parsed = importData.parse(file.bytes)

      assertTrue(parsed is ImportParseResult.Success)
      val success = parsed as ImportParseResult.Success
      assertEquals(document.tags.sortedBy { it.id }, success.document.tags.sortedBy { it.id })
      assertEquals(document.paymentMethods.sortedBy { it.id }, success.document.paymentMethods.sortedBy { it.id })
    }

  @Test
  fun parseThenCommit_persistsImportedData() =
    runTest {
      repository.merge(sampleExportDocument())
      val expected = repository.exportSnapshot()

      for (format in ExportFormat.entries) {
        val file = exportData(format)
        val success = importData.parse(file.bytes) as ImportParseResult.Success
        importData.commit(success.document)
      }

      val actual = repository.exportSnapshot()
      assertEquals(expected.subscriptions.sortedBy { it.id }, actual.subscriptions.sortedBy { it.id })
      assertEquals(expected.priceHistory.sortedBy { it.id }, actual.priceHistory.sortedBy { it.id })
    }

  @Test
  fun parse_rejectsUnsupportedSchemaVersion() {
    val bytes = JsonExportCodec().encode(sampleExportDocument().copy(schemaVersion = 99))

    val result = importData.parse(bytes)

    val failure = result as ImportParseResult.Failure
    assertTrue(failure.issues.any { it.reason == ImportIssueReason.UNSUPPORTED_VERSION })
  }

  @Test
  fun exportFile_usesFormatMetadata() =
    runTest {
      repository.merge(sampleExportDocument())

      val json = exportData(ExportFormat.JSON)
      val csv = exportData(ExportFormat.CSV)

      assertEquals(ExportFormat.JSON.mimeType, json.mimeType)
      assertTrue(json.fileName.endsWith(".json"))
      assertEquals(ExportFormat.CSV.mimeType, csv.mimeType)
      assertTrue(csv.fileName.endsWith(".csv.zip"))
    }
}
