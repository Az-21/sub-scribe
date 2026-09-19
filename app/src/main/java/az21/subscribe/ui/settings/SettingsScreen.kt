package az21.subscribe.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import az21.subscribe.R
import az21.subscribe.domain.model.Currency
import az21.subscribe.domain.model.ThemeMode
import az21.subscribe.domain.model.ThemeSeedSource
import az21.subscribe.domain.model.ThemeVariant
import az21.subscribe.ui.common.ColorPickerDialog
import az21.subscribe.ui.common.PresetColors
import az21.subscribe.ui.navigation.SettingsRoute
import az21.subscribe.ui.navigation.SubScribeBottomBar
import az21.subscribe.ui.theme.AppTheme
import az21.subscribe.ui.theme.FallbackSeedColor

@Composable
fun SettingsScreen(
  onOpenTags: () -> Unit,
  onOpenPaymentMethods: () -> Unit,
  onOpenDataTransfer: () -> Unit,
  onNavigateTopLevel: (NavKey) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: SettingsViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  SettingsContent(
    uiState = uiState,
    onCurrencyChange = viewModel::setCurrency,
    onSeedSourceChange = viewModel::setThemeSeedSource,
    onSeedColorChange = viewModel::setThemeSeedColor,
    onVariantChange = viewModel::setThemeVariant,
    onModeChange = viewModel::setThemeMode,
    onOpenTags = onOpenTags,
    onOpenPaymentMethods = onOpenPaymentMethods,
    onOpenDataTransfer = onOpenDataTransfer,
    onNavigateTopLevel = onNavigateTopLevel,
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
  uiState: SettingsUiState,
  onCurrencyChange: (Currency) -> Unit,
  onSeedSourceChange: (ThemeSeedSource) -> Unit,
  onSeedColorChange: (Int?) -> Unit,
  onVariantChange: (ThemeVariant) -> Unit,
  onModeChange: (ThemeMode) -> Unit,
  onOpenTags: () -> Unit,
  onOpenPaymentMethods: () -> Unit,
  onOpenDataTransfer: () -> Unit,
  onNavigateTopLevel: (NavKey) -> Unit,
  modifier: Modifier = Modifier,
) {
  val settings = uiState.settings
  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
    bottomBar = { SubScribeBottomBar(currentRoute = SettingsRoute, onSelect = onNavigateTopLevel) },
  ) { innerPadding ->
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      CurrencySetting(selected = settings.currency, onCurrencyChange = onCurrencyChange)

      SeedSourceSetting(selected = settings.themeSeedSource, onSeedSourceChange = onSeedSourceChange)

      if (settings.themeSeedSource == ThemeSeedSource.MANUAL) {
        SeedColorSetting(selected = settings.themeSeedColor, onSeedColorChange = onSeedColorChange)
      }

      VariantSetting(selected = settings.themeVariant, onVariantChange = onVariantChange)

      ModeSetting(selected = settings.themeMode, onModeChange = onModeChange)

      Card(modifier = Modifier.fillMaxWidth()) {
        Column {
          ListItem(
            headlineContent = { Text(stringResource(R.string.settings_tags)) },
            modifier = Modifier.clickable(onClick = onOpenTags),
          )
          ListItem(
            headlineContent = { Text(stringResource(R.string.settings_payment_methods)) },
            modifier = Modifier.clickable(onClick = onOpenPaymentMethods),
          )
        }
      }

      Card(modifier = Modifier.fillMaxWidth()) {
        ListItem(
          headlineContent = { Text(stringResource(R.string.transfer_title)) },
          supportingContent = { Text(stringResource(R.string.transfer_export_description)) },
          modifier = Modifier.clickable(onClick = onOpenDataTransfer),
        )
      }
    }
  }
}

@Composable
private fun SettingTitle(text: String) {
  Text(text = text, style = MaterialTheme.typography.titleSmall)
}

@Composable
private fun DropdownSetting(
  label: String,
  selectedLabel: String,
  options: List<Pair<String, () -> Unit>>,
) {
  var expanded by remember { mutableStateOf(false) }
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    SettingTitle(label)
    Box {
      OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
        Text(selectedLabel, modifier = Modifier.weight(1f))
        Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null)
      }
      DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        options.forEach { (optionLabel, onSelect) ->
          DropdownMenuItem(
            text = { Text(optionLabel) },
            onClick = {
              onSelect()
              expanded = false
            },
          )
        }
      }
    }
  }
}

