package az21.subscribe.domain.usecase

import az21.subscribe.data.fake.FakePriceHistoryDao
import az21.subscribe.data.fake.FakeSubscriptionDao
import az21.subscribe.data.repository.PriceHistoryRepositoryImpl
import az21.subscribe.data.repository.SubscriptionRepositoryImpl
import az21.subscribe.domain.model.BillingCycle
import az21.subscribe.domain.model.PriceEntryDraft
import az21.subscribe.domain.model.SubscriptionDraft
import az21.subscribe.domain.model.SubscriptionTransitionException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

class SavePriceHistoryUseCaseTest {
  private val clock = Clock.fixed(Instant.parse("2024-05-20T09:00:00Z"), ZoneOffset.UTC)
  private val subscriptionRepository = SubscriptionRepositoryImpl(FakeSubscriptionDao(), clock)
  private val priceHistoryRepository = PriceHistoryRepositoryImpl(FakePriceHistoryDao(), clock)
  private val useCase =
    SavePriceHistoryUseCase(
      subscriptionRepository,
      priceHistoryRepository,
      AddPriceChangeUseCase(subscriptionRepository, priceHistoryRepository),
    )

  @Test
  fun createsAllEntriesForANewSubscription() =
    runTest {
      val subscription = subscriptionRepository.createSubscription(draft())

      useCase(
        subscription.id,
        listOf(
          PriceEntryDraft(null, BigDecimal("9.99"), LocalDate.of(2024, 1, 1)),
          PriceEntryDraft(null, BigDecimal("12.99"), LocalDate.of(2024, 6, 1)),
        ),
      )

      assertEquals(
        listOf(BigDecimal("12.99"), BigDecimal("9.99")),
        priceHistoryRepository.getTimeline(subscription.id).map { it.price },
      )
    }

  @Test
  fun updatesChangedEntryInPlacePreservingId() =
    runTest {
      val subscription = subscriptionRepository.createSubscription(draft())
      val entry =
        priceHistoryRepository.addPriceChange(subscription.id, BigDecimal("9.99"), LocalDate.of(2024, 1, 1))

      useCase(
        subscription.id,
        listOf(PriceEntryDraft(entry.id, BigDecimal("14.99"), LocalDate.of(2024, 3, 1))),
      )

      val timeline = priceHistoryRepository.getTimeline(subscription.id)
      assertEquals(listOf(BigDecimal("14.99")), timeline.map { it.price })
      assertEquals(entry.id, timeline.single().id)
      assertEquals(LocalDate.of(2024, 3, 1), timeline.single().effectiveFromDate)
    }

  @Test
  fun deletesEntriesMissingFromDesiredList() =
    runTest {
      val subscription = subscriptionRepository.createSubscription(draft())
      priceHistoryRepository.addPriceChange(subscription.id, BigDecimal("9.99"), LocalDate.of(2024, 1, 1))
      priceHistoryRepository.addPriceChange(subscription.id, BigDecimal("12.99"), LocalDate.of(2024, 6, 1))
      val kept =
        priceHistoryRepository.getTimeline(subscription.id).first { it.price == BigDecimal("9.99") }

      useCase(subscription.id, listOf(PriceEntryDraft(kept.id, kept.price, kept.effectiveFromDate)))

      assertEquals(
        listOf(BigDecimal("9.99")),
        priceHistoryRepository.getTimeline(subscription.id).map { it.price },
      )
    }

  @Test
  fun appendsNewEntriesAlongsideExistingOnes() =
    runTest {
      val subscription = subscriptionRepository.createSubscription(draft())
      val existing =
        priceHistoryRepository.addPriceChange(subscription.id, BigDecimal("9.99"), LocalDate.of(2024, 1, 1))

      useCase(
        subscription.id,
        listOf(
          PriceEntryDraft(existing.id, existing.price, existing.effectiveFromDate),
          PriceEntryDraft(null, BigDecimal("19.99"), LocalDate.of(2024, 9, 1)),
        ),
      )

      assertEquals(
        listOf(BigDecimal("19.99"), BigDecimal("9.99")),
        priceHistoryRepository.getTimeline(subscription.id).map { it.price },
      )
    }

  @Test
  fun rejectsEmptyList() =
    runTest {
      val subscription = subscriptionRepository.createSubscription(draft())

      val error = runCatching { useCase(subscription.id, emptyList()) }.exceptionOrNull()

      assertTrue(error is IllegalArgumentException)
    }

  @Test
  fun rejectsNegativePriceForAnUpdate() =
    runTest {
      val subscription = subscriptionRepository.createSubscription(draft())
      val entry =
        priceHistoryRepository.addPriceChange(subscription.id, BigDecimal("9.99"), LocalDate.of(2024, 1, 1))

      val error =
        runCatching {
          useCase(subscription.id, listOf(PriceEntryDraft(entry.id, BigDecimal("-1.00"), entry.effectiveFromDate)))
        }.exceptionOrNull()

      assertTrue(error is IllegalArgumentException)
    }

  @Test
  fun rejectsUnknownSubscription() =
    runTest {
      val error =
        runCatching {
          useCase(
            UUID.randomUUID(),
            listOf(PriceEntryDraft(null, BigDecimal("9.99"), LocalDate.of(2024, 1, 1))),
          )
        }.exceptionOrNull()

      assertTrue(error is SubscriptionTransitionException.NotFound)
    }

  private fun draft(): SubscriptionDraft =
    SubscriptionDraft(
      name = "Netflix",
      iconId = "netflix",
      billingCycle = BillingCycle.MONTHLY,
    )
}
