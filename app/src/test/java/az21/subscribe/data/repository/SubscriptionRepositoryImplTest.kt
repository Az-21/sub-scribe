package az21.subscribe.data.repository

import az21.subscribe.data.fake.FakeSubscriptionDao
import az21.subscribe.domain.model.BillingCycle
import az21.subscribe.domain.model.SubscriptionDraft
import az21.subscribe.domain.model.SubscriptionStatus
import az21.subscribe.domain.model.SubscriptionTransitionException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

class SubscriptionRepositoryImplTest {
  private val today = LocalDate.of(2024, 5, 20)
  private val clock = Clock.fixed(Instant.parse("2024-05-20T09:00:00Z"), ZoneOffset.UTC)
  private val dao = FakeSubscriptionDao()
  private val repository = SubscriptionRepositoryImpl(dao, clock)

  @Test
  fun createSubscription_generatesIdAndDefaults() =
    runTest {
      val created = repository.createSubscription(draft())

      assertEquals(today, created.startDate)
      assertEquals(SubscriptionStatus.ACTIVE, created.status)
      assertNull(created.endDate)
      assertEquals(Instant.parse("2024-05-20T09:00:00Z"), created.createdAt)
      assertEquals(created.createdAt, created.updatedAt)
    }

  @Test
  fun createSubscription_keepsExplicitStartDate() =
    runTest {
      val explicit = LocalDate.of(2020, 1, 1)

      val created = repository.createSubscription(draft(startDate = explicit))

      assertEquals(explicit, created.startDate)
    }

  @Test
  fun cancelSubscription_defaultsEndDateToToday() =
    runTest {
      val created = repository.createSubscription(draft())

      val cancelled = repository.cancelSubscription(created.id)

      assertEquals(SubscriptionStatus.CANCELLED, cancelled.status)
      assertEquals(today, cancelled.endDate)
    }

  @Test
  fun cancelSubscription_honoursExplicitEndDate() =
    runTest {
      val created = repository.createSubscription(draft())
      val end = LocalDate.of(2024, 6, 1)

      val cancelled = repository.cancelSubscription(created.id, end)

      assertEquals(end, cancelled.endDate)
    }

  @Test
  fun archive_rejectsActiveSubscription() =
    runTest {
      val created = repository.createSubscription(draft())

      val error =
        runCatching { repository.archiveSubscription(created.id) }.exceptionOrNull()

      assertTrue(error is SubscriptionTransitionException.CannotArchive)
    }

  @Test
  fun archive_succeedsAfterCancellation() =
    runTest {
      val created = repository.createSubscription(draft())
      repository.cancelSubscription(created.id)

      val archived = repository.archiveSubscription(created.id)

      assertEquals(SubscriptionStatus.ARCHIVED, archived.status)
    }

  @Test
  fun delete_rejectsNotArchivedSubscription() =
    runTest {
      val created = repository.createSubscription(draft())
      repository.cancelSubscription(created.id)

      val error = runCatching { repository.deleteSubscription(created.id) }.exceptionOrNull()

      assertTrue(error is SubscriptionTransitionException.CannotDelete)
    }

  @Test
  fun delete_succeedsWhenArchived() =
    runTest {
      val created = repository.createSubscription(draft())
      repository.cancelSubscription(created.id)
      repository.archiveSubscription(created.id)

      repository.deleteSubscription(created.id)

      assertNull(repository.getSubscription(created.id))
    }

  @Test
  fun transitions_failForUnknownSubscription() =
    runTest {
      val missing = UUID.randomUUID()

      val error = runCatching { repository.cancelSubscription(missing) }.exceptionOrNull()

      assertTrue(error is SubscriptionTransitionException.NotFound)
    }

  @Test
  fun observeSubscriptionsByStatus_filters() =
    runTest {
      val active = repository.createSubscription(draft(name = "Active"))
      val cancelled = repository.createSubscription(draft(name = "Cancelled"))
      repository.cancelSubscription(cancelled.id)

      val result = repository.observeSubscriptionsByStatus(SubscriptionStatus.ACTIVE).first()

      assertEquals(listOf(active.id), result.map { it.id })
    }

  @Test
  fun setTags_replacesExistingLinks() =
    runTest {
      val created = repository.createSubscription(draft())
      val first = UUID.randomUUID()
      val second = UUID.randomUUID()

      repository.setTags(created.id, setOf(first, second))
      repository.setTags(created.id, setOf(second))

      assertEquals(listOf(second), repository.observeTagIds(created.id).first())
    }

  private fun draft(
    name: String = "Netflix",
    startDate: LocalDate? = null,
  ): SubscriptionDraft =
    SubscriptionDraft(
      name = name,
      iconId = "netflix",
      startDate = startDate,
      billingCycle = BillingCycle.MONTHLY,
    )
}
