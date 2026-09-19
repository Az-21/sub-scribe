package az21.subscribe.di

import az21.subscribe.data.reminder.WorkManagerReminderScheduler
import az21.subscribe.domain.reminder.ReminderScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ReminderModule {
  @Binds
  @Singleton
  abstract fun bindReminderScheduler(impl: WorkManagerReminderScheduler): ReminderScheduler
}