@Composable
private fun CurrencySetting(
  selected: Currency,
  onCurrencyChange: (Currency) -> Unit,
) {
  DropdownSetting(
    label = stringResource(R.string.settings_currency),
    selectedLabel = "${selected.code} (${selected.symbol})",
    options = Currency.entries.map { currency -> currency.code to { onCurrencyChange(currency) } },
  )
}

@Composable
private fun SeedSourceSetting(
  selected: ThemeSeedSource,
  onSeedSourceChange: (ThemeSeedSource) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    SettingTitle(stringResource(R.string.settings_theme_source))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      ThemeSeedSource.entries.forEach { source ->
        FilterChip(
          selected = selected == source,
          onClick = { onSeedSourceChange(source) },
          label = {
            Text(
              stringResource(
                if (source == ThemeSeedSource.SYSTEM) {
                  R.string.settings_theme_source_system
                } else {
                  R.string.settings_theme_source_manual
                },
              ),
            )
          },
        )
      }
    }
  }
}

@Composable
private fun SeedColorSetting(
  selected: Int?,
  onSeedColorChange: (Int?) -> Unit,
) {
  var showPicker by remember { mutableStateOf(false) }
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    SettingTitle(stringResource(R.string.settings_theme_color))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      PresetColors.forEach { color ->
        Box(
          modifier =
            Modifier
              .size(32.dp)
              .background(Color(color), CircleShape)
              .border(
                width = if (selected == color) 3.dp else 0.dp,
                color = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape,
              ).clickable { onSeedColorChange(color) },
        )
      }
    }
    OutlinedButton(onClick = { showPicker = true }) {
      Box(
        modifier =
          Modifier
            .size(20.dp)
            .background(Color(selected ?: FallbackSeedColor), CircleShape)
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outline, shape = CircleShape),
      )
      Text(
        text = stringResource(R.string.settings_theme_color_custom),
        modifier = Modifier.padding(start = 8.dp),
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
private fun VariantSetting(
  selected: ThemeVariant,
  onVariantChange: (ThemeVariant) -> Unit,
) {
  DropdownSetting(
    label = stringResource(R.string.settings_theme_variant),
    selectedLabel = stringResource(selected.labelRes()),
    options =
      ThemeVariant.entries.map { variant ->
        stringResource(variant.labelRes()) to { onVariantChange(variant) }
      },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeSetting(
  selected: ThemeMode,
  onModeChange: (ThemeMode) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    SettingTitle(stringResource(R.string.settings_theme_mode))
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
      ThemeMode.entries.forEachIndexed { index, mode ->
        SegmentedButton(
          selected = selected == mode,
          onClick = { onModeChange(mode) },
          shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemeMode.entries.size),
        ) {
          Text(stringResource(mode.labelRes()), maxLines = 1)
        }
      }
    }
  }
}

private fun ThemeVariant.labelRes(): Int =
  when (this) {
    ThemeVariant.TONAL_SPOT -> R.string.variant_tonal_spot
    ThemeVariant.NEUTRAL -> R.string.variant_neutral
    ThemeVariant.VIBRANT -> R.string.variant_vibrant
    ThemeVariant.EXPRESSIVE -> R.string.variant_expressive
  }

private fun ThemeMode.labelRes(): Int =
  when (this) {
    ThemeMode.SYSTEM -> R.string.settings_theme_mode_system
    ThemeMode.LIGHT -> R.string.settings_theme_mode_light
    ThemeMode.DARK -> R.string.settings_theme_mode_dark
  }

@Preview(showBackground = true)
@Composable
private fun SettingsContentPreview() {
  AppTheme {
    SettingsContent(
      uiState = SettingsUiState(isLoading = false),
      onCurrencyChange = {},
      onSeedSourceChange = {},
      onSeedColorChange = {},
      onVariantChange = {},
      onModeChange = {},
      onOpenTags = {},
      onOpenPaymentMethods = {},
      onOpenDataTransfer = {},
      onNavigateTopLevel = {},
    )
  }
}
