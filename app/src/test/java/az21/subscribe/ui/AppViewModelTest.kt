package az21.subscribe.ui

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import az21.subscribe.MainDispatcherRule
import az21.subscribe.data.fake.FakeSettingsRepository
import az21.subscribe.domain.model.AppSettings
import az21.subscribe.domain.model.Currency
import az21.subscribe.domain.model.ThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  @Test
  fun exposesPersistedSettings() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = AppViewModel(FakeSettingsRepository(AppSettings(themeMode = ThemeMode.DARK)))

      viewModel.settings.test {
        val state = awaitUntil { it.themeMode == ThemeMode.DARK }

        assertEquals(ThemeMode.DARK, state.themeMode)
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun reflectsSettingsUpdates() =
    runTest(mainDispatcherRule.testDispatcher) {
      val repository = FakeSettingsRepository()
      val viewModel = AppViewModel(repository)

      viewModel.settings.test {
        awaitItem()
        repository.setCurrency(Currency.EUR)

        val state = awaitUntil { it.currency == Currency.EUR }
        assertEquals(Currency.EUR, state.currency)
        cancelAndIgnoreRemainingEvents()
      }
    }

  private suspend fun ReceiveTurbine<AppSettings>.awaitUntil(predicate: (AppSettings) -> Boolean): AppSettings {
    var state = awaitItem()
    while (!predicate(state)) {
      state = awaitItem()
    }
    return state
  }
}
