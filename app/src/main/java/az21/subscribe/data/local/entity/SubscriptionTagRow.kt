package az21.subscribe.data.local.entity

import androidx.room.ColumnInfo
import java.util.UUID

/** A flattened subscription/tag pairing, used to build per-subscription tag maps in one query. */
data class SubscriptionTagRow(
  @ColumnInfo(name = "subscription_id") val subscriptionId: UUID,
  @ColumnInfo(name = "tag_id") val tagId: UUID,
  @ColumnInfo(name = "name") val name: String,
  @ColumnInfo(name = "color") val color: Int?,
)
