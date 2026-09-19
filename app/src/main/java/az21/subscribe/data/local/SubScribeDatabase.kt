package az21.subscribe.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import az21.subscribe.data.local.dao.PaymentMethodDao
import az21.subscribe.data.local.dao.PriceHistoryDao
import az21.subscribe.data.local.dao.SubscriptionDao
import az21.subscribe.data.local.dao.TagDao
import az21.subscribe.data.local.entity.PaymentMethodEntity
import az21.subscribe.data.local.entity.PriceHistoryEntity
import az21.subscribe.data.local.entity.SubscriptionEntity
import az21.subscribe.data.local.entity.SubscriptionReminderEntity
import az21.subscribe.data.local.entity.SubscriptionTagEntity
import az21.subscribe.data.local.entity.TagEntity

@Database(
  entities = [
    SubscriptionEntity::class,
    SubscriptionReminderEntity::class,
    PriceHistoryEntity::class,
    TagEntity::class,
    SubscriptionTagEntity::class,
    PaymentMethodEntity::class,
  ],
  version = 1,
  exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class SubScribeDatabase : RoomDatabase() {
  abstract fun subscriptionDao(): SubscriptionDao

  abstract fun priceHistoryDao(): PriceHistoryDao

  abstract fun tagDao(): TagDao

  abstract fun paymentMethodDao(): PaymentMethodDao

  companion object {
    const val NAME = "sub-scribe.db"
  }
}
