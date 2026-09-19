package az21.subscribe.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import az21.subscribe.domain.model.BillingCycle
import az21.subscribe.domain.model.SubscriptionStatus
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity(
  tableName = "subscriptions",
  foreignKeys = [
    ForeignKey(
      entity = PaymentMethodEntity::class,
      parentColumns = ["id"],
      childColumns = ["payment_method_id"],
      onDelete = ForeignKey.SET_NULL,
    ),
  ],
  indices = [Index("payment_method_id")],
)
data class SubscriptionEntity(
  @PrimaryKey @ColumnInfo(name = "id") val id: UUID,
  @ColumnInfo(name = "name") val name: String,
  @ColumnInfo(name = "icon_id") val iconId: String,
  @ColumnInfo(name = "start_date") val startDate: LocalDate,
  @ColumnInfo(name = "billing_cycle") val billingCycle: BillingCycle,
  @ColumnInfo(name = "status") val status: SubscriptionStatus,
  @ColumnInfo(name = "end_date") val endDate: LocalDate?,
  @ColumnInfo(name = "reminder_days_before") val reminderDaysBefore: Int?,
  @ColumnInfo(name = "payment_method_id") val paymentMethodId: UUID?,
  @ColumnInfo(name = "notes") val notes: String?,
  @ColumnInfo(name = "created_at") val createdAt: Instant,
  @ColumnInfo(name = "updated_at") val updatedAt: Instant,
)
