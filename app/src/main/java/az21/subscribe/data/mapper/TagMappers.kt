package az21.subscribe.data.mapper

import az21.subscribe.data.local.entity.TagEntity
import az21.subscribe.domain.model.Tag

fun TagEntity.toDomain(): Tag = Tag(id = id, name = name, color = color)

fun Tag.toEntity(): TagEntity = TagEntity(id = id, name = name, color = color)
