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
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import az21.subscribe.ui.common.IconActionButton
import az21.subscribe.ui.common.SubScribeTopAppBar
import az21.subscribe.ui.common.SubscriptionIcon
import az21.subscribe.ui.common.SubscriptionSummary
import az21.subscribe.ui.common.formatMoney
import az21.subscribe.ui.navigation.SubScribeFloatingToolbar
import az21.subscribe.ui.navigation.TopLevelDestination
import az21.subscribe.ui.theme.AppTheme
import kotlinx.coroutines.launch
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
  val searchBarState = rememberSearchBarState()
  val textFieldState = rememberTextFieldState(initialText = uiState.query)
  val searchBarScope = rememberCoroutineScope()
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  LaunchedEffect(textFieldState) {
    snapshotFlow { textFieldState.text.toString() }.collect { value -> onQueryChange(value) }
  }
  Scaffold(
    modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      SubScribeTopAppBar(
        title = stringResource(R.string.home_title),
        scrollBehavior = scrollBehavior,
        actions = {
          HomeTopBarActions(
            currentSort = uiState.sort,
            onSortChange = onSortChange,
            onOpenArchive = onOpenArchive,
            onSearch = { searchBarScope.launch { searchBarState.animateToExpanded() } },
          )
        },
      )
    },
    floatingActionButtonPosition = FabPosition.Center,
    floatingActionButton = {
      SubScribeFloatingToolbar(
        selectedDestination = TopLevelDestination.HOME,
        onNavigateTopLevel = onNavigateTopLevel,
        onCreateSubscription = onAddSubscription,
      )
    },
  ) { innerPadding ->
    HomeBody(
      uiState = uiState,
      onStatusChange = onStatusChange,
      onTagChange = onTagChange,
      onPaymentMethodChange = onPaymentMethodChange,
      onOpenSubscription = onOpenSubscription,
      modifier = Modifier.fillMaxSize().padding(innerPadding),
    )
  }
  HomeSearchBar(
    searchBarState = searchBarState,
    textFieldState = textFieldState,
    items = uiState.items,
    onOpenSubscription = onOpenSubscription,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeSearchBar(
  searchBarState: SearchBarState,
  textFieldState: TextFieldState,
  items: List<SubscriptionSummary>,
  onOpenSubscription: (String) -> Unit,
) {
  val scope = rememberCoroutineScope()
  val inputField =
    @Composable {
      SearchBarDefaults.InputField(
        textFieldState = textFieldState,
        searchBarState = searchBarState,
        onSearch = { scope.launch { searchBarState.animateToCollapsed() } },
        placeholder = { Text(stringResource(R.string.home_search_hint)) },
        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
          if (textFieldState.text.isNotEmpty()) {
            IconButton(onClick = { textFieldState.clearText() }) {
              Icon(imageVector = Icons.Default.Close, contentDescription = null)
            }
          }
        },
      )
    }
  ExpandedFullScreenSearchBar(state = searchBarState, inputField = inputField) {
    items.take(MAX_SEARCH_SUGGESTIONS).forEach { item ->
      ListItem(
        onClick = {
          textFieldState.setTextAndPlaceCursorAtEnd(item.subscription.name)
          scope.launch { searchBarState.animateToCollapsed() }
          onOpenSubscription(item.subscription.id.toString())
        },
        leadingContent = {
          SubscriptionIcon(
            iconId = item.subscription.iconId,
            name = item.subscription.name,
            modifier = Modifier.size(32.dp),
          )
        },
        content = { Text(item.subscription.name) },
      )
    }
  }
}

@Composable
private fun HomeTopBarActions(
  currentSort: SubscriptionSort,
  onSortChange: (SubscriptionSort) -> Unit,
  onOpenArchive: () -> Unit,
  onSearch: () -> Unit,
) {
  IconActionButton(
    onClick = onSearch,
    icon = Icons.Default.Search,
    contentDescription = stringResource(R.string.home_search),
  )
  var sortMenuExpanded by remember { mutableStateOf(false) }
  Box {
    IconActionButton(
      onClick = { sortMenuExpanded = true },
      icon = Icons.AutoMirrored.Filled.Sort,
      contentDescription = stringResource(R.string.home_sort),
    )
    DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
      SortOption.entries.forEachIndexed { index, option ->
        SelectableDropdownMenuItem(
          selected = option.sort == currentSort,
          text = { Text(stringResource(option.labelRes)) },
          onClick = {
            onSortChange(option.sort)
            sortMenuExpanded = false
          },
          shapes = MenuDefaults.itemShape(index, SortOption.entries.size),
        )
      }
    }
  }
  IconActionButton(
    onClick = onOpenArchive,
    icon = Icons.Default.Archive,
    contentDescription = stringResource(R.string.home_open_archive),
  )
}

@Composable
private fun HomeBody(
  uiState: HomeUiState,
  onStatusChange: (SubscriptionStatus?) -> Unit,
  onTagChange: (UUID?) -> Unit,
  onPaymentMethodChange: (UUID?) -> Unit,
  onOpenSubscription: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier) {
    HomeFilterControls(
      uiState = uiState,
      onStatusChange = onStatusChange,
      onTagChange = onTagChange,
      onPaymentMethodChange = onPaymentMethodChange,
    )
    HomeList(uiState = uiState, onOpenSubscription = onOpenSubscription)
  }
}

private const val MAX_SEARCH_SUGGESTIONS = 5

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomeFilterControls(
  uiState: HomeUiState,
  onStatusChange: (SubscriptionStatus?) -> Unit,
  onTagChange: (UUID?) -> Unit,
  onPaymentMethodChange: (UUID?) -> Unit,
) {
  val allLabel = stringResource(R.string.home_filter_all)
  val statusLabels = StatusFilter.entries.associateWith { entry -> stringResource(entry.labelRes) }
  FilterButtonGroup(label = stringResource(R.string.home_filter_status)) {
    toggleableItem(
      checked = uiState.status == null,
      label = allLabel,
      onCheckedChange = { onStatusChange(null) },
      weight = 1f,
    )
    StatusFilter.entries.forEach { entry ->
      toggleableItem(
        checked = uiState.status == entry.status,
        label = statusLabels.getValue(entry),
        onCheckedChange = { onStatusChange(entry.status) },
        weight = 1f,
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomeList(
  uiState: HomeUiState,
  onOpenSubscription: (String) -> Unit,
) {
  when {
    uiState.isLoading -> {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FilterButtonGroup(
  label: String,
  content: ButtonGroupScope.() -> Unit,
) {
  Column {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      modifier = Modifier.padding(start = 16.dp, top = 4.dp),
    )
    ButtonGroup(
      overflowIndicator = {},
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
      content = content,
    )
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
  ListItem(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth(),
    leadingContent = {
      SubscriptionIcon(
        iconId = item.subscription.iconId,
        name = item.subscription.name,
        modifier = Modifier.size(40.dp),
      )
    },
    content = { Text(text = item.subscription.name, style = MaterialTheme.typography.titleMedium) },
    supportingContent =
      if (item.tags.isEmpty()) {
        null
      } else {
        {
          Text(
            text = item.tags.joinToString(" · ") { tag -> tag.name },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      },
    trailingContent = {
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
    },
  )
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
