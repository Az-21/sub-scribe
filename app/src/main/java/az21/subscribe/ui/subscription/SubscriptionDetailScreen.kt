package az21.subscribe.ui.subscription

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az21.subscribe.R
import az21.subscribe.domain.model.BillingCycle
import az21.subscribe.domain.model.Currency
import az21.subscribe.domain.model.SubscriptionStatus
import az21.subscribe.ui.common.DeleteSubscriptionDialog
import az21.subscribe.ui.common.PrimaryAppBarAction
import az21.subscribe.ui.common.SubScribeTopAppBar
import az21.subscribe.ui.common.SubscriptionIcon
import az21.subscribe.ui.common.formatMoney
import az21.subscribe.ui.theme.AppTheme
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun SubscriptionDetailScreen(
  subscriptionId: String,
  onBack: () -> Unit,
  onEdit: (String) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: SubscriptionDetailViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  LaunchedEffect(subscriptionId) { viewModel.initialize(subscriptionId) }

  SubscriptionDetailContent(
    uiState = uiState,
    onBack = onBack,
    onEdit = { onEdit(subscriptionId) },
    onAddPrice = viewModel::addPrice,
    onCancel = viewModel::cancel,
    onArchive = viewModel::archive,
    onDelete = {
      viewModel.delete()
      onBack()
    },
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SubscriptionDetailContent(
  uiState: SubscriptionDetailUiState,
  onBack: () -> Unit,
  onEdit: () -> Unit,
  onAddPrice: (BigDecimal, LocalDate) -> Unit,
  onCancel: (LocalDate?) -> Unit,
  onArchive: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showPriceDialog by remember { mutableStateOf(false) }
  var showDeleteDialog by remember { mutableStateOf(false) }

  DetailScaffold(
    title = uiState.subscription?.name.orEmpty(),
    onBack = onBack,
    onEdit = onEdit,
    modifier = modifier,
  ) { innerPadding ->
    val subscription = uiState.subscription
    if (uiState.isLoading || subscription == null) {
      Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
        LoadingIndicator()
      }
      return@DetailScaffold
    }

    DetailBody(
      uiState = uiState,
      onAddPrice = { showPriceDialog = true },
      onCancel = { onCancel(null) },
      onArchive = onArchive,
      onRequestDelete = { showDeleteDialog = true },
      modifier = Modifier.padding(innerPadding),
    )
  }

  if (showPriceDialog) {
    PriceChangeDialog(
      onConfirm = { price, date ->
        onAddPrice(price, date)
        showPriceDialog = false
      },
      onDismiss = { showPriceDialog = false },
    )
  }

  if (showDeleteDialog && uiState.subscription != null) {
    DeleteSubscriptionDialog(
      subscriptionName = uiState.subscription.name,
      onConfirm = {
        showDeleteDialog = false
        onDelete()
      },
      onDismiss = { showDeleteDialog = false },
    )
  }
}

@Composable
private fun DetailBody(
  uiState: SubscriptionDetailUiState,
  onAddPrice: () -> Unit,
  onCancel: () -> Unit,
  onArchive: () -> Unit,
  onRequestDelete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val subscription = uiState.subscription ?: return
  Column(
    modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    DetailHeader(uiState = uiState)
    DetailInfo(uiState = uiState)
    if (uiState.tags.isNotEmpty()) {
      TagSection(tags = uiState.tags.map { tag -> tag.name })
    }
    if (!subscription.notes.isNullOrBlank()) {
      SectionCard(title = stringResource(R.string.detail_notes)) {
        Text(text = subscription.notes, style = MaterialTheme.typography.bodyMedium)
      }
    }
    PriceHistorySection(uiState = uiState, onAddPrice = onAddPrice)
    LifecycleActions(
      status = subscription.status,
      onCancel = onCancel,
      onArchive = onArchive,
      onDelete = onRequestDelete,
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailScaffold(
  title: String,
  onBack: () -> Unit,
  onEdit: () -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable (PaddingValues) -> Unit,
) {
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  Scaffold(
    modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      SubScribeTopAppBar(
        title = title,
        onNavigateUp = onBack,
        scrollBehavior = scrollBehavior,
        primaryAction =
          PrimaryAppBarAction(
            label = stringResource(R.string.action_edit),
            icon = Icons.Default.Edit,
            onClick = onEdit,
          ),
      )
    },
    content = content,
  )
}

@Composable
private fun DetailHeader(uiState: SubscriptionDetailUiState) {
  val subscription = uiState.subscription ?: return
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    SubscriptionIcon(iconId = subscription.iconId, name = subscription.name, modifier = Modifier.size(56.dp))
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(text = subscription.name, style = MaterialTheme.typography.headlineSmall)
      Text(
        text = stringResource(statusLabel(subscription.status)),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
      )
    }
  }
}

@Composable
private fun DetailInfo(uiState: SubscriptionDetailUiState) {
  val subscription = uiState.subscription ?: return
  val locale = Locale.current.platformLocale
  val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
  SectionCard(title = null) {
    InfoRow(
      label = stringResource(R.string.detail_current_price),
      value =
        uiState.currentPrice?.let { formatMoney(it, uiState.currency, locale) }
          ?: stringResource(R.string.detail_none),
    )
    InfoRow(
      label = stringResource(R.string.detail_next_billing),
      value = uiState.nextBillingDate?.format(dateFormatter) ?: stringResource(R.string.detail_none),
    )
    InfoRow(
      label = stringResource(R.string.detail_start_date),
      value = subscription.startDate.format(dateFormatter),
    )
    InfoRow(
      label = stringResource(R.string.detail_billing_cycle),
      value =
        stringResource(
          if (subscription.billingCycle == BillingCycle.MONTHLY) {
            R.string.form_cycle_monthly
          } else {
            R.string.form_cycle_annual
          },
        ),
    )
    subscription.freeTrialMonths?.takeIf { it > 0 }?.let { months ->
      InfoRow(
        label = stringResource(R.string.detail_free_trial),
        value = pluralStringResource(R.plurals.detail_free_trial_months, months, months),
      )
    }
    uiState.trialEndDate?.let { date ->
      InfoRow(label = stringResource(R.string.detail_trial_end), value = date.format(dateFormatter))
    }
    subscription.reminderDaysBefore?.let { days ->
      InfoRow(
        label = stringResource(R.string.detail_reminder),
        value = pluralStringResource(R.plurals.detail_reminder_days, days, days),
      )
    }
    InfoRow(
      label = stringResource(R.string.detail_payment_method),
      value = uiState.paymentMethodLabel ?: stringResource(R.string.detail_none),
    )
    subscription.endDate?.let { date ->
      InfoRow(
        label = stringResource(R.string.detail_status),
        value = stringResource(R.string.detail_end_date, date.format(dateFormatter)),
      )
    }
  }
}

@Composable
private fun InfoRow(
  label: String,
  value: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(text = label, style = MaterialTheme.typography.bodyMedium)
    Text(text = value, style = MaterialTheme.typography.bodyMedium)
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagSection(tags: List<String>) {
  SectionCard(title = stringResource(R.string.detail_tags)) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      tags.forEach { tag ->
        Text(
          text = tag,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}

@Composable
private fun PriceHistorySection(
  uiState: SubscriptionDetailUiState,
  onAddPrice: () -> Unit,
) {
  val locale = Locale.current.platformLocale
  val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
  SectionCard(title = stringResource(R.string.detail_price_history)) {
    if (uiState.timeline.isEmpty()) {
      Text(text = stringResource(R.string.detail_price_no_history), style = MaterialTheme.typography.bodyMedium)
    } else {
      uiState.timeline.forEachIndexed { index, item ->
        if (index > 0) HorizontalDivider()
        Row(
          modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(text = item.entry.effectiveFromDate.format(dateFormatter), style = MaterialTheme.typography.bodyMedium)
          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = formatMoney(item.entry.price, uiState.currency, locale),
              style = MaterialTheme.typography.titleSmall,
            )
            item.delta?.takeIf { it.signum() != 0 }?.let { delta ->
              Text(
                text = formatDelta(delta, uiState.currency, locale),
                style = MaterialTheme.typography.labelSmall,
                color =
                  if (delta.signum() > 0) {
                    MaterialTheme.colorScheme.error
                  } else {
                    MaterialTheme.colorScheme.primary
                  },
              )
            }
          }
        }
      }
    }
    OutlinedButton(onClick = onAddPrice, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
      Icon(
        imageVector = Icons.Default.Add,
        contentDescription = null,
        modifier = Modifier.size(ButtonDefaults.IconSize),
      )
      Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
      Text(stringResource(R.string.detail_add_price_change))
    }
  }
}

@Composable
private fun LifecycleActions(
  status: SubscriptionStatus,
  onCancel: () -> Unit,
  onArchive: () -> Unit,
  onDelete: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    when (status) {
      SubscriptionStatus.ACTIVE -> {
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize),
          )
          Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
          Text(stringResource(R.string.detail_cancel))
        }
      }

      SubscriptionStatus.CANCELLED -> {
        Button(onClick = onArchive, modifier = Modifier.fillMaxWidth()) {
          Icon(
            imageVector = Icons.Default.Archive,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize),
          )
          Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
          Text(stringResource(R.string.detail_archive))
        }
      }

      SubscriptionStatus.ARCHIVED -> {
        Button(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
          Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize),
          )
          Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
          Text(stringResource(R.string.detail_delete))
        }
      }
    }
  }
}

@Composable
private fun SectionCard(
  title: String?,
  content: @Composable () -> Unit,
) {
  Card(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      if (title != null) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
      }
      content()
    }
  }
}

private fun statusLabel(status: SubscriptionStatus): Int =
  when (status) {
    SubscriptionStatus.ACTIVE -> R.string.home_status_active
    SubscriptionStatus.CANCELLED -> R.string.home_status_cancelled
    SubscriptionStatus.ARCHIVED -> R.string.home_status_archived
  }

private fun formatDelta(
  delta: BigDecimal,
  currency: Currency,
  locale: java.util.Locale,
): String {
  val sign = if (delta.signum() > 0) "+" else "-"
  return sign + formatMoney(delta.abs(), currency, locale)
}

@Preview(showBackground = true)
@Composable
private fun SubscriptionDetailPreview() {
  AppTheme {
    SubscriptionDetailContent(
      uiState = SubscriptionDetailUiState(isLoading = false),
      onBack = {},
      onEdit = {},
      onAddPrice = { _, _ -> },
      onCancel = {},
      onArchive = {},
      onDelete = {},
    )
  }
}
