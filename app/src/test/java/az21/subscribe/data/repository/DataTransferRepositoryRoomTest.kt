package az21.subscribe.data.repository

import az21.subscribe.data.fake.FakeSettingsRepository
import az21.subscribe.data.local.RoomTransactionRunner
import az21.subscribe.data.local.SubScribeDatabase
import az21.subscribe.data.local.entity.SubscriptionTagEntity
import az21.subscribe.data.local.inMemorySubScribeDatabase
import az21.subscribe.domain.export.sampleExportDocument
import az21.subscribe.domain.model.Currency
import az21.subscribe.domain.model.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

/** Exercises [DataTransferRepositoryImpl] against a real in-memory Room database. */
@RunWith(RobolectricTestRunner::class)
class DataTransferRepositoryRoomTest {
  private val clock = Clock.fixed(Instant.parse("2026-02-03T04:05:06Z"), ZoneOffset.UTC)
  private lateinit var database: SubScribeDatabase
  private lateinit var settingsRepository: FakeSettingsRepository
  private lateinit var repository: DataTransferRepositoryImpl

  @Before
  fun setUp() {
    database = inMemorySubScribeDatabase()
    settingsRepository = FakeSettingsRepository()
    repository =
      DataTransferRepositoryImpl(
        subscriptionDao = database.subscriptionDao(),
        priceHistoryDao = database.priceHistoryDao(),
        tagDao = database.tagDao(),
        paymentMethodDao = database.paymentMethodDao(),
        settingsRepository = settingsRepository,
        transactionRunner = RoomTransactionRunner(database),
        clock = clock,
      )
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun mergeThenExport_reproducesDocumentThroughRealDatabase() =
    runTest {
      val document = sampleExportDocument()

      repository.merge(document)
      val snapshot = repository.exportSnapshot()

      assertEquals(clock.instant().toString(), snapshot.exportedAt)
      assertEquals(document.subscriptions.sortedBy { it.id }, snapshot.subscriptions.sortedBy { it.id })
      assertEquals(document.priceHistory.sortedBy { it.id }, snapshot.priceHistory.sortedBy { it.id })
      assertEquals(document.tags.sortedBy { it.id }, snapshot.tags.sortedBy { it.id })
      assertEquals(
        document.subscriptionTags.sortedBy { it.subscriptionId },
        snapshot.subscriptionTags.sortedBy { it.subscriptionId },
      )
      assertEquals(document.paymentMethods.sortedBy { it.id }, snapshot.paymentMethods.sortedBy { it.id })
      assertEquals(document.settings, snapshot.settings)
    }

  @Test
  fun merge_appliesSettings() =
    runTest {
      repository.merge(sampleExportDocument())

      val settings = settingsRepository.settings.first()
      assertEquals(Currency.EUR, settings.currency)
      assertEquals(ThemeMode.DARK, settings.themeMode)
    }

  @Test
  fun merge_isIdempotentWhenRunTwice() =
    runTest {
      val document = sampleExportDocument()

      repository.merge(document)
      repository.merge(document)

      val snapshot = repository.exportSnapshot()
      assertEquals(document.subscriptions.size, snapshot.subscriptions.size)
      assertEquals(document.priceHistory.size, snapshot.priceHistory.size)
      assertEquals(document.tags.size, snapshot.tags.size)
      assertEquals(document.subscriptionTags.size, snapshot.subscriptionTags.size)
      assertEquals(document.paymentMethods.size, snapshot.paymentMethods.size)
    }

  @Test
  fun merge_replacesTagAssignmentsForImportedSubscriptions() =
    runTest {
      repository.merge(sampleExportDocument())
      val importedId = UUID.fromString("11111111-1111-1111-1111-111111111111")
      val staleTag = UUID.randomUUID()
      runBlocking {
        database.tagDao().upsert(
          az21.subscribe.data.local.entity
            .TagEntity(id = staleTag, name = "Stale", color = null),
        )
        database.subscriptionDao().insertTags(
          listOf(SubscriptionTagEntity(subscriptionId = importedId, tagId = staleTag)),
        )
      }

      repository.merge(sampleExportDocument())

      val assignments = database.subscriptionDao().getAllTagAssignments()
      assertEquals(1, assignments.size)
      assertEquals(UUID.fromString("33333333-3333-3333-3333-333333333333"), assignments.single().tagId)
    }
}
