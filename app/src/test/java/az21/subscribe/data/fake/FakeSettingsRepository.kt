package az21.subscribe.data.fake

import az21.subscribe.domain.model.AppSettings
import az21.subscribe.domain.model.Currency
import az21.subscribe.domain.model.ThemeMode
import az21.subscribe.domain.model.ThemeSeedSource
import az21.subscribe.domain.model.ThemeVariant
import az21.subscribe.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [SettingsRepository] for JVM-only tests. */
class FakeSettingsRepository(
  initial: AppSettings = AppSettings(),
) : SettingsRepository {
  private val state = MutableStateFlow(initial)

  override val settings: Flow<AppSettings> = state

  override suspend fun setCurrency(currency: Currency) {
    state.value = state.value.copy(currency = currency)
  }

  override suspend fun setThemeSeedSource(source: ThemeSeedSource) {
    state.value = state.value.copy(themeSeedSource = source)
  }

  override suspend fun setThemeSeedColor(color: Int?) {
    state.value = state.value.copy(themeSeedColor = color)
  }

  override suspend fun setThemeVariant(variant: ThemeVariant) {
    state.value = state.value.copy(themeVariant = variant)
  }

  override suspend fun setThemeMode(mode: ThemeMode) {
    state.value = state.value.copy(themeMode = mode)
  }
}
