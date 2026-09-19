package az21.subscribe.ui.archive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import az21.subscribe.R
import az21.subscribe.domain.model.Currency
import az21.subscribe.domain.model.SubscriptionStatus
import az21.subscribe.ui.common.DeleteSubscriptionDialog
import az21.subscribe.ui.common.SubscriptionIcon
import az21.subscribe.ui.common.SubscriptionSummary
import az21.subscribe.ui.common.formatMoney
import az21.subscribe.ui.navigation.ArchiveRoute
import az21.subscribe.ui.navigation.SubScribeBottomBar
import az21.subscribe.ui.theme.SubScribeTheme
import java.util.UUID

@Composable
fun ArchiveScreen(
  onOpenSubscription: (String) -> Unit,
  onNavigateTopLevel: (NavKey) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: ArchiveViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  ArchiveContent(
    uiState = uiState,
    onOpenSubscription = onOpenSubscription,
    onArchive = viewModel::archive,
    onRequestDelete = viewModel::requestDelete,
    onCancelDelete = viewModel::cancelDelete,
    onConfirmDelete = viewModel::confirmDelete,
    onNavigateTopLevel = onNavigateTopLevel,
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveContent(
  uiState: ArchiveUiState,
  onOpenSubscription: (String) -> Unit,
  onArchive: (UUID) -> Unit,
  onRequestDelete: (SubscriptionSummary) -> Unit,
  onCancelDelete: () -> Unit,
  onConfirmDelete: () -> Unit,
  onNavigateTopLevel: (NavKey) -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = { TopAppBar(title = { Text(stringResource(R.string.archive_title)) }) },
    bottomBar = { SubScribeBottomBar(currentRoute = ArchiveRoute, onSelect = onNavigateTopLevel) },
  ) { innerPadding ->
    when {
      uiState.isLoading -> {
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }
      }

      uiState.items.isEmpty() -> {
        Box(
          modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(text = stringResource(R.string.archive_empty), style = MaterialTheme.typography.bodyLarge)
        }
      }

      else -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize().padding(innerPadding),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(items = uiState.items, key = { item -> item.subscription.id }) { item ->
            ArchiveRow(
              item = item,
              currency = uiState.currency,
              onOpen = { onOpenSubscription(item.subscription.id.toString()) },
              onArchive = { onArchive(item.subscription.id) },
              onDelete = { onRequestDelete(item) },
            )
          }
        }
      }
    }
  }

  uiState.pendingDelete?.let { item ->
    DeleteSubscriptionDialog(
      subscriptionName = item.subscription.name,
      onConfirm = onConfirmDelete,
      onDismiss = onCancelDelete,
    )
  }
}

@Composable
private fun ArchiveRow(
  item: SubscriptionSummary,
  currency: Currency,
  onOpen: () -> Unit,
  onArchive: () -> Unit,
  onDelete: () -> Unit,
) {
  val locale = Locale.current.platformLocale
  Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        SubscriptionIcon(
          iconId = item.subscription.iconId,
          name = item.subscription.name,
          modifier = Modifier.size(40.dp),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(text = item.subscription.name, style = MaterialTheme.typography.titleMedium)
          Text(
            text =
              stringResource(
                if (item.subscription.status == SubscriptionStatus.CANCELLED) {
                  R.string.archive_cancelled
                } else {
                  R.string.archive_archived
                },
              ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Text(
          text = item.price?.let { price -> formatMoney(price, currency, locale) }.orEmpty(),
          style = MaterialTheme.typography.titleSmall,
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (item.subscription.status == SubscriptionStatus.CANCELLED) {
          OutlinedButton(onClick = onArchive) { Text(stringResource(R.string.archive_archive_action)) }
        } else {
          TextButton(onClick = onDelete) {
            Text(stringResource(R.string.archive_delete_action), color = MaterialTheme.colorScheme.error)
          }
        }
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun ArchiveContentPreview() {
  SubScribeTheme {
    ArchiveContent(
      uiState = ArchiveUiState(isLoading = false),
      onOpenSubscription = {},
      onArchive = {},
      onRequestDelete = {},
      onCancelDelete = {},
      onConfirmDelete = {},
      onNavigateTopLevel = {},
    )
  }
}
