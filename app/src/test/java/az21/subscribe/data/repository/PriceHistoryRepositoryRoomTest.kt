package az21.subscribe.data.repository

import az21.subscribe.data.local.SubScribeDatabase
import az21.subscribe.data.local.entity.SubscriptionEntity
import az21.subscribe.data.local.inMemorySubScribeDatabase
import az21.subscribe.domain.model.BillingCycle
import az21.subscribe.domain.model.SubscriptionStatus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

/** Exercises [PriceHistoryRepositoryImpl] against a real in-memory Room database. */
@RunWith(RobolectricTestRunner::class)
class PriceHistoryRepositoryRoomTest {
  private val clock = Clock.fixed(Instant.parse("2024-05-20T09:00:00Z"), ZoneOffset.UTC)
  private lateinit var database: SubScribeDatabase
  private lateinit var subscriptionId: UUID
  private lateinit var repository: PriceHistoryRepositoryImpl

  @Before
  fun setUp() {
    database = inMemorySubScribeDatabase()
    subscriptionId = insertSubscription()
    repository = PriceHistoryRepositoryImpl(database.priceHistoryDao(), clock)
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun getPriceEffectiveOn_returnsLatestEntryNotAfterDate() =
    runTest {
      repository.addPriceChange(subscriptionId, BigDecimal("9.99"), LocalDate.of(2023, 1, 1))
      repository.addPriceChange(subscriptionId, BigDecimal("12.99"), LocalDate.of(2024, 1, 1))
      repository.addPriceChange(subscriptionId, BigDecimal("14.99"), LocalDate.of(2025, 1, 1))

      val price = repository.getPriceEffectiveOn(subscriptionId, LocalDate.of(2024, 6, 1))

      assertEquals(BigDecimal("12.99"), price?.price)
    }

  @Test
  fun getPriceEffectiveOn_resolvesBackdatedAndFutureDatedEntries() =
    runTest {
      repository.addPriceChange(subscriptionId, BigDecimal("12.99"), LocalDate.of(2024, 1, 1))
      repository.addPriceChange(subscriptionId, BigDecimal("8.99"), LocalDate.of(2023, 6, 1))
      repository.addPriceChange(subscriptionId, BigDecimal("14.99"), LocalDate.of(2025, 1, 1))

      assertEquals(
        BigDecimal("8.99"),
        repository.getPriceEffectiveOn(subscriptionId, LocalDate.of(2023, 12, 31))?.price,
      )
      assertEquals(
        BigDecimal("12.99"),
        repository.getPriceEffectiveOn(subscriptionId, LocalDate.of(2024, 12, 31))?.price,
      )
      assertNull(repository.getPriceEffectiveOn(subscriptionId, LocalDate.of(2023, 1, 1)))
    }

  @Test
  fun getPriceEffectiveOn_breaksTiesByCreationTime() =
    runTest {
      val earlier = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC)
      val later = Clock.fixed(Instant.parse("2024-02-01T00:00:00Z"), ZoneOffset.UTC)
      val date = LocalDate.of(2024, 1, 1)
      PriceHistoryRepositoryImpl(database.priceHistoryDao(), earlier)
        .addPriceChange(subscriptionId, BigDecimal("9.99"), date)
      PriceHistoryRepositoryImpl(database.priceHistoryDao(), later)
        .addPriceChange(subscriptionId, BigDecimal("11.99"), date)

      val price = repository.getPriceEffectiveOn(subscriptionId, LocalDate.of(2024, 6, 1))

      assertEquals(BigDecimal("11.99"), price?.price)
    }

  @Test
  fun getTimeline_isNewestFirst() =
    runTest {
      repository.addPriceChange(subscriptionId, BigDecimal("9.99"), LocalDate.of(2023, 1, 1))
      repository.addPriceChange(subscriptionId, BigDecimal("12.99"), LocalDate.of(2024, 1, 1))

      val timeline = repository.getTimeline(subscriptionId)

      assertEquals(listOf(BigDecimal("12.99"), BigDecimal("9.99")), timeline.map { it.price })
    }

  @Test
  fun updateEntry_changesPriceAndDateInRealDatabase() =
    runTest {
      val entry = repository.addPriceChange(subscriptionId, BigDecimal("9.99"), LocalDate.of(2023, 1, 1))

      repository.updateEntry(entry.id, BigDecimal("12.99"), LocalDate.of(2024, 1, 1))

      val updated = repository.getTimeline(subscriptionId).single()
      assertEquals(entry.id, updated.id)
      assertEquals(BigDecimal("12.99"), updated.price)
      assertEquals(LocalDate.of(2024, 1, 1), updated.effectiveFromDate)
    }

  @Test
  fun deletingSubscription_cascadesPriceHistory() =
    runTest {
      repository.addPriceChange(subscriptionId, BigDecimal("9.99"), LocalDate.of(2023, 1, 1))

      database.subscriptionDao().deleteById(subscriptionId)

      assertTrue(repository.getTimeline(subscriptionId).isEmpty())
    }

  private fun insertSubscription(): UUID {
    val id = UUID.randomUUID()
    val now = clock.instant()
    val entity =
      SubscriptionEntity(
        id = id,
        name = "Netflix",
        iconId = "netflix",
        startDate = LocalDate.of(2023, 1, 1),
        billingCycle = BillingCycle.MONTHLY,
        status = SubscriptionStatus.ACTIVE,
        endDate = null,
        paymentMethodId = null,
        notes = null,
        createdAt = now,
        updatedAt = now,
      )
    runBlocking { database.subscriptionDao().upsert(entity) }
    return id
  }
}
