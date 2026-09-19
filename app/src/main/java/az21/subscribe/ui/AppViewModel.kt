package az21.subscribe.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az21.subscribe.domain.model.AppSettings
import az21.subscribe.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Exposes app-wide settings so the root composable can drive the theme reactively. */
@HiltViewModel
class AppViewModel
  @Inject
  constructor(
    settingsRepository: SettingsRepository,
  ) : ViewModel() {
    val settings: StateFlow<AppSettings> =
      settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = AppSettings(),
      )

    private companion object {
      const val STOP_TIMEOUT_MILLIS = 5_000L
    }
  }
