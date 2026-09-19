package az21.subscribe.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalTime
import java.util.UUID

@Entity(
  tableName = "subscription_reminders",
  foreignKeys = [
    ForeignKey(
      entity = SubscriptionEntity::class,
      parentColumns = ["id"],
      childColumns = ["subscription_id"],
      onDelete = ForeignKey.CASCADE,
    ),
  ],
  indices = [Index("subscription_id")],
)
data class SubscriptionReminderEntity(
  @PrimaryKey @ColumnInfo(name = "id") val id: UUID,
  @ColumnInfo(name = "subscription_id") val subscriptionId: UUID,
  @ColumnInfo(name = "days_before") val daysBefore: Int,
  @ColumnInfo(name = "time") val time: LocalTime,
)
