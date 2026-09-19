package az21.subscribe.data.mapper

import az21.subscribe.data.local.entity.PriceHistoryEntity
import az21.subscribe.domain.model.PriceHistory

fun PriceHistoryEntity.toDomain(): PriceHistory =
  PriceHistory(
    id = id,
    subscriptionId = subscriptionId,
    price = price,
    effectiveFromDate = effectiveFromDate,
    createdAt = createdAt,
  )

fun PriceHistory.toEntity(): PriceHistoryEntity =
  PriceHistoryEntity(
    id = id,
    subscriptionId = subscriptionId,
    price = price,
    effectiveFromDate = effectiveFromDate,
    createdAt = createdAt,
  )
