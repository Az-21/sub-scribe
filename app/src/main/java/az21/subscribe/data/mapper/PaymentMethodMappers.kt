package az21.subscribe.data.mapper

import az21.subscribe.data.local.entity.PaymentMethodEntity
import az21.subscribe.domain.model.PaymentMethod

fun PaymentMethodEntity.toDomain(): PaymentMethod = PaymentMethod(id = id, label = label)

fun PaymentMethod.toEntity(): PaymentMethodEntity = PaymentMethodEntity(id = id, label = label)
