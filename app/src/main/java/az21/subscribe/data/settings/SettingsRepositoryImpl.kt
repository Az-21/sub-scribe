package az21.subscribe.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import az21.subscribe.domain.model.AppSettings
import az21.subscribe.domain.model.Currency
import az21.subscribe.domain.model.ThemeMode
import az21.subscribe.domain.model.ThemeSeedSource
import az21.subscribe.domain.model.ThemeVariant
import az21.subscribe.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepositoryImpl
  @Inject
  constructor(
    @param:ApplicationContext private val context: Context,
  ) : SettingsRepository {
    override val settings: Flow<AppSettings> =
      context.settingsDataStore.data
        .catch { error ->
          if (error is IOException) emit(emptyPreferences()) else throw error
        }.map { preferences ->
          AppSettings(
            currency = Currency.fromCode(preferences[Keys.CURRENCY]),
            themeSeedSource = preferences[Keys.SEED_SOURCE].toEnumOr(ThemeSeedSource.SYSTEM),
            themeSeedColor = preferences[Keys.SEED_COLOR],
            themeVariant = preferences[Keys.VARIANT].toEnumOr(ThemeVariant.TONAL_SPOT),
            themeMode = preferences[Keys.MODE].toEnumOr(ThemeMode.SYSTEM),
          )
        }

    override suspend fun setCurrency(currency: Currency) {
      context.settingsDataStore.edit { it[Keys.CURRENCY] = currency.code }
    }

    override suspend fun setThemeSeedSource(source: ThemeSeedSource) {
      context.settingsDataStore.edit { it[Keys.SEED_SOURCE] = source.name }
    }

    override suspend fun setThemeSeedColor(color: Int?) {
      context.settingsDataStore.edit { preferences ->
        if (color == null) preferences.remove(Keys.SEED_COLOR) else preferences[Keys.SEED_COLOR] = color
      }
    }

    override suspend fun setThemeVariant(variant: ThemeVariant) {
      context.settingsDataStore.edit { it[Keys.VARIANT] = variant.name }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
      context.settingsDataStore.edit { it[Keys.MODE] = mode.name }
    }

    private object Keys {
      val CURRENCY = stringPreferencesKey("currency_code")
      val SEED_SOURCE = stringPreferencesKey("theme_seed_source")
      val SEED_COLOR = intPreferencesKey("theme_seed_color")
      val VARIANT = stringPreferencesKey("theme_variant")
      val MODE = stringPreferencesKey("theme_mode")
    }
  }

private inline fun <reified T : Enum<T>> String?.toEnumOr(default: T): T =
  this?.let { value -> enumValues<T>().firstOrNull { it.name == value } } ?: default
