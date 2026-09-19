package az21.subscribe.data.repository

import az21.subscribe.data.fake.FakePaymentMethodDao
import az21.subscribe.data.fake.FakePriceHistoryDao
import az21.subscribe.data.fake.FakeSettingsRepository
import az21.subscribe.data.fake.FakeSubscriptionDao
import az21.subscribe.data.fake.FakeTagDao
import az21.subscribe.data.fake.FakeTransactionRunner
import az21.subscribe.data.local.entity.SubscriptionEntity
import az21.subscribe.data.local.entity.SubscriptionTagEntity
import az21.subscribe.domain.export.sampleExportDocument
import az21.subscribe.domain.model.BillingCycle
import az21.subscribe.domain.model.Currency
import az21.subscribe.domain.model.SubscriptionStatus
import az21.subscribe.domain.model.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

class DataTransferRepositoryImplTest {
  private val subscriptionDao = FakeSubscriptionDao()
  private val priceHistoryDao = FakePriceHistoryDao()
  private val tagDao = FakeTagDao()
  private val paymentMethodDao = FakePaymentMethodDao()
  private val settingsRepository = FakeSettingsRepository()
  private val clock = Clock.fixed(Instant.parse("2026-02-03T04:05:06Z"), ZoneOffset.UTC)
  private val repository =
    DataTransferRepositoryImpl(
      subscriptionDao = subscriptionDao,
      priceHistoryDao = priceHistoryDao,
      tagDao = tagDao,
      paymentMethodDao = paymentMethodDao,
      settingsRepository = settingsRepository,
      transactionRunner = FakeTransactionRunner(),
      clock = clock,
    )

  @Test
  fun mergeThenExport_reproducesDocument() =
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
  fun merge_replacesTagAssignmentsForImportedSubscriptions() =
    runTest {
      val importedId = UUID.fromString("11111111-1111-1111-1111-111111111111")
      val staleTagId = UUID.fromString("99999999-9999-9999-9999-999999999999")
      subscriptionDao.upsert(existingSubscription(importedId))
      subscriptionDao.insertTags(listOf(SubscriptionTagEntity(subscriptionId = importedId, tagId = staleTagId)))

      repository.merge(sampleExportDocument())

      val assignments = subscriptionDao.getAllTagAssignments()
      assertEquals(1, assignments.size)
      assertEquals(UUID.fromString("33333333-3333-3333-3333-333333333333"), assignments.single().tagId)
    }

  private fun existingSubscription(id: UUID): SubscriptionEntity {
    val now = Instant.parse("2024-01-01T00:00:00Z")
    return SubscriptionEntity(
      id = id,
      name = "Old",
      iconId = "old",
      startDate = LocalDate.of(2024, 1, 1),
      billingCycle = BillingCycle.MONTHLY,
      status = SubscriptionStatus.ACTIVE,
      endDate = null,
      paymentMethodId = null,
      notes = null,
      createdAt = now,
      updatedAt = now,
    )
  }
}
