package az21.subscribe.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "payment_methods")
data class PaymentMethodEntity(
  @PrimaryKey @ColumnInfo(name = "id") val id: UUID,
  @ColumnInfo(name = "label") val label: String,
  @ColumnInfo(name = "color") val color: Int? = null,
)
