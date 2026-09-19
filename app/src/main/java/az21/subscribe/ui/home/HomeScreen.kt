package az21.subscribe.ui.home

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import az21.subscribe.ui.common.SubscriptionIcon
import az21.subscribe.ui.common.SubscriptionSummary
import az21.subscribe.ui.common.formatMoney
import az21.subscribe.ui.navigation.HomeRoute
import az21.subscribe.ui.navigation.SubScribeBottomBar
import az21.subscribe.ui.theme.AppTheme
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.UUID

@Composable
fun HomeScreen(
  onAddSubscription: () -> Unit,
  onOpenSubscription: (String) -> Unit,
  onOpenArchive: () -> Unit,
  onNavigateTopLevel: (NavKey) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: HomeViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  HomeContent(
    uiState = uiState,
    onQueryChange = viewModel::onQueryChange,
    onStatusChange = viewModel::onStatusChange,
    onTagChange = viewModel::onTagChange,
    onPaymentMethodChange = viewModel::onPaymentMethodChange,
    onSortChange = viewModel::onSortChange,
    onAddSubscription = onAddSubscription,
    onOpenSubscription = onOpenSubscription,
    onOpenArchive = onOpenArchive,
    onNavigateTopLevel = onNavigateTopLevel,
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
  uiState: HomeUiState,
  onQueryChange: (String) -> Unit,
  onStatusChange: (SubscriptionStatus?) -> Unit,
  onTagChange: (UUID?) -> Unit,
  onPaymentMethodChange: (UUID?) -> Unit,
  onSortChange: (SubscriptionSort) -> Unit,
  onAddSubscription: () -> Unit,
  onOpenSubscription: (String) -> Unit,
  onOpenArchive: () -> Unit,
  onNavigateTopLevel: (NavKey) -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.home_title)) },
        actions = { HomeTopBarActions(onSortChange = onSortChange, onOpenArchive = onOpenArchive) },
      )
    },
    bottomBar = { SubScribeBottomBar(currentRoute = HomeRoute, onSelect = onNavigateTopLevel) },
    floatingActionButton = {
      FloatingActionButton(onClick = onAddSubscription) {
        Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.home_add))
      }
    },
  ) { innerPadding ->
    HomeBody(
      uiState = uiState,
      onQueryChange = onQueryChange,
      onStatusChange = onStatusChange,
      onTagChange = onTagChange,
      onPaymentMethodChange = onPaymentMethodChange,
      onOpenSubscription = onOpenSubscription,
      modifier = Modifier.fillMaxSize().padding(innerPadding),
    )
  }
}

@Composable
private fun HomeTopBarActions(
  onSortChange: (SubscriptionSort) -> Unit,
  onOpenArchive: () -> Unit,
) {
  var sortMenuExpanded by remember { mutableStateOf(false) }
  Box {
    TextButton(onClick = { sortMenuExpanded = true }) { Text(stringResource(R.string.home_sort)) }
    DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
      SortOption.entries.forEach { option ->
        DropdownMenuItem(
          text = { Text(stringResource(option.labelRes)) },
          onClick = {
            onSortChange(option.sort)
            sortMenuExpanded = false
          },
        )
      }
    }
  }
  TextButton(onClick = onOpenArchive) { Text(stringResource(R.string.home_open_archive)) }
}

@Composable
private fun HomeBody(
  uiState: HomeUiState,
  onQueryChange: (String) -> Unit,
  onStatusChange: (SubscriptionStatus?) -> Unit,
  onTagChange: (UUID?) -> Unit,
  onPaymentMethodChange: (UUID?) -> Unit,
  onOpenSubscription: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier) {
    HomeSearchBar(query = uiState.query, onQueryChange = onQueryChange)
    HomeFilterControls(
      uiState = uiState,
      onStatusChange = onStatusChange,
      onTagChange = onTagChange,
      onPaymentMethodChange = onPaymentMethodChange,
    )
    HomeList(uiState = uiState, onOpenSubscription = onOpenSubscription)
  }
}

@Composable
private fun HomeSearchBar(
  query: String,
  onQueryChange: (String) -> Unit,
) {
  OutlinedTextField(
    value = query,
    onValueChange = onQueryChange,
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    placeholder = { Text(stringResource(R.string.home_search_hint)) },
    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
    trailingIcon = {
      if (query.isNotEmpty()) {
        IconButton(onClick = { onQueryChange("") }) {
          Icon(imageVector = Icons.Default.Clear, contentDescription = null)
        }
      }
    },
    singleLine = true,
  )
}

