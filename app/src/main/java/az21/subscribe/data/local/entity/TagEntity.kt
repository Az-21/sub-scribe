package az21.subscribe.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "tags")
data class TagEntity(
  @PrimaryKey @ColumnInfo(name = "id") val id: UUID,
  @ColumnInfo(name = "name") val name: String,
  @ColumnInfo(name = "color") val color: Int?,
)
