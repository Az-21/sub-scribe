package az21.subscribe.ui.settings

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import az21.subscribe.MainDispatcherRule
import az21.subscribe.data.fake.FakeSettingsRepository
import az21.subscribe.domain.model.Currency
import az21.subscribe.domain.model.ThemeMode
import az21.subscribe.domain.model.ThemeVariant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  private val settingsRepository = FakeSettingsRepository()

  @Test
  fun exposesPersistedSettings() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = SettingsViewModel(settingsRepository)

      viewModel.uiState.test {
        val state = filterLoaded()

        assertEquals(Currency.DEFAULT, state.settings.currency)
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun setCurrency_persistsSelection() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = SettingsViewModel(settingsRepository)

      viewModel.uiState.test {
        filterLoaded()
        viewModel.setCurrency(Currency.EUR)

        val state = awaitItem()
        assertEquals(Currency.EUR, state.settings.currency)
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun setThemeVariantAndMode_persistSelections() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = SettingsViewModel(settingsRepository)

      viewModel.uiState.test {
        filterLoaded()
        viewModel.setThemeVariant(ThemeVariant.VIBRANT)
        viewModel.setThemeMode(ThemeMode.DARK)

        var state = awaitItem()
        while (state.settings.themeVariant != ThemeVariant.VIBRANT || state.settings.themeMode != ThemeMode.DARK) {
          state = awaitItem()
        }
        assertEquals(ThemeVariant.VIBRANT, state.settings.themeVariant)
        assertEquals(ThemeMode.DARK, state.settings.themeMode)
        cancelAndIgnoreRemainingEvents()
      }
    }

  private suspend fun ReceiveTurbine<SettingsUiState>.filterLoaded(): SettingsUiState {
    var state = awaitItem()
    while (state.isLoading) {
      state = awaitItem()
    }
    return state
  }
}
