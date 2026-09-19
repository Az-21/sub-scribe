package az21.subscribe.di

import az21.subscribe.data.repository.PaymentMethodRepositoryImpl
import az21.subscribe.data.repository.PriceHistoryRepositoryImpl
import az21.subscribe.data.repository.SubscriptionRepositoryImpl
import az21.subscribe.data.repository.TagRepositoryImpl
import az21.subscribe.data.settings.SettingsRepositoryImpl
import az21.subscribe.domain.repository.PaymentMethodRepository
import az21.subscribe.domain.repository.PriceHistoryRepository
import az21.subscribe.domain.repository.SettingsRepository
import az21.subscribe.domain.repository.SubscriptionRepository
import az21.subscribe.domain.repository.TagRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
  @Binds
  @Singleton
  abstract fun bindSubscriptionRepository(impl: SubscriptionRepositoryImpl): SubscriptionRepository

  @Binds
  @Singleton
  abstract fun bindPriceHistoryRepository(impl: PriceHistoryRepositoryImpl): PriceHistoryRepository

  @Binds
  @Singleton
  abstract fun bindTagRepository(impl: TagRepositoryImpl): TagRepository

  @Binds
  @Singleton
  abstract fun bindPaymentMethodRepository(impl: PaymentMethodRepositoryImpl): PaymentMethodRepository

  @Binds
  @Singleton
  abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
