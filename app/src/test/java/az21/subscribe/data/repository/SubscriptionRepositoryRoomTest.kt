package az21.subscribe.data.repository

import az21.subscribe.data.local.SubScribeDatabase
import az21.subscribe.data.local.entity.SubscriptionTagEntity
import az21.subscribe.data.local.entity.TagEntity
import az21.subscribe.data.local.inMemorySubScribeDatabase
import az21.subscribe.domain.model.BillingCycle
import az21.subscribe.domain.model.SubscriptionDraft
import az21.subscribe.domain.model.SubscriptionStatus
import az21.subscribe.domain.model.SubscriptionTransitionException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

/** Exercises [SubscriptionRepositoryImpl] against a real in-memory Room database. */
@RunWith(RobolectricTestRunner::class)
class SubscriptionRepositoryRoomTest {
  private val today = LocalDate.of(2024, 5, 20)
  private val clock = Clock.fixed(Instant.parse("2024-05-20T09:00:00Z"), ZoneOffset.UTC)
  private lateinit var database: SubScribeDatabase
  private lateinit var repository: SubscriptionRepositoryImpl

  @Before
  fun setUp() {
    database = inMemorySubScribeDatabase()
    repository = SubscriptionRepositoryImpl(database.subscriptionDao(), clock)
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun createSubscription_persistsAndDefaultsMissingStartDate() =
    runTest {
      val created = repository.createSubscription(draft())

      val reloaded = repository.getSubscription(created.id)
      assertEquals(today, reloaded?.startDate)
      assertEquals(SubscriptionStatus.ACTIVE, reloaded?.status)
      assertEquals(created.createdAt, reloaded?.createdAt)
    }

  @Test
  fun observeSubscriptions_ordersByNameCaseInsensitively() =
    runTest {
      repository.createSubscription(draft(name = "spotify"))
      repository.createSubscription(draft(name = "Apple Music"))

      val names = repository.observeSubscriptions().first().map { it.name }

      assertEquals(listOf("Apple Music", "spotify"), names)
    }

  @Test
  fun cancelThenArchiveThenDelete_persistsEachTransition() =
    runTest {
      val created = repository.createSubscription(draft())

      val cancelled = repository.cancelSubscription(created.id)
      assertEquals(SubscriptionStatus.CANCELLED, cancelled.status)
      assertEquals(today, cancelled.endDate)
      assertEquals(SubscriptionStatus.CANCELLED, repository.getSubscription(created.id)?.status)

      repository.archiveSubscription(created.id)
      assertEquals(SubscriptionStatus.ARCHIVED, repository.getSubscription(created.id)?.status)

      repository.deleteSubscription(created.id)
      assertNull(repository.getSubscription(created.id))
    }

  @Test
  fun archive_rejectsActiveSubscriptionAgainstRealDatabase() =
    runTest {
      val created = repository.createSubscription(draft())

      val error =
        runCatching { repository.archiveSubscription(created.id) }.exceptionOrNull()

      assertTrue(error is SubscriptionTransitionException.CannotArchive)
      assertEquals(SubscriptionStatus.ACTIVE, repository.getSubscription(created.id)?.status)
    }

  @Test
  fun setTags_replacesLinksInRealJoinTable() =
    runTest {
      val created = repository.createSubscription(draft())
      val first = TagEntity(id = UUID.randomUUID(), name = "First", color = null)
      val second = TagEntity(id = UUID.randomUUID(), name = "Second", color = null)
      database.tagDao().upsert(first)
      database.tagDao().upsert(second)

      repository.setTags(created.id, setOf(first.id, second.id))
      repository.setTags(created.id, setOf(second.id))

      assertEquals(listOf(second.id), repository.observeTagIds(created.id).first())
    }

  @Test
  fun deleteSubscription_cascadesTagAssignments() =
    runTest {
      val created = repository.createSubscription(draft())
      val tag = TagEntity(id = UUID.randomUUID(), name = "Streaming", color = null)
      database.tagDao().upsert(tag)
      database.subscriptionDao().insertTags(
        listOf(SubscriptionTagEntity(subscriptionId = created.id, tagId = tag.id)),
      )
      repository.cancelSubscription(created.id)
      repository.archiveSubscription(created.id)

      repository.deleteSubscription(created.id)

      assertTrue(database.subscriptionDao().getAllTagAssignments().isEmpty())
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
