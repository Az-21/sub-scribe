package az21.subscribe.domain.repository

import az21.subscribe.domain.model.Tag
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** CRUD for tags, plus lookup of the tags attached to a subscription. */
interface TagRepository {
  fun observeTags(): Flow<List<Tag>>

  fun observeTagsForSubscription(subscriptionId: UUID): Flow<List<Tag>>

  suspend fun getTag(id: UUID): Tag?

  suspend fun createTag(
    name: String,
    color: Int? = null,
  ): Tag

  suspend fun updateTag(tag: Tag)

  suspend fun deleteTag(id: UUID)
}