@Composable
private fun HomeFilterControls(
  uiState: HomeUiState,
  onStatusChange: (SubscriptionStatus?) -> Unit,
  onTagChange: (UUID?) -> Unit,
  onPaymentMethodChange: (UUID?) -> Unit,
) {
  FilterRow(label = stringResource(R.string.home_filter_status)) {
    FilterChip(
      selected = uiState.status == null,
      onClick = { onStatusChange(null) },
      label = { Text(stringResource(R.string.home_filter_all)) },
    )
    StatusFilter.entries.forEach { entry ->
      FilterChip(
        selected = uiState.status == entry.status,
        onClick = { onStatusChange(entry.status) },
        label = { Text(stringResource(entry.labelRes)) },
      )
    }
  }

  if (uiState.allTags.isNotEmpty()) {
    FilterRow(label = stringResource(R.string.home_filter_tag)) {
      FilterChip(
        selected = uiState.tagId == null,
        onClick = { onTagChange(null) },
        label = { Text(stringResource(R.string.home_filter_all)) },
      )
      uiState.allTags.forEach { tag ->
        FilterChip(
          selected = uiState.tagId == tag.id,
          onClick = { onTagChange(tag.id) },
          label = { Text(tag.name) },
        )
      }
    }
  }

  if (uiState.paymentMethods.isNotEmpty()) {
    FilterRow(label = stringResource(R.string.home_filter_payment)) {
      FilterChip(
        selected = uiState.paymentMethodId == null,
        onClick = { onPaymentMethodChange(null) },
        label = { Text(stringResource(R.string.home_filter_all)) },
      )
      uiState.paymentMethods.forEach { method ->
        FilterChip(
          selected = uiState.paymentMethodId == method.id,
          onClick = { onPaymentMethodChange(method.id) },
          label = { Text(method.label) },
        )
      }
    }
  }
}

@Composable
private fun HomeList(
  uiState: HomeUiState,
  onOpenSubscription: (String) -> Unit,
) {
  when {
    uiState.isLoading -> {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    }

    uiState.items.isEmpty() -> {
      Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
          text = stringResource(if (uiState.hasSubscriptions) R.string.home_no_results else R.string.home_empty),
          style = MaterialTheme.typography.bodyLarge,
        )
      }
    }

    else -> {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(items = uiState.items, key = { item -> item.subscription.id }) { item ->
          SubscriptionRow(
            item = item,
            currency = uiState.currency,
            onClick = { onOpenSubscription(item.subscription.id.toString()) },
          )
        }
      }
    }
  }
}

@Composable
private fun FilterRow(
  label: String,
  content: @Composable () -> Unit,
) {
  Column {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      modifier = Modifier.padding(start = 16.dp, top = 4.dp),
    )
    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      content()
    }
  }
}

@Composable
private fun SubscriptionRow(
  item: SubscriptionSummary,
  currency: Currency,
  onClick: () -> Unit,
) {
  val locale = Locale.current.platformLocale
  Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
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
        if (item.tags.isNotEmpty()) {
          Text(
            text = item.tags.joinToString(" · ") { tag -> tag.name },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }
      Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text =
            item.price?.let { price -> formatMoney(price, currency, locale) }
              ?: stringResource(R.string.detail_none),
          style = MaterialTheme.typography.titleSmall,
        )
        Text(
          text = item.nextBillingDate?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) ?: "",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

private enum class SortOption(
  val sort: SubscriptionSort,
  val labelRes: Int,
) {
  NAME(SubscriptionSort.NAME, R.string.home_sort_name),
  PRICE(SubscriptionSort.PRICE, R.string.home_sort_price),
  NEXT_BILLING(SubscriptionSort.NEXT_BILLING, R.string.home_sort_next_billing),
}

private enum class StatusFilter(
  val status: SubscriptionStatus,
  val labelRes: Int,
) {
  ACTIVE(SubscriptionStatus.ACTIVE, R.string.home_status_active),
  CANCELLED(SubscriptionStatus.CANCELLED, R.string.home_status_cancelled),
  ARCHIVED(SubscriptionStatus.ARCHIVED, R.string.home_status_archived),
}

@Preview(showBackground = true)
@Composable
private fun HomeContentPreview() {
  AppTheme {
    HomeContent(
      uiState = HomeUiState(isLoading = false, items = emptyList(), hasSubscriptions = false),
      onQueryChange = {},
      onStatusChange = {},
      onTagChange = {},
      onPaymentMethodChange = {},
      onSortChange = {},
      onAddSubscription = {},
      onOpenSubscription = {},
      onOpenArchive = {},
      onNavigateTopLevel = {},
    )
  }
}
