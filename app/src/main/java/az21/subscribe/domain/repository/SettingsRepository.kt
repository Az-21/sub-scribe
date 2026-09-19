package az21.subscribe.domain.repository

import az21.subscribe.domain.model.AppSettings
import az21.subscribe.domain.model.Currency
import az21.subscribe.domain.model.ThemeMode
import az21.subscribe.domain.model.ThemeSeedSource
import az21.subscribe.domain.model.ThemeVariant
import kotlinx.coroutines.flow.Flow

/** Persistent user preferences: currency and theme controls. */
interface SettingsRepository {
  val settings: Flow<AppSettings>

  suspend fun setCurrency(currency: Currency)

  suspend fun setThemeSeedSource(source: ThemeSeedSource)

  suspend fun setThemeSeedColor(color: Int?)

  suspend fun setThemeVariant(variant: ThemeVariant)

  suspend fun setThemeMode(mode: ThemeMode)
}
