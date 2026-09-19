package az21.subscribe.data.repository

import az21.subscribe.data.fake.FakePriceHistoryDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

class PriceHistoryRepositoryImplTest {
  private val subscriptionId = UUID.randomUUID()
  private val dao = FakePriceHistoryDao()
  private val repository = PriceHistoryRepositoryImpl(dao, Clock.systemUTC())

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
  fun getPriceEffectiveOn_respectsBackdatedCorrection() =
    runTest {
      repository.addPriceChange(subscriptionId, BigDecimal("12.99"), LocalDate.of(2024, 1, 1))
      repository.addPriceChange(subscriptionId, BigDecimal("8.99"), LocalDate.of(2023, 6, 1))

      val price = repository.getPriceEffectiveOn(subscriptionId, LocalDate.of(2023, 12, 31))

      assertEquals(BigDecimal("8.99"), price?.price)
    }

  @Test
  fun getPriceEffectiveOn_ignoresFutureDatedEntries() =
    runTest {
      repository.addPriceChange(subscriptionId, BigDecimal("9.99"), LocalDate.of(2024, 1, 1))
      repository.addPriceChange(subscriptionId, BigDecimal("14.99"), LocalDate.of(2024, 12, 1))

      val price = repository.getPriceEffectiveOn(subscriptionId, LocalDate.of(2024, 6, 1))

      assertEquals(BigDecimal("9.99"), price?.price)
    }

  @Test
  fun getPriceEffectiveOn_isNullBeforeFirstEntry() =
    runTest {
      repository.addPriceChange(subscriptionId, BigDecimal("9.99"), LocalDate.of(2024, 1, 1))

      assertNull(repository.getPriceEffectiveOn(subscriptionId, LocalDate.of(2023, 12, 31)))
    }

  @Test
  fun getTimeline_isNewestFirst() =
    runTest {
      repository.addPriceChange(subscriptionId, BigDecimal("9.99"), LocalDate.of(2023, 1, 1))
      repository.addPriceChange(subscriptionId, BigDecimal("12.99"), LocalDate.of(2024, 1, 1))

      val timeline = repository.getTimeline(subscriptionId)

      assertEquals(
        listOf(BigDecimal("12.99"), BigDecimal("9.99")),
        timeline.map { it.price },
      )
    }

  @Test
  fun observeAllTimelines_groupsBySubscriptionNewestFirst() =
    runTest {
      val otherId = UUID.randomUUID()
      repository.addPriceChange(subscriptionId, BigDecimal("9.99"), LocalDate.of(2023, 1, 1))
      repository.addPriceChange(subscriptionId, BigDecimal("12.99"), LocalDate.of(2024, 1, 1))
      repository.addPriceChange(otherId, BigDecimal("4.99"), LocalDate.of(2024, 2, 1))

      val timelines = repository.observeAllTimelines().first()

      assertEquals(
        listOf(BigDecimal("12.99"), BigDecimal("9.99")),
        timelines.getValue(subscriptionId).map { it.price },
      )
      assertEquals(listOf(BigDecimal("4.99")), timelines.getValue(otherId).map { it.price })
    }

  @Test
  fun updateEntry_changesPriceAndDatePreservingIdentity() =
    runTest {
      val entry = repository.addPriceChange(subscriptionId, BigDecimal("9.99"), LocalDate.of(2023, 1, 1))

      repository.updateEntry(entry.id, BigDecimal("12.99"), LocalDate.of(2024, 1, 1))

      val updated = repository.getTimeline(subscriptionId).single()
      assertEquals(entry.id, updated.id)
      assertEquals(entry.createdAt, updated.createdAt)
      assertEquals(BigDecimal("12.99"), updated.price)
      assertEquals(LocalDate.of(2024, 1, 1), updated.effectiveFromDate)
    }

  @Test
  fun deleteEntry_removesOnlyThatPricePoint() =
    runTest {
      val oldest = repository.addPriceChange(subscriptionId, BigDecimal("9.99"), LocalDate.of(2023, 1, 1))
      repository.addPriceChange(subscriptionId, BigDecimal("12.99"), LocalDate.of(2024, 1, 1))

      repository.deleteEntry(oldest.id)

      assertEquals(
        listOf(BigDecimal("12.99")),
        repository.getTimeline(subscriptionId).map { it.price },
      )
    }

  @Test
  fun deleteEntry_leavesOtherSubscriptionsUntouched() =
    runTest {
      val otherId = UUID.randomUUID()
      val entry = repository.addPriceChange(subscriptionId, BigDecimal("9.99"), LocalDate.of(2023, 1, 1))
      repository.addPriceChange(otherId, BigDecimal("4.99"), LocalDate.of(2024, 2, 1))

      repository.deleteEntry(entry.id)

      assertEquals(
        listOf(BigDecimal("4.99")),
        repository.getTimeline(otherId).map { it.price },
      )
    }

  @Test
  fun addPriceChange_stampsCreationTime() =
    runTest {
      val instant = Instant.parse("2024-03-01T00:00:00Z")
      val fixedRepository = PriceHistoryRepositoryImpl(dao, Clock.fixed(instant, ZoneOffset.UTC))

      val entry =
        fixedRepository.addPriceChange(subscriptionId, BigDecimal("5.00"), LocalDate.of(2024, 3, 1))

      assertEquals(instant, entry.createdAt)
    }
}
