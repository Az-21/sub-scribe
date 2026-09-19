package az21.subscribe.domain.usecase

import az21.subscribe.data.fake.FakeSubscriptionDao
import az21.subscribe.data.repository.SubscriptionRepositoryImpl
import az21.subscribe.domain.model.BillingCycle
import az21.subscribe.domain.model.SubscriptionDraft
import az21.subscribe.domain.model.SubscriptionStatus
import az21.subscribe.domain.model.SubscriptionTransitionException
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

class SubscriptionLifecycleUseCasesTest {
  private val today = LocalDate.of(2024, 5, 20)
  private val clock = Clock.fixed(Instant.parse("2024-05-20T09:00:00Z"), ZoneOffset.UTC)
  private val repository = SubscriptionRepositoryImpl(FakeSubscriptionDao(), clock)
  private val cancel = CancelSubscriptionUseCase(repository)
  private val archive = ArchiveSubscriptionUseCase(repository)
  private val delete = DeleteSubscriptionUseCase(repository)

  @Test
  fun cancel_defaultsEndDateToToday() =
    runTest {
      val created = repository.createSubscription(draft())

      val cancelled = cancel(created.id)

      assertEquals(SubscriptionStatus.CANCELLED, cancelled.status)
      assertEquals(today, cancelled.endDate)
    }

  @Test
  fun cancel_honoursExplicitEndDate() =
    runTest {
      val created = repository.createSubscription(draft())
      val end = LocalDate.of(2024, 6, 30)

      assertEquals(end, cancel(created.id, end).endDate)
    }

  @Test
  fun archive_rejectsActiveSubscription() =
    runTest {
      val created = repository.createSubscription(draft())

      val error = runCatching { archive(created.id) }.exceptionOrNull()

      assertTrue(error is SubscriptionTransitionException.CannotArchive)
    }

  @Test
  fun archive_succeedsAfterCancellation() =
    runTest {
      val created = repository.createSubscription(draft())
      cancel(created.id)

      assertEquals(SubscriptionStatus.ARCHIVED, archive(created.id).status)
    }

  @Test
  fun delete_rejectsNotArchivedSubscription() =
    runTest {
      val created = repository.createSubscription(draft())
      cancel(created.id)

      val error = runCatching { delete(created.id) }.exceptionOrNull()

      assertTrue(error is SubscriptionTransitionException.CannotDelete)
    }

  @Test
  fun delete_succeedsWhenArchived() =
    runTest {
      val created = repository.createSubscription(draft())
      cancel(created.id)
      archive(created.id)

      delete(created.id)

      assertNull(repository.getSubscription(created.id))
    }

  @Test
  fun cancel_rejectsArchivedSubscription() =
    runTest {
      val created = repository.createSubscription(draft())
      cancel(created.id)
      archive(created.id)

      val error = runCatching { cancel(created.id) }.exceptionOrNull()

      assertTrue(error is SubscriptionTransitionException.CannotCancel)
    }

  @Test
  fun archive_rejectsAlreadyArchivedSubscription() =
    runTest {
      val created = repository.createSubscription(draft())
      cancel(created.id)
      archive(created.id)

      val error = runCatching { archive(created.id) }.exceptionOrNull()

      assertTrue(error is SubscriptionTransitionException.CannotArchive)
    }

  @Test
  fun delete_rejectsActiveSubscription() =
    runTest {
      val created = repository.createSubscription(draft())

      val error = runCatching { delete(created.id) }.exceptionOrNull()

      assertTrue(error is SubscriptionTransitionException.CannotDelete)
    }

  @Test
  fun transitions_failForUnknownSubscription() =
    runTest {
      val missing = UUID.randomUUID()

      assertTrue(runCatching { cancel(missing) }.exceptionOrNull() is SubscriptionTransitionException.NotFound)
      assertTrue(runCatching { archive(missing) }.exceptionOrNull() is SubscriptionTransitionException.NotFound)
      assertTrue(runCatching { delete(missing) }.exceptionOrNull() is SubscriptionTransitionException.NotFound)
    }

  private fun draft(): SubscriptionDraft =
    SubscriptionDraft(
      name = "Netflix",
      iconId = "netflix",
      billingCycle = BillingCycle.MONTHLY,
    )
}
