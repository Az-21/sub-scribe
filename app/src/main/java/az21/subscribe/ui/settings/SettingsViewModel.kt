package az21.subscribe.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az21.subscribe.domain.model.AppSettings
import az21.subscribe.domain.model.Currency
import az21.subscribe.domain.model.ThemeMode
import az21.subscribe.domain.model.ThemeSeedSource
import az21.subscribe.domain.model.ThemeVariant
import az21.subscribe.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Immutable state for the settings screen. */
data class SettingsUiState(
  val settings: AppSettings = AppSettings(),
  val isLoading: Boolean = true,
)

/** Drives the settings screen: currency and theme preferences. */
@HiltViewModel
class SettingsViewModel
  @Inject
  constructor(
    private val settingsRepository: SettingsRepository,
  ) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> =
      settingsRepository.settings
        .map { settings -> SettingsUiState(settings = settings, isLoading = false) }
        .stateIn(
          viewModelScope,
          SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
          SettingsUiState(),
        )

    fun setCurrency(currency: Currency) = update { settingsRepository.setCurrency(currency) }

    fun setThemeSeedSource(source: ThemeSeedSource) = update { settingsRepository.setThemeSeedSource(source) }

    fun setThemeSeedColor(color: Int?) = update { settingsRepository.setThemeSeedColor(color) }

    fun setThemeVariant(variant: ThemeVariant) = update { settingsRepository.setThemeVariant(variant) }

    fun setThemeMode(mode: ThemeMode) = update { settingsRepository.setThemeMode(mode) }

    private fun update(action: suspend () -> Unit) {
      viewModelScope.launch { action() }
    }

    private companion object {
      const val STOP_TIMEOUT_MILLIS = 5_000L
    }
  }
