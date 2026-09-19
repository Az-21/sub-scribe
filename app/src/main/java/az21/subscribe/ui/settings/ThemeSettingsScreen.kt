package az21.subscribe.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az21.subscribe.R
import az21.subscribe.domain.model.AppSettings
import az21.subscribe.domain.model.ThemeMode
import az21.subscribe.domain.model.ThemeSeedSource
import az21.subscribe.domain.model.ThemeVariant
import az21.subscribe.ui.common.ColorPicker
import az21.subscribe.ui.common.SubScribeTopAppBar
import az21.subscribe.ui.theme.AppTheme
import az21.subscribe.ui.theme.FallbackSeedColor

@Composable
fun ThemeSettingsScreen(
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: SettingsViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  ThemeSettingsContent(
    uiState = uiState,
    onBack = onBack,
    onSeedSourceChange = viewModel::setThemeSeedSource,
    onSeedColorChange = viewModel::setThemeSeedColor,
    onVariantChange = viewModel::setThemeVariant,
    onModeChange = viewModel::setThemeMode,
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsContent(
  uiState: SettingsUiState,
  onBack: () -> Unit,
  onSeedSourceChange: (ThemeSeedSource) -> Unit,
  onSeedColorChange: (Int?) -> Unit,
  onVariantChange: (ThemeVariant) -> Unit,
  onModeChange: (ThemeMode) -> Unit,
  modifier: Modifier = Modifier,
) {
  val settings = uiState.settings
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  Scaffold(
    modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      SubScribeTopAppBar(
        title = stringResource(R.string.settings_theme),
        onNavigateUp = onBack,
        scrollBehavior = scrollBehavior,
      )
    },
  ) { innerPadding ->
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
      ThemeOptionGroups(
        settings = settings,
        onModeChange = onModeChange,
        onVariantChange = onVariantChange,
        onSeedSourceChange = onSeedSourceChange,
      )

      if (settings.themeSeedSource == ThemeSeedSource.MANUAL) {
        SettingsSection(title = stringResource(R.string.settings_theme_color)) {
          SeedColorSetting(
            selected = settings.themeSeedColor,
            onSeedColorChange = onSeedColorChange,
          )
        }
      }
    }
  }
}

@Composable
private fun ThemeOptionGroups(
  settings: AppSettings,
  onModeChange: (ThemeMode) -> Unit,
  onVariantChange: (ThemeVariant) -> Unit,
  onSeedSourceChange: (ThemeSeedSource) -> Unit,
) {
  SettingsSection(title = stringResource(R.string.settings_theme_mode)) {
    ConnectedOptionGroup(
      options = ThemeMode.entries,
      selected = settings.themeMode,
      label = { stringResource(it.labelRes()) },
      onSelect = onModeChange,
      icon = { it.icon() },
    )
  }

  SettingsSection(title = stringResource(R.string.settings_theme_variant)) {
    ConnectedOptionGroup(
      options = ThemeVariant.entries,
      selected = settings.themeVariant,
      label = { stringResource(it.labelRes()) },
      onSelect = onVariantChange,
    )
  }

  SettingsSection(title = stringResource(R.string.settings_theme_source)) {
    ConnectedOptionGroup(
      options = ThemeSeedSource.entries,
      selected = settings.themeSeedSource,
      label = { stringResource(it.labelRes()) },
      onSelect = onSeedSourceChange,
      icon = { it.icon() },
    )
  }
}

@Composable
private fun SeedColorSetting(
  selected: Int?,
  onSeedColorChange: (Int?) -> Unit,
) {
  ColorPicker(
    selected = selected,
    fallbackColor = FallbackSeedColor,
    customContentDescription = stringResource(R.string.settings_theme_color_custom),
    presetContentDescription = stringResource(R.string.settings_theme_color),
    onColorChange = onSeedColorChange,
  )
}

@Preview(showBackground = true)
@Composable
private fun ThemeSettingsContentPreview() {
  AppTheme {
    ThemeSettingsContent(
      uiState = SettingsUiState(isLoading = false),
      onBack = {},
      onSeedSourceChange = {},
      onSeedColorChange = {},
      onVariantChange = {},
      onModeChange = {},
    )
  }
}
