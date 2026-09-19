package az21.subscribe.data.repository

import az21.subscribe.data.local.SubScribeDatabase
import az21.subscribe.data.local.entity.SubscriptionEntity
import az21.subscribe.data.local.entity.SubscriptionTagEntity
import az21.subscribe.data.local.inMemorySubScribeDatabase
import az21.subscribe.domain.model.BillingCycle
import az21.subscribe.domain.model.SubscriptionStatus
import kotlinx.coroutines.flow.first
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
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

/** Exercises [TagRepositoryImpl] against a real in-memory Room database. */
@RunWith(RobolectricTestRunner::class)
class TagRepositoryRoomTest {
  private val clock = Clock.fixed(Instant.parse("2024-05-20T09:00:00Z"), ZoneOffset.UTC)
  private lateinit var database: SubScribeDatabase
  private lateinit var repository: TagRepositoryImpl

  @Before
  fun setUp() {
    database = inMemorySubScribeDatabase()
    repository = TagRepositoryImpl(database.tagDao())
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun createAndUpdate_persistThroughRealDatabase() =
    runTest {
      val created = repository.createTag("Streaming", color = 0x112233)

      assertEquals("Streaming", repository.getTag(created.id)?.name)

      repository.updateTag(created.copy(name = "Media"))

      assertEquals("Media", repository.getTag(created.id)?.name)
    }

  @Test
  fun delete_removesTag() =
    runTest {
      val created = repository.createTag("Streaming", color = null)

      repository.deleteTag(created.id)

      assertNull(repository.getTag(created.id))
    }

  @Test
  fun observeTags_ordersByNameCaseInsensitively() =
    runTest {
      repository.createTag("work", color = null)
      repository.createTag("Apple", color = null)

      assertEquals(listOf("Apple", "work"), repository.observeTags().first().map { it.name })
    }

  @Test
  fun observeTagsForSubscription_joinsOnlyAttachedTags() =
    runTest {
      val subscriptionId = insertSubscription()
      val attached = repository.createTag("Streaming", color = null)
      repository.createTag("Work", color = null)
      database.subscriptionDao().insertTags(
        listOf(SubscriptionTagEntity(subscriptionId = subscriptionId, tagId = attached.id)),
      )

      val tags = repository.observeTagsForSubscription(subscriptionId).first()

      assertEquals(listOf("Streaming"), tags.map { it.name })
    }

  @Test
  fun observeTagsBySubscription_groupsAttachedTagsBySubscription() =
    runTest {
      val first = insertSubscription()
      val second = insertSubscription()
      val streaming = repository.createTag("Streaming", color = null)
      val work = repository.createTag("Work", color = null)
      database.subscriptionDao().insertTags(
        listOf(
          SubscriptionTagEntity(subscriptionId = first, tagId = streaming.id),
          SubscriptionTagEntity(subscriptionId = second, tagId = streaming.id),
          SubscriptionTagEntity(subscriptionId = second, tagId = work.id),
        ),
      )

      val grouped = repository.observeTagsBySubscription().first()

      assertEquals(listOf("Streaming"), grouped.getValue(first).map { it.name })
      assertEquals(listOf("Streaming", "Work"), grouped.getValue(second).map { it.name })
    }

  @Test
  fun observeTagsBySubscription_isEmptyWithoutAssignments() =
    runTest {
      repository.createTag("Streaming", color = null)

      assertTrue(repository.observeTagsBySubscription().first().isEmpty())
    }

  private fun insertSubscription(): UUID {
    val id = UUID.randomUUID()
    val now = clock.instant()
    val entity =
      SubscriptionEntity(
        id = id,
        name = "Netflix $id",
        iconId = "netflix",
        startDate = LocalDate.of(2023, 1, 1),
        billingCycle = BillingCycle.MONTHLY,
        status = SubscriptionStatus.ACTIVE,
        endDate = null,
        reminderDaysBefore = null,
        paymentMethodId = null,
        notes = null,
        createdAt = now,
        updatedAt = now,
      )
    runBlocking { database.subscriptionDao().upsert(entity) }
    return id
  }
}
