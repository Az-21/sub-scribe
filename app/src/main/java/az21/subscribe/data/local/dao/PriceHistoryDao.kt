package az21.subscribe.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import az21.subscribe.data.local.entity.PriceHistoryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.UUID

@Dao
interface PriceHistoryDao {
  @Query(
    """
    SELECT * FROM price_history
    WHERE subscription_id = :subscriptionId
    ORDER BY effective_from_date DESC, created_at DESC
    """,
  )
  fun observeForSubscription(subscriptionId: UUID): Flow<List<PriceHistoryEntity>>

  @Query("SELECT * FROM price_history")
  fun observeAll(): Flow<List<PriceHistoryEntity>>

  @Query(
    """
    SELECT * FROM price_history
    WHERE subscription_id = :subscriptionId
    ORDER BY effective_from_date DESC, created_at DESC
    """,
  )
  suspend fun getForSubscription(subscriptionId: UUID): List<PriceHistoryEntity>

  @Query(
    """
    SELECT * FROM price_history
    WHERE subscription_id = :subscriptionId AND effective_from_date <= :date
    ORDER BY effective_from_date DESC, created_at DESC
    LIMIT 1
    """,
  )
  suspend fun getEffectiveOn(
    subscriptionId: UUID,
    date: LocalDate,
  ): PriceHistoryEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(entity: PriceHistoryEntity)

  @Query("DELETE FROM price_history WHERE id = :id")
  suspend fun deleteById(id: UUID)

  @Query("DELETE FROM price_history WHERE subscription_id = :subscriptionId")
  suspend fun deleteForSubscription(subscriptionId: UUID)
}
