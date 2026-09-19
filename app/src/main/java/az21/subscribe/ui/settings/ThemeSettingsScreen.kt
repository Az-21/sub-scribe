package az21.subscribe.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az21.subscribe.R
import az21.subscribe.domain.model.AppSettings
import az21.subscribe.domain.model.ThemeMode
import az21.subscribe.domain.model.ThemeSeedSource
import az21.subscribe.domain.model.ThemeVariant
import az21.subscribe.ui.common.ColorPickerDialog
import az21.subscribe.ui.common.PresetColors
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
  var showPicker by remember { mutableStateOf(false) }
  val customLabel = stringResource(R.string.settings_theme_color_custom)
  val presetLabel = stringResource(R.string.settings_theme_color)
  FlowRow(
    horizontalArrangement = Arrangement.spacedBy(ColorSwatchSpacing),
    verticalArrangement = Arrangement.spacedBy(ColorSwatchSpacing),
  ) {
    SeedColorSwatch(
      color = selected ?: FallbackSeedColor,
      selected = selected != null && selected !in PresetColors,
      onClickLabel = customLabel,
      onClick = { showPicker = true },
      badgeIcon = Icons.Outlined.Colorize,
      modifier = Modifier.size(ColorSwatchSize),
    )
    PresetColors.forEach { color ->
      SeedColorSwatch(
        color = color,
        selected = selected == color,
        onClickLabel = presetLabel,
        onClick = { onSeedColorChange(color) },
        modifier = Modifier.size(ColorSwatchSize),
      )
    }
  }
  if (showPicker) {
    ColorPickerDialog(
      initialColor = selected ?: FallbackSeedColor,
      onConfirm = { color ->
        onSeedColorChange(color)
        showPicker = false
      },
      onDismiss = { showPicker = false },
    )
  }
}

@Composable
private fun SeedColorSwatch(
  color: Int,
  selected: Boolean,
  onClickLabel: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  badgeIcon: ImageVector? = null,
) {
  Box(
    modifier =
      modifier
        .clip(CircleShape)
        .background(Color(color))
        .clickable(onClickLabel = onClickLabel, role = Role.RadioButton, onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    if (badgeIcon != null) {
      Box(
        modifier =
          Modifier
            .size(BadgeDiameter)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = BADGE_SCRIM_ALPHA), CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = badgeIcon,
          contentDescription = null,
          modifier = Modifier.size(BadgeIconSize),
          tint = MaterialTheme.colorScheme.onSurface,
        )
      }
    }
    if (selected) {
      Box(
        modifier =
          Modifier.matchParentSize().border(SelectionStroke, MaterialTheme.colorScheme.onSurface, CircleShape),
      )
    }
  }
}

private val ColorSwatchSize = 52.dp
private val ColorSwatchSpacing = 12.dp
private val SelectionStroke = 3.dp
private val BadgeDiameter = 24.dp
private val BadgeIconSize = 16.dp
private const val BADGE_SCRIM_ALPHA = 0.85f

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
