package az21.subscribe

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import az21.subscribe.data.reminder.ReminderNotifier
import az21.subscribe.di.ApplicationScope
import az21.subscribe.domain.usecase.RescheduleAllRemindersUseCase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SubScribeApplication :
  Application(),
  Configuration.Provider {
  @Inject lateinit var workerFactory: HiltWorkerFactory

  @Inject lateinit var rescheduleAllReminders: RescheduleAllRemindersUseCase

  @Inject @ApplicationScope
  lateinit var applicationScope: CoroutineScope

  override val workManagerConfiguration: Configuration
    get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

  override fun onCreate() {
    super.onCreate()
    ReminderNotifier.createChannel(this)
    applicationScope.launch { runCatching { rescheduleAllReminders() } }
  }
}
