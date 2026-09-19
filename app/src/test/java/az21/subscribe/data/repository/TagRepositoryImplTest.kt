package az21.subscribe.data.repository

import az21.subscribe.data.fake.FakeTagDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class TagRepositoryImplTest {
  private val dao = FakeTagDao()
  private val repository = TagRepositoryImpl(dao)

  @Test
  fun observeTagsBySubscription_groupsAttachedTagsBySubscription() =
    runTest {
      val streaming = repository.createTag("Streaming")
      val work = repository.createTag("Work")
      val first = UUID.randomUUID()
      val second = UUID.randomUUID()
      dao.assignments.value =
        mapOf(
          first to setOf(streaming.id),
          second to setOf(streaming.id, work.id),
        )

      val grouped = repository.observeTagsBySubscription().first()

      assertEquals(setOf("Streaming"), grouped.getValue(first).map { it.name }.toSet())
      assertEquals(setOf("Streaming", "Work"), grouped.getValue(second).map { it.name }.toSet())
    }

  @Test
  fun observeTagsBySubscription_omitsSubscriptionsWithoutTags() =
    runTest {
      repository.createTag("Streaming")
      dao.assignments.value = emptyMap()

      assertEquals(
        emptyMap<UUID, List<az21.subscribe.domain.model.Tag>>(),
        repository.observeTagsBySubscription().first(),
      )
    }
}
