package az21.subscribe.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity(
  tableName = "price_history",
  foreignKeys = [
    ForeignKey(
      entity = SubscriptionEntity::class,
      parentColumns = ["id"],
      childColumns = ["subscription_id"],
      onDelete = ForeignKey.CASCADE,
    ),
  ],
  indices = [Index("subscription_id", "effective_from_date")],
)
data class PriceHistoryEntity(
  @PrimaryKey @ColumnInfo(name = "id") val id: UUID,
  @ColumnInfo(name = "subscription_id") val subscriptionId: UUID,
  @ColumnInfo(name = "price") val price: BigDecimal,
  @ColumnInfo(name = "effective_from_date") val effectiveFromDate: LocalDate,
  @ColumnInfo(name = "created_at") val createdAt: Instant,
)
