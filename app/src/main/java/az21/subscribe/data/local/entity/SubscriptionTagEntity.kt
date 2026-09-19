package az21.subscribe.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.util.UUID

@Entity(
  tableName = "subscription_tags",
  primaryKeys = ["subscription_id", "tag_id"],
  foreignKeys = [
    ForeignKey(
      entity = SubscriptionEntity::class,
      parentColumns = ["id"],
      childColumns = ["subscription_id"],
      onDelete = ForeignKey.CASCADE,
    ),
    ForeignKey(
      entity = TagEntity::class,
      parentColumns = ["id"],
      childColumns = ["tag_id"],
      onDelete = ForeignKey.CASCADE,
    ),
  ],
  indices = [Index("tag_id")],
)
data class SubscriptionTagEntity(
  @ColumnInfo(name = "subscription_id") val subscriptionId: UUID,
  @ColumnInfo(name = "tag_id") val tagId: UUID,
)
