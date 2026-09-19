package az21.subscribe.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.ImportExport
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import az21.subscribe.R
import az21.subscribe.ui.common.SubScribeTopAppBar
import az21.subscribe.ui.common.segmentedListItemColors
import az21.subscribe.ui.navigation.SubScribeFloatingToolbar
import az21.subscribe.ui.navigation.TopLevelDestination
import az21.subscribe.ui.theme.AppTheme

@Composable
fun SettingsScreen(
  onOpenGeneral: () -> Unit,
  onOpenTheme: () -> Unit,
  onOpenTags: () -> Unit,
  onOpenPaymentMethods: () -> Unit,
  onOpenDataTransfer: () -> Unit,
  onNavigateTopLevel: (NavKey) -> Unit,
  modifier: Modifier = Modifier,
) {
  SettingsContent(
    onOpenGeneral = onOpenGeneral,
    onOpenTheme = onOpenTheme,
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
  onOpenGeneral: () -> Unit,
  onOpenTheme: () -> Unit,
  onOpenTags: () -> Unit,
  onOpenPaymentMethods: () -> Unit,
  onOpenDataTransfer: () -> Unit,
  onNavigateTopLevel: (NavKey) -> Unit,
  modifier: Modifier = Modifier,
) {
  val entries =
    listOf(
      SettingsEntry(R.string.settings_general, Icons.Outlined.Tune, onOpenGeneral),
      SettingsEntry(R.string.settings_theme, Icons.Outlined.Palette, onOpenTheme),
      SettingsEntry(R.string.settings_tags, Icons.AutoMirrored.Outlined.Label, onOpenTags),
      SettingsEntry(R.string.settings_payment_methods, Icons.Outlined.CreditCard, onOpenPaymentMethods),
      SettingsEntry(R.string.settings_import_export, Icons.Outlined.ImportExport, onOpenDataTransfer),
    )

  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  Scaffold(
    modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      SubScribeTopAppBar(title = stringResource(R.string.settings_title), scrollBehavior = scrollBehavior)
    },
    floatingActionButtonPosition = FabPosition.Center,
    floatingActionButton = {
      SubScribeFloatingToolbar(
        selectedDestination = TopLevelDestination.SETTINGS,
        onNavigateTopLevel = onNavigateTopLevel,
      )
    },
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
      itemsIndexed(items = entries) { index, entry ->
        SettingsListRow(
          entry = entry,
          shapes = ListItemDefaults.segmentedShapes(index = index, count = entries.size),
        )
      }
    }
  }
}

@Composable
private fun SettingsListRow(
  entry: SettingsEntry,
  shapes: ListItemShapes,
) {
  SegmentedListItem(
    onClick = entry.onClick,
    shapes = shapes,
    colors = segmentedListItemColors(),
    leadingContent = { Icon(imageVector = entry.icon, contentDescription = null) },
    trailingContent = { NavigationChevron() },
    content = { Text(text = stringResource(entry.labelRes)) },
  )
}

private data class SettingsEntry(
  @param:StringRes val labelRes: Int,
  val icon: ImageVector,
  val onClick: () -> Unit,
)

@Preview(showBackground = true)
@Composable
private fun SettingsContentPreview() {
  AppTheme {
    SettingsContent(
      onOpenGeneral = {},
      onOpenTheme = {},
      onOpenTags = {},
      onOpenPaymentMethods = {},
      onOpenDataTransfer = {},
      onNavigateTopLevel = {},
    )
  }
}
