package az21.subscribe.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TimeModule {
  /** Injectable clock so repositories are deterministic in tests. */
  @Provides
  @Singleton
  fun provideClock(): Clock = Clock.systemDefaultZone()
}
