package az21.subscribe.data.repository

import az21.subscribe.data.local.dao.TagDao
import az21.subscribe.data.local.entity.TagEntity
import az21.subscribe.data.mapper.toDomain
import az21.subscribe.data.mapper.toEntity
import az21.subscribe.domain.model.Tag
import az21.subscribe.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepositoryImpl
  @Inject
  constructor(
    private val tagDao: TagDao,
  ) : TagRepository {
    override fun observeTags(): Flow<List<Tag>> =
      tagDao.observeAll().map { entities -> entities.map(TagEntity::toDomain) }

    override fun observeTagsForSubscription(subscriptionId: UUID): Flow<List<Tag>> =
      tagDao.observeForSubscription(subscriptionId).map { entities -> entities.map(TagEntity::toDomain) }

    override suspend fun getTag(id: UUID): Tag? = tagDao.getById(id)?.toDomain()

    override suspend fun createTag(
      name: String,
      color: Int?,
    ): Tag {
      val entity = TagEntity(id = UUID.randomUUID(), name = name, color = color)
      tagDao.upsert(entity)
      return entity.toDomain()
    }

    override suspend fun updateTag(tag: Tag) {
      tagDao.upsert(tag.toEntity())
    }

    override suspend fun deleteTag(id: UUID) {
      tagDao.deleteById(id)
    }
  }
