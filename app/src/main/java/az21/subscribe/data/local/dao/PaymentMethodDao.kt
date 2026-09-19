package az21.subscribe.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import az21.subscribe.data.local.entity.PaymentMethodEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface PaymentMethodDao {
  @Query("SELECT * FROM payment_methods ORDER BY label COLLATE NOCASE ASC")
  fun observeAll(): Flow<List<PaymentMethodEntity>>

  @Query("SELECT * FROM payment_methods ORDER BY label COLLATE NOCASE ASC")
  suspend fun getAll(): List<PaymentMethodEntity>

  @Query("SELECT * FROM payment_methods WHERE id = :id")
  suspend fun getById(id: UUID): PaymentMethodEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(entity: PaymentMethodEntity)

  @Upsert
  suspend fun upsertAll(items: List<PaymentMethodEntity>)

  @Query("DELETE FROM payment_methods WHERE id = :id")
  suspend fun deleteById(id: UUID)
}
