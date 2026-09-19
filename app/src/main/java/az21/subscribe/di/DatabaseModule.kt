package az21.subscribe.di

import android.content.Context
import androidx.room.Room
import az21.subscribe.data.local.SubScribeDatabase
import az21.subscribe.data.local.dao.PaymentMethodDao
import az21.subscribe.data.local.dao.PriceHistoryDao
import az21.subscribe.data.local.dao.SubscriptionDao
import az21.subscribe.data.local.dao.TagDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
  @Provides
  @Singleton
  fun provideDatabase(
    @ApplicationContext context: Context,
  ): SubScribeDatabase = Room.databaseBuilder(context, SubScribeDatabase::class.java, SubScribeDatabase.NAME).build()

  @Provides
  fun provideSubscriptionDao(database: SubScribeDatabase): SubscriptionDao = database.subscriptionDao()

  @Provides
  fun providePriceHistoryDao(database: SubScribeDatabase): PriceHistoryDao = database.priceHistoryDao()

  @Provides
  fun provideTagDao(database: SubScribeDatabase): TagDao = database.tagDao()

  @Provides
  fun providePaymentMethodDao(database: SubScribeDatabase): PaymentMethodDao = database.paymentMethodDao()
}
