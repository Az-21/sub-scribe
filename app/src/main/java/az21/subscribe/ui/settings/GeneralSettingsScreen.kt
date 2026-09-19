package az21.subscribe.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az21.subscribe.R
import az21.subscribe.domain.model.Currency
import az21.subscribe.ui.common.SubScribeTopAppBar
import az21.subscribe.ui.common.segmentedListItemColors
import az21.subscribe.ui.theme.AppTheme

@Composable
fun GeneralSettingsScreen(
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: SettingsViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  GeneralSettingsContent(
    uiState = uiState,
    onBack = onBack,
    onCurrencyChange = viewModel::setCurrency,
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsContent(
  uiState: SettingsUiState,
  onBack: () -> Unit,
  onCurrencyChange: (Currency) -> Unit,
  modifier: Modifier = Modifier,
) {
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  Scaffold(
    modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      SubScribeTopAppBar(
        title = stringResource(R.string.settings_general),
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
      CurrencyPicker(
        selected = uiState.settings.currency,
        onCurrencyChange = onCurrencyChange,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyPicker(
  selected: Currency,
  onCurrencyChange: (Currency) -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }
  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = it },
    modifier = Modifier.fillMaxWidth(),
  ) {
    SegmentedListItem(
      shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
      colors = segmentedListItemColors(),
      modifier =
        Modifier
          .fillMaxWidth()
          .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
      content = { Text(text = stringResource(R.string.settings_currency)) },
      trailingContent = { Text(text = selected.displayName()) },
    )
    ExposedDropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
    ) {
      Currency.entries.forEachIndexed { index, currency ->
        SelectableDropdownMenuItem(
          selected = currency == selected,
          onClick = {
            onCurrencyChange(currency)
            expanded = false
          },
          text = { Text(text = currency.displayName()) },
          shapes = MenuDefaults.itemShape(index, Currency.entries.size),
        )
      }
    }
  }
}

private fun Currency.displayName(): String = "$code ($symbol)"

@Preview(showBackground = true)
@Composable
private fun GeneralSettingsContentPreview() {
  AppTheme {
    GeneralSettingsContent(
      uiState = SettingsUiState(isLoading = false),
      onBack = {},
      onCurrencyChange = {},
    )
  }
}
